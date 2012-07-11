(ns sweeney-backend.dbpool-test
  (:use clojure.test
        sweeney-backend.dbpool))

(deftest db-pool-test
  (let [db-spec {:classname "org.sqlite.JDBC"
                 :subprotocol "sqlite"
                 :subname ":memory:"
                 :user "test"
                 :password "test"}
        datasource (:datasource (db-pool db-spec :min-connections 5
                                                 :max-connections 20
                                                 :partitions 2
                                                 :connection-timeout 60
                                                 :pool-name "test-name"))]
    (is (instance? javax.sql.DataSource datasource))
    (is (= 5 (.getMinConnectionsPerPartition datasource)))
    (is (= 20 (.getMaxConnectionsPerPartition datasource)))
    (is (= 2 (.getPartitionCount datasource)))
    (is (= 60 (.getConnectionTimeout datasource java.util.concurrent.TimeUnit/SECONDS)))
    (is (= "test-name" (.getPoolName datasource)))))
