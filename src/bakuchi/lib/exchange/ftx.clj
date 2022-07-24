(ns bakuchi.lib.exchange.ftx
  (:require
   [bakuchi.lib.exchange.client :as client]
   [bakuchi.lib.exchange.interface :as if]
   [bakuchi.lib.exchange.lib :as lib]
   [bakuchi.lib.tool :as tool]))

(def creds-file "creds.edn")
(def creds
  (-> creds-file
      tool/load-edn
      :ftx))

(def base-url "https://ftx.com/api")

(defn get-public [path]
  (let [url (client/->url base-url path)]
    (client/get-public url)))

(defrecord FTX
  [api-key api-secret]

  if/Public
  (fetch-ticker [_]
    (get-public "/markets/BTC_JPY"))

  if/Library
  (get-best-tick [this]
    (let [tick (:result (.fetch-ticker this))
          ask  (-> tick :ask)
          bid  (-> tick :bid)]
      (lib/->best-tick ask bid))))

(def ftx (map->FTX creds))

(comment
  (if/fetch-ticker ftx)
  (if/get-best-tick ftx)
  )
