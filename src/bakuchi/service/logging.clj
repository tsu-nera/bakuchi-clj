(ns bakuchi.service.logging
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [unilog.config  :refer [start-logging!]]))

(def logging-pattern "%d:%p: %m%n")
(def logging-file "logs/trade.log")

(def config-map
  {:level     "debug"
   :console   false
   :appenders [{:appender :file
                :file     logging-file
                :encoder  :pattern
                :pattern  logging-pattern}]
   :overrides {"org.apache.http" :error}})

(defmethod ig/init-key ::logger [_ _]
  (start-logging! config-map))

(comment
  (require '[clojure.tools.logging :as log])
  (log/info "test"))
