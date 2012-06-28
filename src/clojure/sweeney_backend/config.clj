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

(defonce db-pool
  ^{:doc ""}
  (delay (dbpool/db-pool dev-db :min-pool-size 3 :max-pool-size 15)))

(defonce event-pool
  ^{:doc ""}
  (events/init-action-pack (threadpool/t-pool :variable :size 6)))

(defonce scheduled-pool
  ^{:doc ""}
  (at/mk-pool :cpu-count (threadpool/cpu-count)))

(def last-n-stories
  ""
  5)

(def min-period
  ""
  (* 1000 60 15))

(def server-port
  ""
  4242)

(def api-ns
  ""
  "sweeney-backend.api")

(def debug
  ""
  false)
