(ns bakuchi.tool
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io])
  (:import
    (javax.crypto
      Mac)
    (javax.crypto.spec
      SecretKeySpec)))


(defn load-edn
  [file-path]
  (when-let [file (io/resource file-path)]
    (-> file
        slurp
        edn/read-string)))


(defn secretKeyInst
  [key mac]
  (SecretKeySpec. (.getBytes key) (.getAlgorithm mac)))


(defn toHexString
  "Convert bytes to a String"
  [bytes]
  (apply str (map #(format "%02x" %) bytes)))


(defn sign
  "Returns the signature of a string with a given
  key, using a SHA-256 HMAC."
  [key text]
  (let [algo      "HMACSHA256"
        mac       (Mac/getInstance algo)
        secretKey (secretKeyInst key mac)]
    (-> (doto mac
          (.init secretKey)
          (.update (.getBytes text)))
        .doFinal
        toHexString)))


(comment

  (def text "hogehoge")
  (def secret-key (:api-secret (load-edn "creds.edn")))
  (def algo "HMACSHA256")

  (def key-spec (SecretKeySpec. (.getBytes secret-key) algo))

  (def mac (Mac/getInstance algo))

  (.init mac key-spec)
  (def signed-bytes
    (.doFinal mac (.getBytes text)))

  (=
   (toHexString signed-bytes)
   (sign secret-key text)
   )

  )
