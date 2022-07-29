(ns bakuchi.strategy.mm
  (:require
   [bakuchi.lib.exchange.ftx :refer [ftx] :rename {ftx ex}]
   [bakuchi.lib.exchange.interface :as if]
   [bakuchi.lib.tool :refer [load-edn]]
   [clojure.tools.logging :as log]
   [statecharts.core :as fsm :refer [assign]]))

(def config-file "strategy.edn")
(def config (load-edn config-file))

(defn logging-tick! [tick]
  (let [ask         (:ask tick)
        bid         (:bid tick)
        spread      (:spread tick)
        spread-rate (:spread-rate tick)
        out         (format "spread=%.0f(%.5f), ask=%.0f, bid=%.0f"
                            spread spread-rate ask bid)]
    (log/debug out)))

(defn entry-orders? [_ {:keys [spread-rate]}]
  (> spread-rate (:spread-entry config)))

(defn ->padding-ask [ask]
  (let [margin (:margin-price config)]
    (- ask margin)))

(defn ->padding-bid [bid]
  (let [margin (:margin-price config)]
    (+ bid margin)))

(defn limit-order! [state side rate size]
  (when-let [resp (if/create-limit-order ex side rate size)]
    (let [id   (:id resp)
          rate (:price resp)
          size (:size resp)]
      (assoc state
             (keyword side)
             {:id id :rate rate :size size}))))

(defn buy-limit-order! [state {:keys [bid]}]
  (let [rate (->padding-bid bid)
        size (:lot config)]
    (log/info (format "buy limit order, bid=%.2f, size=%.4f" rate size))
    (limit-order! state "buy" rate size)))

(defn sell-limit-order! [state {:keys [ask]}]
  (let [rate (->padding-ask ask)
        size (:lot config)]
    (log/info (format "sell limit order, ask=%.2f, size=%.4f" rate size))
    (limit-order! state "sell" rate size)))

(defn cancel-buy-order! [state _]
  (let [id (get-in state [:buy :id])]
    (if/cancel-order ex id)))

(defn cancel-sell-order! [state _]
  (let [id (get-in state [:sell :id])]
    (if/cancel-order ex id)))

(defn- order-closed? [id]
  (when-let [resp (if/fetch-order ex id)]
    (= (:status resp) "closed")))

(defn buy-order-closed? [state _]
  (let [id (get-in state [:buy :id])]
    (order-closed? id)))

(defn sell-order-closed? [state _]
  (let [id (get-in state [:sell :id])]
    (order-closed? id)))

(defn- reentry-order? [spread-rate my-order-rate eff-padding-rate side]
  (let [spread-reentry (:spread-reentry config)
        ret            (and (> spread-rate spread-reentry)
                            (not= my-order-rate eff-padding-rate))]
    (when ret
      (log/info (format "re-entry %s order(my-rate=%.2f, eff-rate=%.2f)"
                        side my-order-rate eff-padding-rate)))
    ret))

(defn reentry-buy-order? [state {:keys [spread-rate bid]}]
  (let [order            (:buy state)
        my-order-rate    (:rate order)
        eff-padding-rate (->padding-bid bid)]
    (reentry-order? spread-rate my-order-rate eff-padding-rate "buy")))

(defn reentry-sell-order? [state {:keys [spread-rate ask]}]
  (let [order            (:sell state)
        my-order-rate    (:rate order)
        eff-padding-rate (->padding-ask ask)]
    (reentry-order? spread-rate my-order-rate eff-padding-rate "sell")))

(defn both-closed? [state _]
  (let [sell-state (get-in state [:_state :entry :entry.sell])
        buy-state  (get-in state [:_state :entry :entry.buy])]
    (and (= sell-state :closed) (= buy-state :closed))))

(defn fn-action-logging [message]
  (fn [& _]
    (log/info message)))

(defn spy-action [state event]
  (println state event))

(def entry-sell-fsm
  {:id      :entry.sell
   :states
   {:open   {:on   {:tick [{:guard  sell-order-closed?
                            :target :closed}
                           {:guard reentry-sell-order?
                            :actions
                            [cancel-sell-order!
                             (assign sell-limit-order!)]}]}
             :exit (fn-action-logging "sell order closed.")}
    :closed {:always {:guard  both-closed?
                      :target [:> :none]}}}
   :initial :open})

(def entry-buy-fsm
  {:id      :entry.buy
   :states
   {:open   {:on   {:tick [{:guard  buy-order-closed?
                            :target :closed}
                           {:guard reentry-buy-order?
                            :actions
                            [cancel-buy-order!
                             (assign buy-limit-order!)]}]}
             :exit (fn-action-logging "buy order closed.")}
    :closed {:always {:guard  both-closed?
                      :target [:> :none]}}}
   :initial :open})

(def trade-fsm
  {:id      :position
   :initial :none
   :states
   {:none
    {:on
     {:tick
      [{:target  :entry
        :guard   entry-orders?
        :actions [(assign sell-limit-order!)
                  (assign buy-limit-order!)]}]}}
    :entry
    {:type :parallel
     :regions
     {:entry.sell entry-sell-fsm
      :entry.buy  entry-buy-fsm}
     :exit (fn-action-logging "all position closed")}}})

(defn init []
  (let [machine (fsm/machine trade-fsm)]
    (fsm/service machine)))

(defn start! [service]
  (fsm/start service))

(defn get-eff-tick [state]
  (let [status     (:_state state)
        eff-amount (:effective-amount config)
        amount     {:eff-amount eff-amount}]
    (if (= status :none)
      (if/get-eff-tick ex amount)
      (let [buy-status  (:buy state)
            sell-status (:sell state)
            params      (merge amount
                               {:ask-price (:price sell-status)
                                :ask-size  (:amount sell-status)
                                :bid-price (:price buy-status)
                                :bid-size  (:amount buy-status)})]
        (if/get-eff-tick ex params)))))

(defn step! [service]
  (let [state    (fsm/state service)
        eff-tick (get-eff-tick state)
        event    (merge {:type :tick} eff-tick)]
    (logging-tick! eff-tick)
    (fsm/send service event)))

(defn stop! []
  (log/info "cancel all orders")
  (if/cancel-all-orders ex))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (def machine (fsm/machine trade-fsm))
  (def service (fsm/service machine))

  (def service (init))
  (fsm/start service)
  (step! service)

  (def state (fsm/state service))
  (def status (:_state state))
  (def buy-status (:buy state))
  (def sell-status (:sell state))
  )
