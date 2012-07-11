(ns sweeney-backend.dbpool
  (:import com.jolbox.bonecp.BoneCPDataSource
           java.util.concurrent.TimeUnit))

(defn db-pool
  "Factory for creating a database pool, using bonecp library. Returns a map
  containing a key :datasource with a new instance of
  `com.jolbox.bonecp.BoneCPDataSource`.

  `spec` is a map of database connection options with these keys:

    :classname    - JDBC driver class
    :subprotocol  - database type
    :subname      - path to the database
    :user         - username
    :password     - password

  This function also accepts these optional parameters:

  :min-connections - Sets the minimum number of connections that
                     will be contained in every partition.
                     Defaults to 3.

  :max-connections - Sets the maximum number of connections that
                     will be contained in every partition. Setting
                     this to 5 with 3 partitions means you will
                     have 15 unique connections to the database.
                     Defaults to 15.

  :partitions - The number of partitions to use (see also
                http://is.gd/bonecp_partitions).
                Defaults to 1.

  :connection-timeout - Sets the maximum time (in seconds) to wait before
                        a call to getConnection is timed out. Setting this
                        to zero specifies that there is no timeout.
                        Defaults to 30 seconds.

  :pool-name - The name of the pool for thread names.
               Defaults to \"sweeney-backend-dbpool\"

  Example of use:

            (def my-spec
              {:classname \"com.mysql.jdbc.Driver\"
               :subprotocol \"mysql\"
               :subname \"//127.0.0.1:3306/mydb\"
               :user \"myaccount\"
               :password \"secret\"})

            (def my-pool
              (delay
                (db-pool my-spec :max-connections 30)))

            And then access `@my-pool` wherever you need access to the pool.

  (see also: http://is.gd/clj_jdbc_pool)
  "
  [spec & {:keys [min-connections max-connections partitions
                  connection-timeout pool-name]
           :or {min-connections 3
                max-connections 15
                partitions 1
                connection-timeout 30
                pool-name "sweeney-backend-dbpool"}}]
  (let [ds (doto (BoneCPDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUsername (:user spec))
               (.setPassword (:password spec))
               (.setMinConnectionsPerPartition min-connections)
               (.setMaxConnectionsPerPartition max-connections)
               (.setPartitionCount partitions)
               (.setConnectionTimeout connection-timeout TimeUnit/SECONDS)
               ;TODO: (.setLazyInit lazy) ?
               (.setPoolName pool-name))]
    {:datasource ds}))
