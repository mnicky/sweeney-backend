(ns sweeney-backend.dbpool
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn db-pool
  "Factory for creating a database pool, using c3p0 library. Returns a map
  containing a key :datasource with a new instance of
  `com.mchange.v2.c3p0.ComboPooledDataSource`.

  `spec` is a map of database connection options with these keys:

    :classname    - JDBC driver class
    :subprotocol  - database type
    :subname      - path to the database
    :user         - username
    :password     - password

  This function also accepts these optional parameters:

  :min-pool-size - Minimum number of connections a pool will maintain at any
                   given time. Defaults to 3.

  :max-pool-size - Maximum number of connections a pool will maintain at any
                   given time. Defaults to 15.

  :idle-time - Defines how many seconds a connection should be permitted
               to go unused before being culled from the pool. Zero means
               idle connections never expire. Defaults to 3 hours.

  :excess-idle-time - Defines number of seconds that connections in excess
                      of min-pool-size should be permitted to remain idle
                      in the pool before being culled. Zero means that
                      these connections never expire. Defaults to 30 minutes.

  (see also: http://is.gd/c3p0_pool_conf)

  Examples:

            (def my-spec
              {:classname \"com.mysql.jdbc.Driver\"
               :subprotocol \"mysql\"
               :subname \"//127.0.0.1:3306/mydb\"
               :user \"myaccount\"
               :password \"secret\"})

            (def my-pool
              (delay
                (db-pool my-spec :max-pool-size 30 :idle-time (* 6 60 60))))

            And then access `@my-pool` wherever you need access to the pool.

  (see also: http://is.gd/clj_jdbc_pool)
  "
  [spec & {:keys [min-pool-size max-pool-size idle-time excess-idle-time]
           :or {min-pool-size 3
                max-pool-size 15
                idle-time (* 3 60 60)
                excess-idle-time  (* 30 60)}}]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMinPoolSize min-pool-size)
               (.setInitialPoolSize min-pool-size)
               (.setMaxPoolSize max-pool-size)
               (.setMaxIdleTime idle-time)
               (.setMaxIdleTimeExcessConnections excess-idle-time))]
    {:datasource cpds}))
