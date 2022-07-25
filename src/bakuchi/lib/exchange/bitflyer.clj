(ns bakuchi.lib.exchange.bitflyer
  (:require
   [bakuchi.lib.exchange.client :as client]
   [bakuchi.lib.tool :as tool]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [cheshire.core :refer [generate-string]])
  (:import
   (java.time
    Instant)))

(def product-code "BTCJPY05AUG2022")
#_(def product-code "BTCJPY30SEP2022")
#_(def product-code "FX_BTC_JPY")
(def base-params {"product_code" product-code})

(def base-url "https://api.bitflyer.com")

(def creds-file "creds.edn")
(def creds
  (-> creds-file
      tool/load-edn
      :bitflyer))

(defn ->timestamp
  []
  (.toString (.getEpochSecond (Instant/now))))

(defn ->signature-text
  [timestamp method path & {:as params}]
  (cond-> (str timestamp method path)
    params (str (generate-string params))))

(defn ->access-sign
  [timestamp method path & {:as params}]
  (let [key  (:api-secret creds)
        text (->signature-text timestamp method path params)]
    (tool/->sign key text)))

(defn ->signed-headers
  [method path & {:as params}]
  (let [key       (:api-key creds)
        timestamp (->timestamp)
        sign      (->access-sign timestamp method path params)]
    {"ACCESS-KEY"       key
     "ACCESS-TIMESTAMP" timestamp
     "ACCESS-SIGN"      sign}))

#_(defn post-private
    [path params]
    (let [headers (->signed-headers "POST" path params)
          url     (client/->url path)
          body    (generate-string params)]
      (when-let [resp (client/post url
                                   {:headers       headers
                                    :body          body
                                    :content-type  :json
                                    :as            :json
                                    :cookie-policy :standard})]
        (->> resp
             :body
             (cske/transform-keys csk/->kebab-case)))))

(defn get-public [path]
  (client/get (client/->url path) base-params))

(defn fetch-markets
  []
  (get-public "/v1/markets"))
#_(fetch-markets)

(defn fetch-order-book
  []
  (get-public "/v1/board"))
#_(fetch-order-book)

(defn fetch-tick
  []
  (get-public "/v1/ticker"))
#_(fetch-tick)

#_(defn fetch-balance
    "資産残高を取得"
    []
    (get-private "/v1/me/getbalance"))
#_(fetch-balance)

#_(defn fetch-collateral
    "証拠金の状態を取得"
    []
    (get-private "/v1/me/getcollateral"))
#_(fetch-collateral)

#_(defn create-order
    [params]
    (let [path       "/v1/me/sendchildorder"
          req-params (merge base-params params)]
      (post-private path req-params)))

#_(defn cancel-order
    [id]
    (let [path       "/v1/me/cancelchildorder"
          req-params (merge base-params {"child_order_acceptance_id" id})]
      (post-private path req-params)))

#_(defn fetch-orders
    "TODO オプションいろいろあるので用途ごとにラッパー関数を作成する"
    [& [query-params]]
    (get-private "/v1/me/getchildorders" query-params))
#_(fetch-orders)

(comment

  (def method "GET")
  (def path "/v1/me/getbalance")
  ;; (def path "/v1/me/getcollateral")
  (def headers (->signed-headers method path))
  (def url (client/->url path))

  (-> (client/get url
                  {:headers       headers
                   :as            :json
                   :content-type  :json
                   :cookie-policy :standard})
      :body)
  )
