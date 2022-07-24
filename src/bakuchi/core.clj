(ns bakuchi.core
  (:require
   [bakuchi.app :as app]
   [chime.core :as chime])
  (:import
   (java.time
    Duration
    Instant)))

;; integrant configuration map
(def config-file "config.edn")

(def app (atom nil))

(defn start
  []
  (println "Bot started.")
  (deref @app))

(defn stop
  []
  (.close @@app))

(defn- make-periodic-seq
  [interval]
  (chime/periodic-seq
   (Instant/now)
   (Duration/ofSeconds interval)))

(defn init
  []
  (reset! app
          (delay (chime/chime-at
                  (make-periodic-seq 3)
                  app/step
                  {:on-finished
                   (fn []
                     (println "Bot finished."))}))))

;;

(comment
  (init)
  (start)
  (stop)
  )

(comment

  ;; 有限のスケジュール.いったん廃止.
  (defn- make-interval-seq [times interval]
    (let [now (Instant/now)]
      (->> (range 1 (+ 1 times))
           (map #(* interval %))
           (map (fn [x] (.plusSeconds now x)))
           (into []))))
  )
