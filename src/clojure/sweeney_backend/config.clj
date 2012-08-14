(ns sweeney-backend.config
  (:require [sweeney-backend.dbpool :as dbpool]
            [sweeney-backend.events :as events]
            [sweeney-backend.threadpool :as threadpool]
            [overtone.at-at :as at]))

(def dev-db
  ""
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/sweeney-frontend_development"
   :user "postgres"
   :password ""})

(defonce ^{:doc ""}
  db-pool
  (delay (dbpool/db-pool dev-db :min-connections 3 :max-connections 15)))

(defonce ^{:doc ""}
  event-pool
  (events/init-action-pack (threadpool/t-pool :variable :size 6)))

(defonce ^{:doc ""}
  scheduled-pool
  (at/mk-pool :cpu-count (threadpool/cpu-count)))

(def ^:const last-n-stories
  ""
  5)

(def ^:const min-period
  ""
  (* 1000 60 15))

(def ^:const server-port
  ""
  4242)

(def ^:const api-ns
  ""
  "sweeney-backend.api")

(def ^:const log-level
  "Default log level."
  :debug)
