(ns bakuchi.strategy.mm
  (:require
   [bakuchi.lib.exchange.ftx :refer [ftx] :rename {ftx ex}]
   [bakuchi.lib.exchange.interface :as if]
   [clojure.tools.logging :as log]
   [statecharts.core :as fsm]))

(def position (atom "none"))
(def ask-status (atom "closed"))
(def bid-status (atom "closed"))

(def spread-entry 0.005)
(def spread-cancel 0.003)
(def product-code "BTC_JPY")

(defn update-position
  [current]
  (if (= current "none") "entry" "none"))

(defn update-status
  [current]
  (if (= current "open") "closed" "open"))

(defn logging-tick [tick]
  (let [ask         (:ask tick)
        bid         (:bid tick)
        spread      (:spread tick)
        spread-rate (:spread-rate tick)
        out         (format "spread=%.0f(%.5f), ask=%.0f, bid=%.0f"
                            spread spread-rate ask bid)]
    (log/debug out)))

(defn step-none->entry!
  []
  (let [{:keys [spread-rate] :as tick} (if/get-best-tick ex)]
    (logging-tick tick)
    (when (> spread-rate spread-entry)
      (swap! position update-position)
      (swap! ask-status update-status)
      (swap! bid-status update-status)
      (log/info "none -> entry"))))

(defn step-entry->none!
  []
  (let [{:keys [spread-rate] :as tick} (if/get-best-tick ex)]
    (logging-tick tick)
    (when (> spread-rate spread-cancel)
      (swap! position update-position)
      (swap! ask-status update-status)
      (swap! bid-status update-status)
      (log/info "entry -> none"))))

(defn step [_]
  (if (= @position "none")
    (step-none->entry!)
    (step-entry->none!)))
#_(step nil)

(comment
  (swap! position update-position)
  (swap! ask-status update-status)
  )

(comment

  (defn entry-orders? [_ {:keys [spread-rate]}]
    (> spread-rate spread-entry))

  (defn cancel-order? [_ {:keys [spread-rate]}]
    (> spread-rate spread-cancel))

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
     {:open   {:on {:tick {:guard  cancel-order?
                           :target :closed}}}
      :closed {:entry {:guard  both-closed?
                       :target [:> :none]}}}
     :initial :open})

  (def entry-bid-fsm
    {:id      :bid
     :states
     {:open   {:on {:tick {:guard  cancel-order?
                           :target :closed}}}
      :closed {:entry {:guard  both-closed?
                       :target [:> :none]}}}
     :initial :open})

  (def trade-fsm
    {:id      :position
     :initial :none
     :states
     {:none
      {:on
       {:tick {:target :entry
               :guard  entry-orders?}}
       :exit (fn-action-logging "open buy/sell positions")}
      :entry
      {:type :parallel
       :regions
       {:entry.ask entry-ask-fsm
        :entry.bid entry-bid-fsm}
       :exit (fn-action-logging "all position closed")}}})

  (defn init []
    (let [machine (fsm/machine trade-fsm)
          service (fsm/service machine)]
      (fsm/start service)
      service))

  (def service (init))

  (defn tick! [service]
    (let [tick  (if/get-best-tick ex)
          event {:type :tick}]
      (logging-tick tick)
      (fsm/send service (merge event tick))))
  #_(tick! service)

  )
