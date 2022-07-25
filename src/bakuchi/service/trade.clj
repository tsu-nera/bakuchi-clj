(ns bakuchi.service.trade
  (:require
   [bakuchi.strategy.mm :as app]
   [chime.core :as chime]
   [clojure.tools.logging :as log]
   [integrant.core :as ig])
  (:import
   (java.time
    Duration
    Instant)))

(defn- make-periodic-seq
  [interval]
  (chime/periodic-seq
   (Instant/now)
   (Duration/ofSeconds interval)))

(defn start
  []
  (log/info "Bot started.")
  (chime/chime-at
   (make-periodic-seq 5)
   app/step
   {:on-finished
    (fn []
      (log/info "Bot finished."))}))

(defmethod ig/init-key ::bot [_ _]
  (start))

(defmethod ig/halt-key! ::bot [_ app]
  (.close app))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  ;; 有限のスケジュール.いったん廃止.
  (defn- make-interval-seq [times interval]
    (let [now (Instant/now)]
      (->> (range 1 (+ 1 times))
           (map #(* interval %))
           (map (fn [x] (.plusSeconds now x)))
           (into []))))
  )
