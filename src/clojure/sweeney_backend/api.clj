(ns sweeney-backend.api
  (:require [sweeney-backend.actions :as actions]
            [sweeney-backend.feeds :as feeds]))

(defn check-feed!
  ""
  [request]
  {:pre [(map? (:body request))]}
  (let [url ((:body request) "url")]
    ;(actions/fire :check-feed url)
    (feeds/parse-feed url)
    {:status 200 :body {:status "ok"}}
   ))
