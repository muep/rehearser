(ns rehearser.api
  (:require [rehearser.api.account :as account]
            [rehearser.exp :as exp]))

(def routes [["/login" {:post account/login}]
             ["/signup" {:post account/signup}]
             ["/"]
             ["/logout" {:post account/logout}]
             ["/whoami" {:get account/whoami}]
             ["/params" {:get exp/params-get
                         :post exp/params-post}]])
