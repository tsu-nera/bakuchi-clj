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
  [fsm]
  (log/info "=========================")
  (log/info "=== Trading Bot Start ===")
  (log/info "=========================")
  (chime/chime-at
   (make-periodic-seq 5)
   (fn [_]
     (app/step! fsm))
   {:on-finished
    (fn []
      (log/info "=== Trading Bot End ==="))}))

(defmethod ig/init-key ::fsm [_ _]
  (let [service (app/init)]
    (app/start! service)
    service))

(defmethod ig/init-key ::bot [_ fsm]
  (start fsm))

(defmethod ig/halt-key! ::bot [_ trade]
  (app/stop!)
  (.close trade))

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
