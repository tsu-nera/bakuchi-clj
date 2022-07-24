(ns bakuchi.lib.exchange.client
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [clj-http.client :as client]))

(defn ->url [base-url path]
  (str base-url path))

(defn get-public
  [url & {:as payload}]
  (let [options {:as            :json
                 :query-params  payload
                 :cookie-policy :standard}]
    (when-let [resp (client/get url options)]
      (->> resp
           :body
           (cske/transform-keys csk/->kebab-case)))))
