(ns sweeney-backend.utils
  (:require [overtone.at-at :as at])
  (:import [java.sql Timestamp]
           [sweeney_backend.utils Utils]))

(defn now
  "Returns the current time in milliseconds, measured from 1.1.1970 0:00 UTC."
  []
  (System/currentTimeMillis))

(defn to-timestamp
  "Converts java.util.Date to java.sql.Timestamp."
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

(def fn-desc-separator " --- fn: ")

(defn ^{:doc (str "Schedules fun to be executed after delay-ms (in milliseconds). The same
  as overtone.at-at/after, but also adds the string \"" fn-desc-separator "FUN\" at the
  end of the supplied description, where the FUN is printed form of the fun.
  This, together with the use of serializable-fn/fn as fun, allows
  for serialization.")}
  after
  [delay-ms fun pool & {:keys [desc], :or {desc ""}}]
  (at/after delay-ms fun pool :desc (str desc fn-desc-separator (pr-str fun))))

(defn ^{:doc (str "Schedules fun to be executed at ms-time (in milliseconds). The same
  as overtone.at-at/at, but also adds the string \"" fn-desc-separator "FUN\" at the
  end of the supplied description, where the FUN is printed form of the fun.
  This, together with the use of serializable-fn/fn as fun, allows
  for serialization.")}
  at
  [ms-time fun pool & {:keys [desc], :or {desc ""}}]
  (at/at ms-time fun pool :desc (str desc fn-desc-separator (pr-str fun))))

(defn ^{:doc (str "Calls fun every ms-period, and takes an optional initial-delay for
  the first call in ms.  Returns a scheduled-fn which may be cancelled
  with cancel. The same as overtone.at-at/every, but also adds the string
  \"" fn-desc-separator "FUN\" at the end of the supplied description, where the FUN
  is printed form of the fun.
  This, together with the use of serializable-fn/fn as fun, allows
  for serialization.")}
  every
  [ms-period fun pool & {:keys [initial-delay desc], :or {initial-delay 0 desc ""}}]
  (at/every ms-period fun pool :initial-delay initial-delay :desc (str desc fn-desc-separator (pr-str fun))))

;taken from clojure.contrib.with-ns
(defmacro with-ns
  "Evaluates body in another namespace.  ns is either a namespace
  object or a symbol.  This makes it possible to define functions in
  namespaces other than the current one."
  [ns & body]
  `(binding [*ns* (the-ns ~ns)]
     ~@(map (fn [form] `(eval '~form)) body)))
