(ns rehearser.service.mistral
  (:require
   [jsonista.core :as j]
   [rehearser.http-client :as http-client]))

(def completions-endpoint "https://api.mistral.ai/v1/chat/completions")

(defn parse-text-chunk [chunk]
  (try
    (update chunk :text j/read-value j/keyword-keys-object-mapper)
    (catch Exception _ chunk)))

(defn- parse-sequential-json-chunk [chunk]
  (case (:type chunk)
    "text" (parse-text-chunk chunk)
    "thinking" (update chunk :thinking (fn [thinking]
                                         (mapv parse-sequential-json-chunk thinking)))
    (throw (ex-info "Expected some supported kind of chunk" {:type :mistral-response-format}))))

(defn- parse-json-content [content]
  (cond
    (string? content) (j/read-value content j/keyword-keys-object-mapper)
    (sequential? content) (mapv parse-sequential-json-chunk content)
    :else (throw (ex-info "Expected some supported format for content" {:type :mistral-response-format}))))

(defn completions-with-json-schema  
  [api-key prompt json-schema model]
  (let [req {:model model
             :reasoning_effort "high"
             :response_format {:type "json_schema"
                               :json_schema {:strict true
                                             :name "response-schema"
                                             :schema json-schema}}
             :messages [{:role "user"
                         :content prompt}]}
        start-time (System/nanoTime)
        response (http-client/post completions-endpoint
                                   {"authorization" (str "Bearer " api-key)}
                                   (j/write-value-as-string req))
        elapsed-ms (double (/ (- (System/nanoTime) start-time) 1000000))]
    (when-not (= 200 (.statusCode response))
      (throw (ex-info (str "Expected to get 200 status code from " completions-endpoint)
                      {:type :mistral-status
                       :response response})))
    {:request req
     :response (-> response
                   .body
                   (j/read-value j/keyword-keys-object-mapper)
                   (update-in [:choices 0 :message :content]
                              parse-json-content))
     :elapsed-ms elapsed-ms}))

(defn primary-answer-from-response
  "Get the primary response from the model

  Especially in the case where there's reasoning messages included, the response
  format is kind of complicated"
  [response]
  (-> response
      (get-in [:choices 0 :message :content])
      (->> (filter (fn [{:keys [type]}] (= type "text"))))
      first
      :text))
