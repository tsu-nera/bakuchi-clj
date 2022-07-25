(ns bakuchi.strategy.mm
  (:require
   [bakuchi.lib.exchange.ftx :refer [ftx] :rename {ftx ex}]
   [bakuchi.lib.exchange.interface :as if]))

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

(defn step-none->entry!
  []
  (let [{:keys [spread-rate] :as tick} (if/get-best-tick ex)]
    (println tick)
    (when (> spread-rate spread-entry)
      (swap! position update-position)
      (swap! ask-status update-status)
      (swap! bid-status update-status)
      (println "none -> entry"))))

(defn step-entry->none!
  []
  (let [{:keys [spread-rate] :as tick} (if/get-best-tick ex)]
    (println tick)
    (when (> spread-rate spread-cancel)
      (swap! position update-position)
      (swap! ask-status update-status)
      (swap! bid-status update-status)
      (println "entry -> none"))))

(defn step
  [time]
  (if (= @position "none")
    (step-none->entry!)
    (step-entry->none!)))

(comment
  (swap! position update-position)
  (swap! ask-status update-status)
  )
