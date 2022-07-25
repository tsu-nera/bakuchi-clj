(ns bakuchi.service.logging
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [unilog.config  :refer [start-logging!]]))

(def config-map
  {:level     "info"
   :console   false
   :appenders [{:appender :console
                :encoder  :pattern
                :pattern  "[%d] %p - %m%n"}]})

(defmethod ig/init-key ::logger [_ _]
  (start-logging! config-map))

(comment
  (require '[clojure.tools.logging :as log])
  (log/info "test")
  )
