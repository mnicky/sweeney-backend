(ns sweeney-backend.server
  (:require [wakeful.core :as wakeful]
            [ring.adapter.jetty :as jetty]
            [clj-json.core :as json]
            [sweeney-backend.config :as config]
            [taoensso.timbre :as log])
  (:import [java.net MalformedURLException UnknownHostException]
           [java.io FileNotFoundException]
           [org.xml.sax SAXParseException]
           [org.codehaus.jackson JsonParseException]
           [com.sun.syndication.io ParsingFeedException]))

(defonce ^{:doc "Var containing the running server." :dynamic true}
  *server* nil)

(defn error-response
  [e body]
  "Returns proper JSON response about error e, containing the given body.
  If the config/log-level is set to the :debug or higher, then the exception
  description will be added to the response.
  "
  (let [body (if (log/logging-enabled? :debug) (assoc body :debug (.toString e)) body)]
    {:status 200
     :headers {"Content-Type" "application/json;charset=utf-8"}
     :body (json/generate-string body)}))

(defn get-root-cause
  "Returns root cause of the supplied exception."
  [e]
  {:pre [(instance? Throwable e)]}
  (if-not (.getCause e)
    e
    (recur (.getCause e))))

(defn cond-error
  "Returns information according to the type of the error."
  [e]
  (condp instance? e
    JsonParseException    {:status "error" :type "malformed-request"}
    AssertionError        {:status "error" :type "malformed-request"}

    MalformedURLException {:status "error" :type "malformed-url"}

    UnknownHostException  {:status "error" :type "not-found"}
    FileNotFoundException {:status "error" :type "not-found"}

    ParsingFeedException  {:status "error" :type "malformed-feed"}
    SAXParseException     {:status "error" :type "malformed-feed"}

                          {:status "error" :type "other"}
   ))

(defn wrap-errors
  "Wraps the handler to return an appropriate response on error."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (log/error e "error caught on the request handler")
        (error-response e (cond-error (get-root-cause e)))))))

(defn set-server
  "Atomically sets the *sever* var to the given `value`."
  [value]
  (alter-var-root (var *server*) (fn [_] value)))

(defn start
  "Starts server on the given port, with specified namespace as a REST api."
  [port namesp]
  {:pre [(string? namesp)]}
  (let [handler (wrap-errors (wakeful/wakeful :root namesp))]
    (try
      (log/info "starting the server on port" port)
      (jetty/run-jetty handler {:port port :join? false})
    (catch Exception e
      (log/fatal e "exception caught when starting the server")))))

(defn stop
  "Stops the server and returns nil."
  []
  (try
    (when *server*
      (.stop *server*))
  (finally
    (set-server nil))))

(defn running?
  "Returns whether the server is running."
  []
  (if *server*
    (.isRunning *server*)
    false))

(defn run
  "Runs the server if it's not running yet and returns true. If server
  wasn't run because it was already running, returns nil.

  Uses these configuration options:
    - config/server-port
    - config/api-ns
  "
  []
  (if-not *server*
    (let [server (start config/server-port config/api-ns)]
      (set-server server)
      true)
    (log/info "server not started because it's already running")))
