(ns bakuchi.strategy.mm
  (:require
   [bakuchi.lib.exchange.ftx :refer [ftx] :rename {ftx ex}]
   [bakuchi.lib.exchange.interface :as if]
   [bakuchi.lib.tool :refer [load-edn]]
   [clojure.tools.logging :as log]
   [statecharts.core :as fsm :refer [assign]]))

(def config-file "strategy.edn")
(def config (load-edn config-file))

(defn logging-tick [tick]
  (let [ask         (:ask tick)
        bid         (:bid tick)
        spread      (:spread tick)
        spread-rate (:spread-rate tick)
        out         (format "spread=%.0f(%.5f), ask=%.0f, bid=%.0f"
                            spread spread-rate ask bid)]
    (log/debug out)))

(defn entry-orders? [_ {:keys [spread-rate]}]
  (> spread-rate (:spread-entry config)))

(defn buy-limit-order! [state {:keys [bid]}]
  (let [lot   (:lot config)
        delta (:mm-delta config)]
    (when-let [resp (if/create-limit-order ex "buy" lot (+ bid delta))]
      (assoc state :bid-id (:id resp)))))

(defn sell-limit-order! [state {:keys [ask]}]
  (let [lot   (:lot config)
        delta (:mm-delta config)]
    (when-let [resp (if/create-limit-order ex "sell" lot (- ask delta))]
      (assoc state :ask-id (:id resp)))))

(defn- order-closed? [id]
  (when-let [resp (if/fetch-order ex id)]
    (= (:status resp) "closed")))

(defn buy-order-closed? [state _]
  (let [id (:bid-id state)]
    (order-closed? id)))

(defn sell-order-closed? [state _]
  (let [id (:ask-id state)]
    (order-closed? id)))

(defn cancel-order? [_ {:keys [spread-rate]}]
  (> spread-rate (:spread-cancel config)))

(defn both-closed? [state _]
  (let [ask-state (get-in state [:_state :entry :entry.ask])
        bid-state (get-in state [:_state :entry :entry.bid])]
    (and (= ask-state :closed) (= bid-state :closed))))

(defn fn-action-logging [message]
  (fn [& _]
    (log/info message)))

(def entry-ask-fsm
  {:id      :ask
   :states
   {:open   {:on   {:tick {:guard  buy-order-closed?
                           :target :closed}}
             :exit (fn-action-logging "buy order closed.")}
    :closed {:always {:guard  both-closed?
                      :target [:> :none]}}}
   :initial :open})

(def entry-bid-fsm
  {:id      :bid
   :states
   {:open   {:on   {:tick {:guard  sell-order-closed?
                           :target :closed}}
             :exit (fn-action-logging "sell order closed.")}
    :closed {:always {:guard  both-closed?
                      :target [:> :none]}}}
   :initial :open})

(def trade-fsm
  {:id      :position
   :initial :none
   :states
   {:none
    {:on
     {:tick {:target  :entry
             :guard   entry-orders?
             :actions [(assign sell-limit-order!)
                       (assign buy-limit-order!)]}}
     :exit (fn-action-logging "open buy/sell positions")}
    :entry
    {:type :parallel
     :regions
     {:entry.ask entry-ask-fsm
      :entry.bid entry-bid-fsm}
     :exit (fn-action-logging "all position closed")}}})

(defn init []
  (let [machine (fsm/machine trade-fsm)]
    (fsm/service machine)))

(defn start! [service]
  (fsm/start service))

(defn step! [service]
  (let [tick  (if/get-best-tick ex)
        event {:type :tick}]
    (logging-tick tick)
    (fsm/send service (merge event tick))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def machine (fsm/machine trade-fsm))
  (def service (fsm/service machine))

  (def service (init))
  (fsm/start service)
  (step! service)
  )
