(ns sweeney-backend.threadpool-test
  (:use clojure.test
        sweeney-backend.threadpool))

(deftest mk-pool-test
  (let [fixed-pool (mk-pool :fixed :size 5)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor fixed-pool))
    (is (= 5 (min-size fixed-pool)))
    (is (= 5 (max-size fixed-pool))))

  (let [cached-pool (mk-pool :cached :keepalive 20000)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor cached-pool))
    (is (= 0 (min-size cached-pool)))
    (is (= 20000 (keepalive-time cached-pool))))

  (let [variable-pool (mk-pool :variable :size 3)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor variable-pool))
    (is (= 3 (min-size variable-pool)))
    (is (= 3 (max-size variable-pool))))

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
