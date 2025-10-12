(ns rehearser.routes
  (:require
   [reitit.core]
   [rehearser.http-service]))

(def method-order [:post :get :put :delete])
(def methods (set method-order))
(def method-rank (zipmap method-order (range)))

(defn routes []
  (->> (rehearser.http-service/make-app nil nil nil nil)
     :router
     reitit.core/routes 
     (mapcat (fn [[path data]]
               (for [m (keys data)
                     :when (contains? methods m)]
                 [m path])))
     (sort-by (fn [[m p]] [p (method-rank m)]))))
