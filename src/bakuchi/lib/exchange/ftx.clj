(ns bakuchi.lib.exchange.ftx
  (:require
   [bakuchi.lib.exchange.client :as client]
   [bakuchi.lib.exchange.interface :as if]
   [bakuchi.lib.exchange.lib :as lib]
   [bakuchi.lib.time :refer [->timestamp]]
   [bakuchi.lib.tool :as tool]))

(def creds-file "creds.edn")
(def creds
  (-> creds-file
      tool/load-edn
      :ftx))

(def base-url "https://ftx.com")

(defn- ->result [resp]
  (when (:success resp)
    (:result resp)))

(defn- get-public [path & {:as payload}]
  (let [url (client/->url base-url path)]
    (when-let [resp (client/get url {:payload payload})]
      (-> resp ->result))))

(defn- ->private-headers [key timestamp sign]
  {"FTX-KEY"        key
   "FTX-TS"         timestamp
   "FTX-SIGN"       sign
   "FTX-SUBACCOUNT" (:subaccount creds)})

(defn- get-private [path & {:as payload}]
  (let [url       (client/->url base-url path)
        timestamp (->timestamp)
        text      (client/->get-signature-text timestamp path payload)
        sign      (tool/->sign (:api-secret creds) text)
        headers   (->private-headers (:api-key creds) timestamp sign)]
    (when-let [resp (client/get url {:payload payload :headers headers})]
      (-> resp ->result))))

(defn- post-private [path & {:as params}]
  (let [url       (client/->url base-url path)
        timestamp (->timestamp)
        text      (client/->post-signature-text timestamp path params)
        sign      (tool/->sign (:api-secret creds) text)
        headers   (->private-headers (:api-key creds) timestamp sign)]
    (when-let [resp (client/post url {:params params :headers headers})]
      (-> resp ->result))))

(defn- delete-private [path]
  (let [url       (client/->url base-url path)
        timestamp (->timestamp)
        text      (client/->delete-signature-text timestamp path)
        sign      (tool/->sign (:api-secret creds) text)
        headers   (->private-headers (:api-key creds) timestamp sign)]
    (when-let [resp (client/delete url {:headers headers})]
      (-> resp ->result))))

(defn calc-effective [orders eff-amount [my-price my-size]]
  (let [[best-price best-size] (first orders)]
    (if (> best-size eff-amount)
      best-price
      (letfn [(effective? [[eff-price amount] [price size]]
                (let [remaining (if (= price my-price)
                                  (- size my-size)
                                  size)
                      next-eff  (if (zero? remaining)
                                  eff-price
                                  price)
                      sum       (+ amount remaining)]
                  (if (> sum eff-amount)
                    (reduced next-eff) [next-eff sum])))]
        (reduce effective? [best-price best-size] (rest orders))))))

(defrecord FTX
  [api-key api-secret symbol subaccount]

  if/Public
  (fetch-ticker [this]
    (let [market (:symbol this)
          path   (str "/api/markets" "/" market)]
      (get-public path)))
  (fetch-orderbook [this]
    (let [market (:symbol this)
          path   (str "/api/markets" "/" market "/orderbook")]
      (get-public path)))

  if/Private
  ;; https://docs.ftx.com/#get-balances
  (fetch-balance [_]
    (let [path "/api/wallet/balances"]
      (get-private path)))
  ;; https://docs.ftx.com/#get-open-orders
  (fetch-open-orders [this]
    (let [market (:symbol this)
          path   (str "/api/orders?market=" market)]
      (get-private path)))
  ;; https://docs.ftx.com/#get-order-history
  (fetch-closed-orders [this]
    (let [market (:symbol this)
          path   (str "/api/orders/history?market=" market)]
      (get-private path)))
  ;; https://docs.ftx.com/#get-order-status
  (fetch-order [_ id]
    (let [path (str "/api/orders/" id)]
      (get-private path)))
  ;; https://docs.ftx.com/#place-order
  (create-order [_ params]
    (let [path "/api/orders"]
      (post-private path params)))
  ;; https://docs.ftx.com/#cancel-order
  (cancel-order [_ id]
    (let [path (str "/api/orders/" id)]
      (delete-private path)))
  ;; https://docs.ftx.com/#cancel-all-orders
  (cancel-all-orders [_]
    (let [path "/api/orders"]
      (delete-private path)))

  if/Library
  (get-best-tick [this]
    (let [{:keys [bid ask]} (.fetch-ticker this)]
      (lib/->tick ask bid)))
  (get-eff-tick [this params]
    (let [{:keys [eff-amount ask-rate ask-size bid-rate bid-size]
           :or   {eff-amount 0.01
                  ask-size   0 ask-rate 0
                  bid-size   0 bid-rate 0}} params
          orderbook                         (if/fetch-orderbook this)
          bids                              (:bids orderbook)
          asks                              (:asks orderbook)
          bid
          (calc-effective bids eff-amount [bid-rate bid-size])
          ask
          (calc-effective asks eff-amount [ask-rate ask-size])]
      (lib/->tick ask bid)))
  (create-limit-order [this side rate size]
    (let [market (:symbol this)]
      (if/create-order this {"market" market
                             "side"   side
                             "type"   "limit"
                             "size"   size
                             "price"  rate})))
  (create-market-order [this side size]
    (let [market (:symbol this)]
      (if/create-order this {"market" market
                             "side"   side
                             "type"   "market"
                             "size"   size
                             "price"  nil})))

  ;; https://docs.ftx.com/#modify-order
  ;; "cancelしてlimit orderしている.  market orderは不可."
  ;; order-idは新しいものが使われる.
  (modify-order
    [this id rate size]
    (let [path (str "/api/orders/" id "/modify")]
      (post-private path {"size" size "price" rate}))))

(def ftx (map->FTX (merge creds {:symbol "BTC/JPY"})))

(comment
  (if/fetch-ticker ftx)
  (if/fetch-orderbook ftx)

  (if/fetch-balance ftx)
  (if/fetch-open-orders ftx)
  (if/fetch-closed-orders ftx)
  (if/fetch-order ftx "166136326482")

  (def resp (if/create-limit-order ftx "sell" 0.0001 3133000))
  (def order-id (:id resp))

  (def resp (if/modify-order ftx order-id 0.0002 3133000))

  (def resp (if/cancel-order ftx order-id))

  (if/cancel-all-orders ftx)

  )

(comment
  (if/get-best-tick ftx)

  (def effective-amount 0.01)
  (def orderbook (if/fetch-orderbook ftx))

  (def bids (:bids orderbook))
  (rest bids)
  (def asks (:asks orderbook))

  (if/get-eff-tick ftx {:eff-amount 0.01})
  )
