(ns sweeney-backend.config
  (:require [sweeney-backend.dbpool :as dbpool]
            [sweeney-backend.events :as events]
            [sweeney-backend.threadpool :as threadpool]))

(def db-spec {:classname "org.postgresql.JDBC"
              :subprotocol "postgresql"
              :subname "sweeney-frontend_development"
              :user "postgres"
              :password ""})

(def db-pool (delay (dbpool/db-pool db-spec :min-pool-size 3
                                            :max-pool-size 15)))

(def actions (events/init-action-pack (threadpool/t-pool :variable :size 6)))
