(ns bakuchi.core
  (:require
   [clojure.java.io :as io]
   [integrant.core :as ig]))

;; integrant configuration map
(def config-file "config.edn")

(defn load-config [config-file]
  (-> config-file
      io/resource
      slurp
      ig/read-string
      (doto
       ig/load-namespaces)))

(defn -main
  [& args]
  (-> config-file
      load-config
      ig/init))
