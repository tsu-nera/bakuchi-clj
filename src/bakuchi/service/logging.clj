(ns bakuchi.service.logging
  (:require
   [integrant.core :as ig]
   [unilog.config  :refer [start-logging!]]))

(def config-map {:level "info" :console true})

(defmethod ig/init-key ::logger [_ _]
  (start-logging! config-map))
