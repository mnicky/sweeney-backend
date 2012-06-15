(ns sweeney-backend.utils-test
  (:use clojure.test
        sweeney-backend.utils))

(deftest now-test
  (is (instance? java.lang.Long (now)))
  (let [date-hh-mm #(re-find #"[\w]+ [\w]+ [\d]+ [\d]+:[\d]+" (str %))]
    (is (= (date-hh-mm (java.util.Date.)) (date-hh-mm (java.util.Date. (now)))))))

(deftest to-sql-date-test
  (is (instance? java.sql.Date (to-sql-date (java.util.Date.)))))
