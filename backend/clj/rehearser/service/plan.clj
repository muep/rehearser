(ns rehearser.service.plan
  (:require
   [jsonista.core :as j]
   [rehearser.service.exercise :as exercise]
   [rehearser.service.mistral :as mistral]
   [rehearser.service.rehearsal :as rehearsal]))

(def model "mistral-small-latest")

(def reference-selection-schema
  {:$schema "http://json-schema.org/draft-07/schema#"
   :type "object"
   :properties {:rehearsal-ids {:type "array"
                                :description "List of rehearsal IDs"
                                :items {:type "integer"}}
                :rationale {:type "string"
                            :description "Explanation for the chosen IDs"}}
   :required ["rehearsal-ids" "rationale"]
   :additionalProperties false})

(defn rehearsal-summaries [db whoami]
  (let [[{:keys [is-open id]} & past-rehearsals]
        (->> (rehearsal/find-all db whoami)
             (sort-by (fn [{:keys [is-open start-time]}]
                        [is-open start-time]))
             reverse
             (take 100))]
    (when is-open
      ;; We only get this context material if there actually is an
      ;; open rehearsal
      (let [open-rehearsal (rehearsal/find-rehearsal db whoami id)]
        {:current-rehearsal open-rehearsal
         :past-rehearsals past-rehearsals}))))

(def reference-prompt
  "

Your task is to look at the current rehearsal and then from the list
of past rehearsals, point out a subset of recent rehearsals that are
likely to match what they are currently rehearsing.

Especially look at the titles of rehearsals in the history. If they
have titles that seem similar the title of the current one, you MUST
include those in the set of results.

Form a list of rehearsal ids. Find five rehearsals at most.

Answer in JSON form according to schema and based on data provided.
")

(defn- infer-reference-rehearsals [api-key summaries]
  (assert (not (empty? summaries)))
  (let [prompt (str reference-prompt "\n\n" (j/write-value-as-string summaries))]
    (mistral/completions-with-json-schema api-key
                                          prompt
                                          reference-selection-schema
                                          model)))

(defn get-reference-rehearsals [db whoami api-key]
  (let [summaries (rehearsal-summaries db whoami)
        reference-inference (infer-reference-rehearsals api-key summaries)]
    {:current-rehearsal (:current-rehearsal summaries)
     :reference-rehearsal-ids (-> reference-inference
                                  :response
                                  mistral/primary-answer-from-response
                                  :rehearsal-ids)
     :reference-inference reference-inference}))

(def suggestion-schema
  {:$schema "http://json-schema.org/draft-07/schema#"
   :type "object"
   :properties {:exercises {:type "array"
                            :description "List of tune suggestions"
                            :items {:type "object"
                                    :properties {:exercise-id {:type "integer"}
                                                 :rationale {:type "string"}}
                                    :required ["exercise-id" "rationale"]}}}
   :required ["exercises"]
   :additionalProperties false})

(defn suggestion-context-package [db whoami {:keys [reference-rehearsal-ids
                                                    current-rehearsal]}]
  (assert (not (nil? current-rehearsal)))
  (assert (not (empty? reference-rehearsal-ids)))
  (let [reference-rehearsals (mapv (fn [id] (rehearsal/find-rehearsal db whoami id))
                                   reference-rehearsal-ids)]
    {:current-rehearsal current-rehearsal
     :reference-rehearsals reference-rehearsals}))

(def suggestion-prompt
  "Form a list of tune suggestions, referring to exercises in past
rehearsals, that are likely to be consistent or otherwise logical ways
to continue the currently ongoing rehearsal.

Answer in JSON form according to schema and based on data provided.
")

(defn enrich-suggestions [db whoami suggestions]
  (map (fn [{:keys [exercise-id rationale]}]
         (-> (exercise/find-by-id db whoami exercise-id)
             (merge {:rationale rationale})))
       suggestions))

(defn get-exercise-suggestions [db whoami api-key]
  (assert (not (empty? api-key)))
  (let [reference-step (get-reference-rehearsals db whoami api-key)
        context-package (suggestion-context-package db whoami reference-step)
        prompt (str suggestion-prompt "\n\n" context-package)
        suggestion-inference (mistral/completions-with-json-schema api-key
                                                       prompt
                                                       suggestion-schema
                                                       model)
        suggestions (mapv (fn [{:keys [exercise-id rationale]}]
                            (-> (exercise/find-by-id db whoami exercise-id)
                                first
                                (merge {:rationale rationale})))
                          (-> suggestion-inference
                              :response
                              mistral/primary-answer-from-response
                              :exercises))]
    (merge reference-step
           {:suggestion-inference suggestion-inference
            :suggestions suggestions})))

;; "An atom of account-id -> suggestion result, as returned from get-exercise-suggestions"
(defonce suggestions-store
  (atom {}))

(defn initiate-suggestions [db {:keys [account-id] :as whoami} api-key]
  (future
    (try
      (let [result (get-exercise-suggestions db whoami api-key)]
        (swap! suggestions-store assoc account-id result))
      (catch Exception e
        (swap! suggestions-store assoc account-id {:error (.getMessage e)})))))

(defn suggestions
  "Get exercise suggestions for the account, if any are available"
  [{:keys [account-id]}]
  (get @suggestions-store account-id))
