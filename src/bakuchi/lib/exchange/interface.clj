(ns bakuchi.lib.exchange.interface)

;; 名前はccxtを参考にする.
;; https://docs.ccxt.com/en/latest/manual.html

(defprotocol Public
  (fetch-ticker [this])
  (fetch-orderbook [this] "板情報の取得"))

(defprotocol Private
  (fetch-balance [this])
  (fetch-order [this id])
  (fetch-open-orders [this] "未約定の注文一覧を取得")
  (fetch-closed-orders [this] "約定した注文一覧を取得")
  (create-order [this params] "新規注文")
  (cancel-order [this id] "注文をキャンセル")
  (cancel-all-orders [this] "全ての注文をキャンセル"))

(defprotocol Library
  (get-best-tick [this] "最良価格を取得")
  (get-eff-tick [this params] "実効価格を板情報から計算")
  (create-limit-order [this side rate size] "指値注文")
  (create-market-order [this side size] "成行注文")
  (modify-order [this id rate size] "注文内容の修正"))
