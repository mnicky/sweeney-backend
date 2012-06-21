(ns sweeney-backend.dbpool-test
  (:use clojure.test
        sweeney-backend.dbpool))

(deftest db-pool-test
  (let [db-spec {:classname "org.sqlite.JDBC"
                 :subprotocol "sqlite"
                 :subname ":memory:"
                 :user "test"
                 :password "test"}
        datasource (:datasource (db-pool db-spec :min-pool-size 5
                                                 :max-pool-size 20
                                                 :idle-time 18000
                                                 :excess-idle-time 1800))]
    (is (instance? javax.sql.DataSource datasource))
    (is (= 5 (.getMinPoolSize datasource)))
    (is (= 20 (.getMaxPoolSize datasource)))
    (is (= 18000 (.getMaxIdleTime datasource)))
    (is (= 1800 (.getMaxIdleTimeExcessConnections datasource)))))
