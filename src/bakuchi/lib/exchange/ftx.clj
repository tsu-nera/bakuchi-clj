(ns bakuchi.lib.exchange.ftx
  (:require
   [bakuchi.lib.exchange.interface :as if]
   [bakuchi.lib.tool :as tool]
   [cheshire.core :refer [generate-string]]
   [clj-http.client :as client]))

(def creds-file "creds.edn")
(def creds
  (-> creds-file
      tool/load-edn
      :ftx))

(defrecord FTX
  [api-key api-secret]
  if/Public
  (fetch-ticker [this]
    (client/get "https://ftx.com/api/markets")))

(def ftx (map->FTX creds))

(comment
  (if/fetch-ticker ftx)
  )
