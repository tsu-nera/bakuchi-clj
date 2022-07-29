(ns system
  (:require
   [integrant.repl.state :refer [config system]]
   [statecharts.core :as fsm]))

(defn trade-status []
  (-> system :bakuchi.service.trade/fsm fsm/value))
#_(trade-status)

(defn trade-state []
  (-> system :bakuchi.service.trade/fsm fsm/state))
#_(trade-state)
