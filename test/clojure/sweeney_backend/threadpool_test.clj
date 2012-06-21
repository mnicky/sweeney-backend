(ns sweeney-backend.threadpool-test
  (:use clojure.test
        sweeney-backend.threadpool))

(deftest t-pool-test
  (let [fixed-pool (t-pool :fixed :size 5)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor fixed-pool))
    (is (= 5 (min-size fixed-pool)))
    (is (= 5 (max-size fixed-pool))))

  (let [cached-pool (t-pool :cached :keepalive 20000)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor cached-pool))
    (is (= 0 (min-size cached-pool)))
    (is (= 20000 (keepalive-time cached-pool))))

  (let [variable-pool (t-pool :variable :size 3)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor variable-pool))
    (is (= 3 (min-size variable-pool)))
    (is (= 3 (max-size variable-pool))))

  (is (thrown? RuntimeException (t-pool :undefined))))

(deftest shutdown-test
  (is (= false (shutdown? (t-pool))))
  (is (= true (shutdown? (shutdown! (t-pool)))))
  (is (= true (shutdown? (shutdown-now! (t-pool))))))

(deftest classes-reification-test
  (is (future? (to-future (fn [] 1))))
  (is (instance? java.util.concurrent.Callable (to-callable (fn [] 2)))))

(deftest submit-test
  (is (future? (submit (t-pool) (fn [] 1))))
  (is (instance? java.util.concurrent.Future (submit (t-pool) (fn [] 2))))
  (is (= 3 (.get (submit (t-pool) (fn [] 3)))))
  (is (= 4 @(submit (t-pool) (fn [] 4)))))
