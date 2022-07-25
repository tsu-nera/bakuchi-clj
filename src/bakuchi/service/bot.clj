(ns bakuchi.service.bot
  (:require
   [bakuchi.strategy.mm :as app]
   [chime.core :as chime]
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

(defn init
  []
  (chime/chime-at
   (make-periodic-seq 3)
   app/step
   {:on-finished
    (fn []
      (println "Bot finished."))}))

(defmethod ig/init-key ::app [_ _]
  (init))

(defmethod ig/halt-key! ::app [_ app]
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
