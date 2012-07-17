(ns sweeney-backend.core
  (:gen-class)
  (:require [sweeney-backend.actions :as actions]
            [sweeney-backend.config :as config]
            [sweeney-backend.server :as server]
            [sweeney-backend.utils :as utils]
            [taoensso.timbre :as log]))

(defn bootstrap
  "Adds default actions and runs the server."
  []
  (log/set-level! config/log-level)
  (actions/add-action #{:check-feed} actions/check-feed-action "check feed")
  (utils/on-shutdown actions/save-scheduled)
  (server/run)
 )

(defn -main
  "Main method that will start the backend."
  [& args]
  (bootstrap)
 )
