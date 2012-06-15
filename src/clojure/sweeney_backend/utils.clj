(ns sweeney-backend.utils
  (:import [java.sql Date]))

(defn now
  "Returns the current time in milliseconds, measured from 1.1.1970 0:00 UTC."
  []
  (System/currentTimeMillis))

(defn to-sql-date
  "Converts java.util.Date to java.sql.Date."
  [date]
  (Date. (.getTime date)))
