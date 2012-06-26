(ns sweeney-backend.config
  (:require [sweeney-backend.dbpool :as dbpool]
            [sweeney-backend.events :as events]
            [sweeney-backend.threadpool :as threadpool]
            [overtone.at-at :as at]))

(defonce dev-db {:classname "org.postgresql.Driver"
                 :subprotocol "postgresql"
                 :subname "sweeney-frontend_development"
                 :user "postgres"
                 :password ""})

(defonce db-pool (delay (dbpool/db-pool dev-db :min-pool-size 3
                                           :max-pool-size 15)))

(defonce event-pool (events/init-action-pack (threadpool/t-pool :variable :size 6)))

(defonce scheduled-pool (at/mk-pool :cpu-count (threadpool/cpu-count)))

(defonce last-n-stories 5)

(defonce min-period (* 1000 60 15))
