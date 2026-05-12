(ns rehearser.http-client
  (:import (java.net.http
            HttpClient
            HttpRequest
            HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers)
           (java.net URI)
           (java.time Duration)))

(defn- build-request [method url headers body]
  (let [builder (-> (HttpRequest/newBuilder)
                    (.uri (URI. url))
                    (.timeout (Duration/ofSeconds 30)))
        request (reduce (fn [builder [nom value]]
                          (.header builder nom value))
                        builder
                        headers)
        request (if body
                  (.POST request (HttpRequest$BodyPublishers/ofString body))
                  (.GET request))]
    (.build request)))

(defn get
  "Performs an HTTP GET request.

  Parameters:
    - url: string - The URL to request
    - headers: map - Map of header name (string) to value (string)

  Returns: java.net.http.HttpResponse<String> - The raw HTTP response object"
  [url headers]
  (let [client (HttpClient/newHttpClient)
        request (build-request :GET url headers nil)]
    (.send client request (HttpResponse$BodyHandlers/ofString))))

(defn post
  "Performs an HTTP POST request.

  Parameters:
    - url: string - The URL to request
    - headers: map - Map of header name (string) to value (string)
    - body: string - The request body as a string

  Returns: java.net.http.HttpResponse<String> - The raw HTTP response object"
  [url headers body]
  (let [client (HttpClient/newHttpClient)
        request (build-request :POST url headers body)]
    (.send client request (HttpResponse$BodyHandlers/ofString))))
