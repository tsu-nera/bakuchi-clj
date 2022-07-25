(ns bakuchi.lib.exchange.interface)

;; 名前はccxtを参考にする.
;; https://docs.ccxt.com/en/latest/manual.html

(defprotocol Public
  (fetch-ticker [this])
  (fetch-orderbook [this]))

(defprotocol Private
  (fetch-balance [this])
  (fetch-order [this id])
  (create-order [this])
  (cancel-order [this]))

(defprotocol Library
  (get-best-tick [this] "最良価格を取得")
  (get-eff-tick [this] "実効価格を板情報から計算"))
