(ns bakuchi.service.logging
  (:require
   [bakuchi.lib.tool :refer [load-edn]]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [unilog.config  :refer [start-logging!]]))

(def config-file "logging.edn")

(defmethod ig/init-key ::logger [_ _]
  (-> config-file
      load-edn
      start-logging!))

(comment
  (require '[clojure.tools.logging :as log])
  (log/info "test"))
