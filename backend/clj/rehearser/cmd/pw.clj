(ns rehearser.cmd.pw
  (:import (org.springframework.security.crypto.bcrypt BCrypt)))

(defn check [{:keys [subcmd-args]}]
  (-> (System/console)
       (.readPassword "Password: " nil)
       String.
       (BCrypt/checkpw (first subcmd-args))
       println))

(defn hashpw [_]
   (-> (System/console)
       (.readPassword "Password: " nil)
       String.
       (BCrypt/hashpw (BCrypt/gensalt 12))
       println))
