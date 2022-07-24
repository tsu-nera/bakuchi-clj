(ns bakuchi.lib.exchange.bitflyer
  (:require
   [bakuchi.lib.tool :as tool]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [cheshire.core :refer [generate-string]]
   [clj-http.client :as client])
  (:import
   (java.time
    Instant)))

(def product-code "BTCJPY05AUG2022")
#_(def product-code "BTCJPY30SEP2022")
#_(def product-code "FX_BTC_JPY")
(def base-params {"product_code" product-code})

(def base-url "https://api.bitflyer.com")

(defn ->url
  [path]
  (str base-url path))

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
    (tool/sign key text)))

(defn ->signed-headers
  [method path & {:as params}]
  (let [key       (:api-key creds)
        timestamp (->timestamp)
        sign      (->access-sign timestamp method path params)]
    {"ACCESS-KEY"       key
     "ACCESS-TIMESTAMP" timestamp
     "ACCESS-SIGN"      sign}))

(defn get-public
  [path]
  (let [url     (->url path)
        options {:as            :json
                 :query-params  base-params
                 :cookie-policy :standard}]
    (when-let [resp (client/get url options)]
      (->> resp
           :body
           (cske/transform-keys csk/->kebab-case)))))

(defn path->with-query-string
  [path & {:as params}]
  (cond-> path
    params (str "?" (client/generate-query-string params))))

(defn get-private
  [pathname & {:as params}]
  (let [query-params (merge base-params params)
        path         (path->with-query-string pathname query-params)
        headers      (->signed-headers "GET" path)
        url          (->url path)]
    (when-let [resp (client/get url
                                {:headers      headers
                                 :as           :json
                                 :content-type :json

                                 :cookie-policy :standard})]
      (->> resp
           :body
           (cske/transform-keys csk/->kebab-case)))))

(defn post-private
  [path params]
  (let [headers (->signed-headers "POST" path params)
        url     (->url path)
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

(defn fetch-balance
  "資産残高を取得"
  []
  (get-private "/v1/me/getbalance"))
#_(fetch-balance)

(defn fetch-collateral
  "証拠金の状態を取得"
  []
  (get-private "/v1/me/getcollateral"))
#_(fetch-collateral)

(defn create-order
  [params]
  (let [path       "/v1/me/sendchildorder"
        req-params (merge base-params params)]
    (post-private path req-params)))

(defn cancel-order
  [id]
  (let [path       "/v1/me/cancelchildorder"
        req-params (merge base-params {"child_order_acceptance_id" id})]
    (post-private path req-params)))

(defn fetch-orders
  "TODO オプションいろいろあるので用途ごとにラッパー関数を作成する"
  [& [query-params]]
  (get-private "/v1/me/getchildorders" query-params))
#_(fetch-orders)

(comment

  (def method "GET")
  (def path "/v1/me/getbalance")
  ;; (def path "/v1/me/getcollateral")
  (def headers (->signed-headers method path))
  (def url (->url path))

  (-> (client/get url
                  {:headers       headers
                   :as            :json
                   :content-type  :json
                   :cookie-policy :standard})
      :body)
  )

(comment

  (def query-params {:a 1 :b 2})
  (client/generate-query-string query-params)
  (path->with-query-string "/v1/hoge" query-params)

  )
