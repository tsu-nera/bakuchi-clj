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
  {"FTX-KEY"  key
   "FTX-TS"   timestamp
   "FTX-SIGN" sign})

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

(defrecord FTX
  [api-key api-secret symbol]

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
  (fetch-order [this id]
    (let [path (str "/api/orders/" id)]
      (get-private path)))
  ;; https://docs.ftx.com/#place-order
  (create-order [this params]
    (let [path "/api/orders"]
      (post-private path params)))
  ;; https://docs.ftx.com/#cancel-order
  (cancel-order [this id]
    (let [path (str "/api/orders/" id)]
      (delete-private path)))

  if/Library
  (get-best-tick [this]
    (let [tick (.fetch-ticker this)
          ask  (-> tick :ask)
          bid  (-> tick :bid)]
      (lib/->best-tick ask bid))))

(def ftx (map->FTX (merge creds {:symbol "BTC/JPY"})))

(comment
  (if/fetch-ticker ftx)
  (if/fetch-orderbook ftx)

  (if/fetch-balance ftx)
  (if/fetch-open-orders ftx)
  (if/fetch-closed-orders ftx)
  (if/fetch-order ftx "166136326482")

  (def resp (if/create-order ftx {"market" "BTC/JPY"
                                  "side"   "sell"
                                  "type"   "limit"
                                  "size"   0.0001
                                  "price"  2952861.11}))
  (def order-id (:id resp))
  (def resp (if/cancel-order ftx order-id))

  )

(comment
  (if/get-best-tick ftx)
  )
