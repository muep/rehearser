(ns rehearser.handler-progressive-test
  (:require
   [clojure.set :as set]
   [clojure.test :as t]
   [reitit.ring :as reitit-ring]

   [reitit.ring.middleware.parameters :refer [parameters-middleware]]

   [muuntaja.middleware :refer [wrap-format]]

   [reitit.coercion.malli :as malli-coercion]
   [reitit.ring.coercion :refer [coerce-exceptions-middleware
                                 coerce-request-middleware
                                 coerce-response-middleware]]

   [malli.core :as m])

  (:import
   (java.io ByteArrayInputStream)
   (java.time Instant)))

;; This test file attempts to lay out the assumptions and other
;; thoughts going into the design of the middleware stack

(defn str->body [s]
  (-> s .getBytes ByteArrayInputStream.))

(defn echo-handler [req]
  {:status 200
   :body {:status "ok"}
   :enriched-request req})

(def routes ["/api/echo/:id" {:get {:handler echo-handler}
                              :post {:handler echo-handler}}])

(def get-request {:request-method :get
                  :headers {"accept" "application/json"}
                  :uri "/api/echo/4"})

(defn post-request []
  (let [body-text "{\"id\":1,\"name\":\"hello\",\"timestamp\":1681134173}"]
    {:request-method :post
     :uri "/api/echo/4"
     :headers {"accept" "application/json"
               "content-type" "application/json"
               "content-length" (-> body-text .length str)}
     :body (str->body body-text)}))

