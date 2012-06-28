(ns sweeney-backend.server
  (:require [wakeful.core :as wakeful]
            [ring.adapter.jetty :as jetty]
            [clj-json.core :as json]
            [sweeney-backend.config :as config])
  (:import [java.net MalformedURLException UnknownHostException]
           [java.io FileNotFoundException]
           [org.xml.sax SAXParseException]
           [org.codehaus.jackson JsonParseException]
           [com.sun.syndication.io ParsingFeedException]))

(def ^:dynamic *server*
  "Var containing the running server."
  nil)

(defn error-response
  [e body]
  "Returns proper JSON response about error e, containing the given body.

  Uses these configuration options:
    - config/debug
  "
  (let [body (if config/debug (assoc body :debug (.toString e)) body)]
    {:status 200
     :headers {"Content-Type" "application/json;charset=utf-8"}
     :body (json/generate-string body)}))

(defn wrap-errors
  "Wraps the handler to return an appropriate response on error."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (error-response e
          (condp instance? e
            JsonParseException    {:status "error" :type "malformed-request"}
            AssertionError        {:status "error" :type "malformed-request"}

            MalformedURLException {:status "error" :type "malformed-url"}

            UnknownHostException  {:status "error" :type "not-found"}
            FileNotFoundException {:status "error" :type "not-found"}

            ParsingFeedException  {:status "error" :type "malformed-feed"}
            SAXParseException     {:status "error" :type "malformed-feed"}

                                  {:status "error" :type "other"}
           ))))))

(defn set-server
  "Atomicaly sets the *sever* var to the given `value`."
  [value]
  (alter-var-root (var *server*) (fn [_] value)))

(defn start
  "Starts server on the given port, with specified namespace as a REST api."
  [port namesp]
  {:pre [(string? namesp)]}
  (let [handler (wrap-errors (wakeful/wakeful :root namesp))]
    (jetty/run-jetty handler {:port port :join? false})))

(defn run
  "Runs the server if it's not running yet and returns true. If server
  wasn't run because it was already runnning, returns nil.

  Uses these configuration options:
    - config/server-port
    - config/api-ns
  "
  []
  (when (not *server*)
    (and (set-server (start config/server-port config/api-ns))
         true)))
