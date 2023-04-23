(ns rehearser.handler-test
  (:require
   [clojure.test :as t]
   [reitit.ring :as reitit-ring]

   [reitit.coercion.malli :as malli-coercion]
   [rehearser.http-service]

   [malli.core :as m]
   [malli.registry :as mr])
  (:import
   (java.io ByteArrayInputStream)
   (java.time Instant)))

(defn number->instant [num]
  (-> num (* 1000) Instant/ofEpochMilli))

(defn instant->number [i]
  (-> i .toEpochMilli (/ 1000)))

(def my-registry
  (-> m/default-registry
      (mr/composite-registry {:timestamp [(m/-simple-schema {:type :int
                                                             :pred #(instance? Instant %)})
                                          {:decode {:json number->instant}
                                           :encode {:json instant->number}}]})))
(def basic-body
  [:map
   [:id :int]
   [:name :string]
   [:timestamp :timestamp]])

(defn echo-handler [{{:keys [body]} :parameters
                     :as req}]
  {:status 200
   :body body
   :request req})

(defn post-json-text-request [uri body-text]
  {:request-method :post
   :uri uri
   :headers {"accept" "application/json"
             "content-type" "application/json"
             "content-length" (-> body-text .length str)}
   :body (-> body-text .getBytes ByteArrayInputStream.)})


(def routes ["/api/echo/:id" {:post {:handler echo-handler
                                     :parameters {:path {:id :int}
                                                  :body basic-body}
                                     :responses {200 basic-body}}}])

(def router (reitit-ring/router routes
                                {:data {:coercion (malli-coercion/create (assoc-in malli-coercion/default-options
                                                                                   [:options :registry]
                                                                                   my-registry))
                                        :middleware rehearser.http-service/api-adapter-middlewares}}))

(def app (reitit-ring/ring-handler router (reitit-ring/create-default-handler)))

(t/deftest basics
  (let [response (app (post-json-text-request
                       "/api/echo/1"
                       "{\"id\":1,\"name\":\"hello\",\"timestamp\":1681134173}"))
        request (:request response)]
    (t/is (= 200 (:status response)))
    (t/is (= {:id 1 :name "hello" :timestamp (Instant/ofEpochSecond 1681134173)}
             (get-in request [:parameters :body])))))

(t/deftest bad-json-gives-bad-request
  (let [response (app (post-json-text-request
                       "/api/echo/1"
                       "{\"id\":1,\"name\":\"hello,\"timestamp\":1681134173}"))]
    (t/is (= 400 (:status response)))
    (t/is (= "Malformed \"application/json\" request." (:body response)))))

(t/deftest bad-field-gives-bad-request
  (let [response (app (post-json-text-request
                       "/api/echo/1"
                       "{\"id\":\"1\",\"name\":\"hello\",\"timestamp\":1681134173}"))]
    (t/is (= 400 (:status response)))))

(t/deftest bad-url-gives-404
  (let [response (app (post-json-text-request
                       "/api/echoo/1"
                       "{\"id\":\"1\",\"name\":\"hello\",\"timestamp\":1681134173}"))]
    (t/is (= 404 (:status response)))))
