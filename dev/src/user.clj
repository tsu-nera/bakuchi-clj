(ns user)

(defn local []
  (require 'local)
  (in-ns 'local)
  (println ":switch to the local namespace")
  :loaded)
