(ns bakuchi.lib.exchange.client
  (:refer-clojure :exclude [get])
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [cheshire.core :refer [generate-string]]
   [clj-http.client :as client]))

(defn ->url [base-url path]
  (str base-url path))

(defn path->with-payload
  [path payload]
  (str path "?" (client/generate-query-string payload)))

(defn ->get-signature-text
  [timestamp path & {:as payload}]
  (let [path-with-payload (cond-> path
                            payload (path->with-payload payload))]
    (str timestamp "GET" path-with-payload)))

(defn ->post-signature-text
  [timestamp path & {:as body}]
  (cond-> (str timestamp "POST" path)
    body (str (generate-string body))))

(defn get
  [url & {:keys [payload headers]}]
  (let [options {:as            :json
                 :query-params  payload
                 :headers       headers
                 :cookie-policy :standard}]
    (when-let [resp (client/get url options)]
      (->> resp
           :body
           (cske/transform-keys csk/->kebab-case)))))