;; This is kind of assuming the input as described in this file, so it
;; might be unrealistically minimal
(def baseline-get-keys
  #{:request-method :uri :headers :path-params :reitit.core/match :reitit.core/router})

(def baseline-post-keys (set/union baseline-get-keys #{:body :headers}))

;; The default setup is pretty bare-bones. Reitit simply adds some
;; information for us on how it arrived in its routing choice
(t/deftest empty-baseline-get
  (let [router (reitit-ring/router routes)
        app (reitit-ring/ring-handler router)
        request-in get-request
        response (app request-in)
        request-out (-> response
                        :enriched-request)]
    (t/is (not (nil? request-out))
          "Echo mechanism problem")
    (t/is (= baseline-get-keys
             (-> request-out keys set))
          "Unexpected set of keys")
    (t/is (= "4" (-> request-out :path-params :id)))
    (t/is (= router (-> request-out :reitit.core/router))
          "Router reference is doing something weird")
    (t/is (= (-> request-in :uri) (-> request-out :uri))
          "URI was changed unexpectedly")
    (t/is (= {:status "ok"} (-> response :body))
          "Reponse was changed unexpectedly")))

;; For the post method, there is just one more key getting delivered
;; to the handler.
(t/deftest empty-baseline-post
  (let [router (reitit-ring/router routes)
        app (reitit-ring/ring-handler router)
        request-in (post-request)
        request-out (-> (app request-in)
                        :enriched-request)]
    (t/is (= baseline-post-keys
             (-> request-out keys set))
          "Unexpected set of keys")
    (t/is (= (-> request-in :body) (-> request-out :body))
          "Body went missing or mangled")))

;; Now for the first addition: Let's add the parameters middleware
(t/deftest parameters-post
  (let [router (reitit-ring/router routes
                                   {:data {:middleware [parameters-middleware]}})
        app (reitit-ring/ring-handler router)
        request-in (post-request)
        response (app request-in)
        request-out (-> response
                        :enriched-request)]
    ;; Three new keys in request
    (t/is (= (set/union baseline-post-keys
                        #{:params :form-params :query-params})
             (-> request-out keys set))
          "Unexpected set of keys")
    (t/is (= (-> request-in :body) (-> request-out :body))
          "Body went missing or mangled")
    (let [{:keys [query-params form-params params]} request-out]
      (t/is (empty? query-params))
      (t/is (empty? form-params))
      (t/is (empty? params)))
    (t/is (= {:status "ok"} (-> response :body))
          "Response was changed unexpectedly")))

;; Second change: Add muuntaja/wrap-format
(t/deftest muuntaja-post
  (let [router (reitit-ring/router routes
                                   {:data {:middleware [parameters-middleware
                                                        wrap-format]}})
        app (reitit-ring/ring-handler router)
        request-in (post-request)
        response (app request-in)
        request-out (-> response
                        :enriched-request)]
    ;; Three new keys in request
    (t/is (= (set/union baseline-post-keys
                        #{:params :form-params :query-params
                          :body-params :muuntaja/request :muuntaja/response})
             (-> request-out keys set))
          "Unexpected set of keys")
    ;; Should still be same as in first step
    (t/is (= "4" (-> request-out :path-params :id)))
    ;; Also still expecting this to be empty
    (t/is (= {} (-> request-out :params)))

    ;; Body should be the same object as what went in, even though it
    ;; is drained by muuntaja
    (t/is (= (-> request-in :body) (-> request-out :body))
          "Body went missing or mangled")
    (t/is (= "" (-> request-out :body slurp))
          "Muuntaja should have drained the input stream")

    (t/is (= {:name "hello" :id 1 :timestamp 1681134173} (-> request-out :body-params))
          "Muuntaja did not decode the expected kind of body-params")
    (t/is (= "{\"status\":\"ok\"}" (-> response :body slurp))
          "Expecting a JSON-formatted response")))

;; Third level: coerce parameters. Now need to do some extra footwork
;; to prepare types and suchlike
(def request-body
  [:map
   [:id :int]
   [:name :string]
   [:timestamp :int]])

(def response-body
  [:map
   [:status :string]])


(def coercable-routes ["/api/echo/:id" {:get {:handler echo-handler
                                              :parameters {:path {:id :int}}}
                                        :post {:handler echo-handler
                                               :parameters {:path {:id :int}
                                                            :body request-body}
                                               :responses {200 response-body}}}])

(t/deftest coerced-post
  (let [router (reitit-ring/router coercable-routes
                                   {:data {:coercion malli-coercion/coercion
                                           :middleware [parameters-middleware
                                                        wrap-format
                                                        coerce-exceptions-middleware
                                                        coerce-request-middleware
                                                        coerce-response-middleware]}})
        app (reitit-ring/ring-handler router)
        request-in (post-request)
        response (app request-in)
        request-out (-> response
                        :enriched-request)]
    ;; One more :parameters key in the request
    (t/is (= (set/union baseline-post-keys
                        #{:params :form-params :query-params
                          :body-params :muuntaja/request :muuntaja/response
                          :parameters})
             (-> request-out keys set))
          "Unexpected set of keys")
    ;; Should still be same as in first step
    (t/is (= "4" (-> request-out :path-params :id)))
    ;; Coerced parameters have this other path
    (t/is (= 4 (-> request-out :parameters :path :id)))

    ;; Body should be the same object as what went in, even though it
    ;; is drained by muuntaja
    (t/is (= (-> request-in :body) (-> request-out :body))
          "Body went missing or mangled")
    (t/is (= "" (-> request-out :body slurp))
          "Muuntaja should have drained the input stream")

    ;; body-params should be as it was, but we should also have that :parameters :body
    (t/is (= {:name "hello" :id 1 :timestamp 1681134173} (-> request-out :body-params))
          "Muuntaja did not decode the expected kind of body-params")
    (t/is (= {:name "hello" :id 1 :timestamp 1681134173} (-> request-out :parameters :body)))

    (t/is (= "{\"status\":\"ok\"}" (-> response :body slurp))
          "Expecting a JSON-formatted response")))


;; Final level - coerce into custom types
(defn number->instant [num]
  (-> num (* 1000) Instant/ofEpochMilli))

(defn instant->number [i]
  (-> i .toEpochMilli (/ 1000)))

(def timestamp-schema (m/-simple-schema {:type :int
                                         :pred #(instance? Instant %)}))


(def timestamp [timestamp-schema {:decode {:json number->instant}
                                  :encode {:json instant->number}}])


(def advanced-request-body
  [:map
   [:id :int]
   [:name :string]
   [:timestamp timestamp]])

(def advanced-coercable-routes ["/api/echo/:id" {:get {:handler echo-handler
                                                       :parameters {:path {:id :int}}}
                                                 :post {:handler echo-handler
                                                        :parameters {:path {:id :int}
                                                                     :body advanced-request-body}
                                                        :responses {200 response-body}}}])

(t/deftest advanced-coerced-post-test
  (let [router (reitit-ring/router advanced-coercable-routes
                                   {:data {:coercion malli-coercion/coercion
                                           :middleware [parameters-middleware
                                                        wrap-format
                                                        coerce-exceptions-middleware
                                                        coerce-request-middleware
                                                        coerce-response-middleware]}})
        app (reitit-ring/ring-handler router)
        request-in (post-request)
        response (app request-in)
        request-out (-> response
                        :enriched-request)]
    (t/is (= 200 (-> response :status)))
    ;; One more :parameters key in the request
    (t/is (= (set/union baseline-post-keys
                        #{:params :form-params :query-params
                          :body-params :muuntaja/request :muuntaja/response
                          :parameters})
             (-> request-out keys set))
          "Unexpected set of keys")
    ;; Should still be same as in first step
    (t/is (= "4" (-> request-out :path-params :id)))
    ;; Coerced parameters have this other path
    (t/is (= 4 (-> request-out :parameters :path :id)))

    ;; Body should be the same object as what went in, even though it
    ;; is drained by muuntaja
    (t/is (= (-> request-in :body) (-> request-out :body))
          "Body went missing or mangled")
    (t/is (= "" (-> request-out :body slurp))
          "Muuntaja should have drained the input stream")

    ;; body-params should be as it was, but we should also have
    ;; that :parameters :body and it should now have an instance of an
    ;; Instant
    (t/is (= {:name "hello" :id 1 :timestamp 1681134173}
             (-> request-out :body-params))
          "Muuntaja did not decode the expected kind of body-params")
    (t/is (= {:name "hello" :id 1 :timestamp (Instant/ofEpochMilli 1681134173000)}
             (-> request-out :parameters :body)))

    (t/is (= "{\"status\":\"ok\"}" (-> response :body slurp))
          "Expecting a JSON-formatted response")))
