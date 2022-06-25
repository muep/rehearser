(ns rehearser.api.account
  (:require [jeesql.core :refer [defqueries]])
  (:import (org.springframework.security.crypto.bcrypt BCrypt)))

(defqueries "rehearser/account.sql")

(defn whoami [{:keys [session]}]
  {:status 200
   :body (if (not (empty? session))
           (select-keys session [:account-id
                                 :account-name])
           {:account-id nil
            :account-name "anonymous"})})

(defn login [{:keys [db session]
              {:strs [username password]} :params}]
  (if-let [{:keys [id pwhash]} (first
                                (select-account-by-name db
                                                        {:name username}))]
    (if (BCrypt/checkpw password pwhash)
      {:status 303
       :session (assoc session
                       :account-id id
                       :account-name username)
       :headers {"Location" "../index.html"}
       :body "Did log in"}
      {:status 303
       :headers {"Location" "../login.html"}
       :body "Password check failed"})
    {:status 303
     :headers {"Location" "../login.html"}
     :body "Password check failed"}))

(defn logout [req]
  {:status 303
   :headers {"Location" "../login.html"}
   :session {}
   :body "Successfully logged out"})

(def passwd login)

(def signup login)
