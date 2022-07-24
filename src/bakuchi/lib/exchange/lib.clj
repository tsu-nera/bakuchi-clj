(ns bakuchi.lib.exchange.lib)

(defn ->spread [ask bid]
  (- ask bid))

(defn ->spread-rate [ask bid]
  (/ (->spread ask bid) bid))

(defn ->best-tick [ask bid]
  (let [spread      (->spread ask bid)
        spread-rate (->spread-rate ask bid)]
    {:ask ask :bid bid :spread spread :spread-rate spread-rate}))
