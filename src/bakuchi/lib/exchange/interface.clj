(ns bakuchi.lib.exchange.interface)

;; 名前はccxtを参考にする.
;; https://docs.ccxt.com/en/latest/manual.html

(defprotocol Public
  (fetch-ticker [this]))

(defprotocol Private)
