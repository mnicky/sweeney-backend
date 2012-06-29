(ns sweeney-backend.api
  (:require [sweeney-backend.actions :as actions]
            [sweeney-backend.config :as config]
            [sweeney-backend.feeds :as feeds]))

(defn parse-feed!
  "docstring here..."
  [request]
  {:pre [(map? (:body request))]}
  (let [url ((:body request) "url")
        feed (feeds/parse-feed url)]
    {:status 200 :body {:status "ok" :feed (:info feed)}}
   ))

(defn check-feed!
  "docstring here...

  This function uses these configuration options:
    - config/db-pool
  "
  [request]
  {:pre [(map? (:body request))]}
  (let [url     ((:body request) "url")
        actions (seq (actions/fire :check-feed url))
        feed-id (when actions @(val (first actions)))
        feed    (when feed-id (feeds/find-feed-by-id @config/db-pool feed-id))]
    {:status 200 :body {:status "ok" :feed feed}}
   ))
