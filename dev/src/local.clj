(ns local
  (:require
   [bakuchi.core :as core]
   [hashp.core]
   [integrant.repl :as igr]
   [integrant.repl.state :refer [config system]]))

(defn start []
  (-> core/config-file
      core/load-config
      constantly
      igr/set-prep!)
  (igr/prep)
  (igr/init))

(defn stop []
  (igr/halt))

(defn restart []
  (igr/reset-all))

(comment
  (start)
  (stop)
  (restart))
