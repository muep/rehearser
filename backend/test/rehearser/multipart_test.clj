(ns rehearser.multipart-test
  (:require
   [clojure.test :as t]
   [rehearser.handler :as handler]
   [ring.mock.request :as mock]
   [clojure.java.io :as io]))

(defn- dummy-handler [request]
  {:status 200
   ;; This is expected to just flow back into the test case for
   ;; inspection, since it is not normally present in responses
   :request request})

;; Test routes for multipart testing
(def multipart-routes ["/api/multipart-test"
                       {:post {:parameters {:multipart {:files any?
                                                        :text-field string?
                                                        :num-field int?}
                                            #_:form #_[:map
                                                     [:text-field {:optional true} string?]]}
                               :handler dummy-handler}}])

(def app (:handler (handler/handler [] [] multipart-routes nil)))

(defmacro is-status
  [expected-status response]
  `(let [resp# ~response
         status# (:status resp#)
         body# (:body resp#)]
     (t/is (= ~expected-status status#)
           (str "Expected status " ~expected-status " but got " status#))
     (when-not (= ~expected-status status#)
       (let [body-content# (if (instance? java.io.InputStream body#)
                             (slurp body#)
                             body#)]
         (throw (ex-info (str "Expected status " ~expected-status " but got " status#)
                         {:type :rehearser.test/assertion-failed
                          :response-body body-content#}))))
     resp#))

(t/deftest multipart-basics-test
  (let [request (-> (mock/request :post "/api/multipart-test")
                    (mock/content-type "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
                    (mock/body (str "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                                    "Content-Disposition: form-data; name=\"text-field\"\r\n\r\n"
                                    "Hello World\r\n"
                                    "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                                    "Content-Disposition: form-data; name=\"num-field\"\r\n\r\n"
                                    "67\r\n"
                                    "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                                    "Content-Disposition: form-data; name=\"files\"; filename=\"test.txt\"\r\n"
                                    "Content-Type: text/plain\r\n\r\n"
                                    "Test file content\r\n"
                                    "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n")))
        response (-> request app)
        {{{:keys [files text-field num-field]} :multipart} :parameters :as req} (-> response :request)]
    (is-status 200 response)
    (t/is (= [67] num-field))
    (t/is (= ["Hello World"] text-field))
    (t/is (map? (first files)))
    (t/is (= #{:filename :content-type :tempfile :size} (-> files first keys set)))
    (t/is (= "Test file content" (-> files first :tempfile slurp)))))

(t/deftest multipart-multiple-files-test
  (let [request (-> (mock/request :post "/api/multipart-test")
                    (mock/content-type "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
                    (mock/body (str "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                                    "Content-Disposition: form-data; name=\"text-field\"\r\n\r\n"
                                    "Hello World\r\n"

                                    "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                                    "Content-Disposition: form-data; name=\"num-field\"\r\n\r\n"
                                    "67\r\n"

                                    "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                                    "Content-Disposition: form-data; name=\"files\"; filename=\"test.txt\"\r\n"
                                    "Content-Type: text/plain\r\n\r\n"
                                    "Test file #1content\r\n"

                                    "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\n"
                                    "Content-Disposition: form-data; name=\"files\"; filename=\"test.txt\"\r\n"
                                    "Content-Type: text/plain\r\n\r\n"
                                    "Test file #2 content\r\n"

                                    "------WebKitFormBoundary7MA4YWxkTrZu0gW--\r\n")))

        response (-> request app)
        {{{:keys [files text-field num-field]} :multipart} :parameters :as req} (-> response :request)]
    (is-status 200 response)
    (t/is (= [67] num-field))
    (t/is (= ["Hello World"] text-field))

    (t/is (vector? files))

    (doseq [f files]
      (t/is (map? f))
      (t/is (= #{:filename :content-type :tempfile :size} (-> f keys set))))))
