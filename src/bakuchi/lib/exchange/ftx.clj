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

(defn get-public [path & {:as payload}]
  (let [url (client/->url base-url path)]
    (client/get url {:payload payload})))

(defn get-private [path & {:as payload}]
  (let [url       (client/->url base-url path)
        timestamp (->timestamp)
        text      (client/->get-signature-text timestamp path payload)
        sign      (tool/->sign (:api-secret creds) text)
        headers   {"FTX-KEY"  (:api-key creds)
                   "FTX-TS"   timestamp
                   "FTX-SIGN" sign}]
    (client/get url {:payload payload :headers headers})))

(defrecord FTX
  [api-key api-secret symbol]

  if/Public
  (fetch-ticker [this]
    (let [market-name (:symbol this)
          path        (str "/api/markets" "/" market-name)]
      (get-public path)))
  (fetch-orderbook [this]
    (let [market-name (:symbol this)
          path        (str "/api/markets" "/" market-name "/orderbook")]
      (get-public path)))

  if/Private
  (fetch-balance [this]
    (let [path "/api/wallet/balances"]
      (get-private path)))

  if/Library
  (get-best-tick [this]
    (let [tick (:result (.fetch-ticker this))
          ask  (-> tick :ask)
          bid  (-> tick :bid)]
      (lib/->best-tick ask bid))))

(def ftx (map->FTX (merge creds {:symbol "BTC_JPY"})))

(comment
  (if/fetch-ticker ftx)
  (if/fetch-orderbook ftx)

  (if/fetch-balance ftx)
  )

(comment
  (if/get-best-tick ftx)
  )
