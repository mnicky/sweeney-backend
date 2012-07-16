(ns sweeney-backend.utils
  (:import [java.sql Timestamp]
           [sweeney_backend.utils Utils]))

(defn now
  "Returns the current time in milliseconds, measured from 1.1.1970 0:00 UTC."
  []
  (System/currentTimeMillis))

(defn to-timestamp
  "Converts java.util.Date to java.sql.Date."
  [date]
  (Timestamp. (.getTime date)))

(defn extract-text
  "Extracts plain text from HTML string."
  [html]
  (Utils/extractText html))

(defn on-shutdown
  "Registers function (of zero arguments) as a shutdown hook
  of current Runtime and returns nil. See also: http://is.gd/shutdown_hook"
  [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))
