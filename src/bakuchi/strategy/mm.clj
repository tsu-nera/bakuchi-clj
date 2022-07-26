(ns bakuchi.strategy.mm
  (:require
   [bakuchi.lib.exchange.ftx :refer [ftx] :rename {ftx ex}]
   [bakuchi.lib.exchange.interface :as if]
   [clojure.tools.logging :as log]))

(def position (atom "none"))
(def ask-status (atom "closed"))
(def bid-status (atom "closed"))

(def spread-entry 0.005)
(def spread-cancel 0.003)
(def product-code "BTC_JPY")

(defn update-position
  [current]
  (if (= current "none") "entry" "none"))

(defn update-status
  [current]
  (if (= current "open") "closed" "open"))

(defn logging-tick [tick]
  (let [ask         (:ask tick)
        bid         (:bid tick)
        spread      (:spread tick)
        spread-rate (:spread-rate tick)
        out         (format "spread=%.0f(%.5f), ask=%.0f, bid=%.0f"
                            spread spread-rate ask bid)]
    (log/debug out)))

(defn step-none->entry!
  []
  (let [{:keys [spread-rate] :as tick} (if/get-best-tick ex)]
    (logging-tick tick)
    (when (> spread-rate spread-entry)
      (swap! position update-position)
      (swap! ask-status update-status)
      (swap! bid-status update-status)
      (log/info "none -> entry"))))

(defn step-entry->none!
  []
  (let [{:keys [spread-rate] :as tick} (if/get-best-tick ex)]
    (println tick)
    (when (> spread-rate spread-cancel)
      (swap! position update-position)
      (swap! ask-status update-status)
      (swap! bid-status update-status)
      (log/info "entry -> none"))))

(defn step [_]
  (if (= @position "none")
    (step-none->entry!)
    (step-entry->none!)))
#_(step nil)

(comment
  (swap! position update-position)
  (swap! ask-status update-status)
  )
