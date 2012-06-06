(ns sweeney-backend.events-test
  (:use clojure.test
        sweeney-backend.events)
  (:import [java.util.concurrent TimeUnit]))

(deftest mk-pool-test
  (let [fixed-pool (mk-pool :fixed 5)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor fixed-pool))
    (is (= 5 (.getCorePoolSize fixed-pool)))

  (let [cached-pool (mk-pool :cached 20000)]
    (is (instance? java.util.concurrent.ThreadPoolExecutor cached-pool))
    (is (= 20000 (.getKeepAliveTime cached-pool TimeUnit/MILLISECONDS)))))

  (let [single-pool (mk-pool :single)]
    (cast java.util.concurrent.ExecutorService single-pool))
  (is (thrown? RuntimeException (mk-pool :undefined))))

(deftest shutdown-test
  (is (= false (shutdown? (mk-pool :single))))
  (is (= true (shutdown? (shutdown! (mk-pool :single)))))
  (is (= true (shutdown? (shutdown-now! (mk-pool :single))))))

(deftest classes-reification-test
  (is (future? (to-future (fn [] 1))))
  (is (instance? java.util.concurrent.Callable (to-callable (fn [] 2)))))

(deftest submit-test
  (is (future? (submit (mk-pool :single) (fn [] 1))))
  (is (instance? java.util.concurrent.Future (submit (mk-pool :single) (fn [] 2))))
  (is (= 3 (.get (submit (mk-pool :single) (fn [] 3)))))
  (is (= 4 @(submit (mk-pool :single) (fn [] 4)))))

(deftest add-action-test
  (let [actions (init-actions (mk-pool :single))]
    (is (= 0 (:last-id @actions)))

    (add-action actions true (fn [] "empty") 10)
    (is (contains? (:actions @actions) 1))
    (is (= 1 (:last-id @actions)))

    (let [newly-added ((:actions @actions) 1)]
      (is (= true (:event-pred newly-added)))
      (is (= 10 (:max-count newly-added)))
      (is (= 0 (:count newly-added))))

    (add-action actions false (fn [] "empty") 20)
    (is (contains? (:actions @actions) 2))
    (is (= 2 (:last-id @actions)))))

(deftest remove-action-test
  (let [actions (init-actions (mk-pool :single))]
    (add-action actions true (fn [] "empty") 10)
    (is (contains? (:actions @actions) 1))
    (remove-action actions 1)
    (is (= false (contains? (:actions @actions) 1)))))
