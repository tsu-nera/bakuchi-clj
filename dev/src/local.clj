(ns local
  (:require
   [bakuchi.core :as core]
   [hashp.core]
   [integrant.repl :as igr]
   [integrant.repl.state :refer [config system]]))

(defn init []
  (-> core/config-file
      core/load-config
      constantly
      igr/set-prep!)
  (igr/prep))

(defn start []
  (igr/go))

(defn stop []
  (igr/halt))

(defn restart []
  (igr/reset-all))

(defn clear []
  (igr/clear))

(comment
  (init)
  (start)
  (stop)
  (clear)
  (restart))
