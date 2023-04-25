(ns rehearser.handler-test
  (:require
   [clojure.test :as t]
   [rehearser.handler])
  (:import
   (java.io ByteArrayInputStream)
   (java.time Instant)))

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

(def app (rehearser.handler/handler [] [] routes nil))

(t/deftest basics
  (let [response (app (post-json-text-request
                       "/api/echo/1"
                       "{\"id\":1,\"name\":\"hello\",\"timestamp\":1681134173}"))
        request (:request response)]
    (t/is (= 200 (:status response)))
    (t/is (= {:id 1 :name "hello" :timestamp (Instant/ofEpochSecond 1681134173)}
             (get-in request [:parameters :body])))))

(t/deftest bad-json->bad-request
  (let [response (app (post-json-text-request
                       "/api/echo/1"
                       "{\"id\":1,\"name\":\"hello,\"timestamp\":1681134173}"))]
    (t/is (= 400 (:status response)))
    (t/is (= "Malformed \"application/json\" request." (:body response)))))

(t/deftest bad-field->-bad-request
  (let [response (app (post-json-text-request
                       "/api/echo/1"
                       "{\"id\":\"1\",\"name\":\"hello\",\"timestamp\":1681134173}"))]
    (t/is (= 400 (:status response)))))

(t/deftest bad-url->404
  (let [response (app (post-json-text-request
                       "/api/echoo/1"
                       "{\"id\":\"1\",\"name\":\"hello\",\"timestamp\":1681134173}"))]
    (t/is (= 404 (:status response)))))
