(ns rehearser.handler
  (:require
   [reitit.ring :as reitit-ring]
   [reitit.ring.middleware.exception :refer [exception-middleware]]
   [reitit.ring.middleware.parameters :refer [parameters-middleware]]
   [reitit.ring.coercion :refer [coerce-request-middleware
                                 coerce-response-middleware]]

   [reitit.coercion :refer [compile-request-coercers]]
   [reitit.coercion.malli :as malli-coercion]

   [muuntaja.middleware :refer [wrap-format-negotiate
                                wrap-format-response
                                wrap-format-request]]
   [malli.core :as m]
   [malli.registry :as mr])
  (:import (java.time Instant)))

(defn number->instant [num]
  (-> num Instant/ofEpochSecond))

(defn instant->number [i]
  (-> i .toEpochSecond))

(def schema-registry
  (-> m/default-registry
      (mr/composite-registry {:timestamp [(m/-simple-schema {:type :int
                                                             :pred #(instance? Instant %)})
                                          {:decode {:json number->instant}
                                           :encode {:json instant->number}}]})))

(def coercion (malli-coercion/create (assoc-in malli-coercion/default-options
                                               [:options :registry]
                                               schema-registry)))

(defn wrap-disable-cache [handler]
  (fn [req]
    (-> (handler req)
        (assoc-in [:headers "Cache-Control"] "no-store"))))

(def api-adapter-middlewares
  [wrap-disable-cache
   wrap-format-negotiate
   wrap-format-response
   exception-middleware
   wrap-format-request
   parameters-middleware
   coerce-request-middleware
   coerce-response-middleware])

(defn handler [before-middlewares after-middlewares routes default-handler]
  (reitit-ring/ring-handler
   (reitit-ring/router routes
                       {:data {:coercion coercion
                               :compile compile-request-coercers
                               :middleware (vec (concat before-middlewares
                                                        api-adapter-middlewares
                                                        after-middlewares))}})
   (if (fn? default-handler)
     (reitit-ring/routes default-handler (reitit-ring/create-default-handler))
     (reitit-ring/create-default-handler))))
