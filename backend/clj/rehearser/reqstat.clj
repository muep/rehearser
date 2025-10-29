(ns rehearser.reqstat)

(defn reqstat-middleware+handler []
  (let [stats (atom {})]
    {:middleware
     (fn [handler]
       (fn [req]
         (let [template (-> req (get :reitit.core/match) :template)]
           (swap! stats (fn [s]
                          (update s [(:request-method req) template]
                                  (comp inc #(or % 0))))))
         (handler req)))
     :get-handler
     (fn [req]
       (let [s @stats]
         {:status 200
          :body s}))}))
