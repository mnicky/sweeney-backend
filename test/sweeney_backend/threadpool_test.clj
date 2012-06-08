(ns sweeney-backend.threadpool-test
  (:use clojure.test
        sweeney-backend.threadpool)
  (:import [java.util.concurrent TimeUnit]))

(deftest mk-pool-test
  (let [fixed-pool (mk-pool :fixed :size 5)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor fixed-pool))
    (is (= 5 (.getCorePoolSize fixed-pool)))

  (let [cached-pool (mk-pool :cached :keepalive 20000)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor cached-pool))
    (is (= 20000 (.getKeepAliveTime cached-pool TimeUnit/MILLISECONDS)))))

  (is (thrown? RuntimeException (mk-pool :undefined))))

(deftest shutdown-test
  (is (= false (shutdown? (mk-pool))))
  (is (= true (shutdown? (shutdown! (mk-pool)))))
  (is (= true (shutdown? (shutdown-now! (mk-pool))))))

(deftest classes-reification-test
  (is (future? (to-future (fn [] 1))))
  (is (instance? java.util.concurrent.Callable (to-callable (fn [] 2)))))

(deftest submit-test
  (is (future? (submit (mk-pool) (fn [] 1))))
  (is (instance? java.util.concurrent.Future (submit (mk-pool) (fn [] 2))))
  (is (= 3 (.get (submit (mk-pool) (fn [] 3)))))
  (is (= 4 @(submit (mk-pool) (fn [] 4)))))
