(ns bakuchi.lib.time
  (:import
   (java.time
    Instant)))

(defn ->timestamp "unix時間"
  []
  (.toString (* (.getEpochSecond (Instant/now)) 1000)))
#_(->timestamp)
