(ns rehearser.api.account
  (:require
   [rehearser.hugsql :refer [def-db-fns]]
   [rehearser.service.account :as service])
  (:import
   (org.springframework.security.crypto.bcrypt BCrypt)))

(def-db-fns "rehearser/account.sql")

(def account-exceptions
  {:account-name-format {:status 400
                         :body "Invalid format for user name"}})

(defn handler-with-exceptions [handler ex-map]
  (fn [req]
    (try
      (handler req)
      (catch clojure.lang.ExceptionInfo e
        (if-let [handler (get ex-map (-> e ex-data :type))]
          (if (fn? handler) (handler (ex-data e)) handler)
          (throw e))))))

(defn whoami [{:keys [session]}]
  {:status 200
   :body (if (not (empty? session))
           (select-keys session [:account-id
                                 :account-name])
           {:account-id nil
            :account-name "anonymous"})})

(defn- login- [{:keys [db session]
                {:strs [username password]} :params}]
  (if-let [{:keys [id pwhash]} (first
                                (select-account-by-name db
                                                        {:name (service/normalized-name username)}))]
    (if (BCrypt/checkpw password pwhash)
      {:status 303
       :session (assoc session
                       :account-id id
                       :account-name username
                       :account-admin? false)
       :headers {"Location" "../index.html"}
       :body "Did log in"}
      {:status 303
       :headers {"Location" "../login.html"}
       :body "Password check failed"})
    {:status 303
     :headers {"Location" "../login.html"}
     :body "Password check failed"}))

(def login (handler-with-exceptions login- account-exceptions))

(defn admin-login [admin-pwhash]
  (if (empty? admin-pwhash)
    ;; No acceptable password hash -> admin login never possible
    (fn [_]
      {:status 403})
    ;; Actual implementation, installed only if it seems that a
    ;; useful password hash is available.
    (fn [{:keys [db session]
          {:strs [password]} :params}]
      (if (BCrypt/checkpw password admin-pwhash)
        {:status 200
         :session (assoc session
                         :account-id nil
                         :account-name "admin"
                         :account-admin? true)}
        {:status 403}))))

(defn logout [req]
  {:status 303
   :headers {"Location" "../login.html"}
   :session {}
   :body "Successfully logged out"})

(def passwd login)


(defn- signup- [{:keys [db session]
                 {:strs [username password]} :params}]
  (if-let [account (service/create-account! db
                                            (service/normalized-name username)
                                            password)]
    {:status 303
     :headers {"Location" "../login.html"}
     :body "Account created"}
    {:status 303
     :headers {"Location" "../signup.html#name-conflict=true"}
     :body "Requested name was already taken"}))

(def signup (handler-with-exceptions signup- account-exceptions))
