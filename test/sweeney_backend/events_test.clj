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
  (let [actions (init-action-pack (mk-pool :single))]
    (is (= 0 (:last-id @actions)))

    (is (= 1 (add-action actions true (fn [x y] "example-action"))))
    (is (contains? (:actions @actions) 1))
    (is (= 1 (:last-id @actions)))

    (let [newly-added ((:actions @actions) 1)]
      (is (= true (:event-pred newly-added))))

    (is (= 2 (add-action actions false (fn [x y] "example-action"))))
    (is (contains? (:actions @actions) 2))
    (is (= 2 (:last-id @actions)))))

(deftest remove-action-test
  (let [actions (init-action-pack (mk-pool :single))]
    (add-action actions true (fn [x y] "example-action"))
    (is (contains? (:actions @actions) 1))

    (is (= (get-in @actions [:actions 1 :fun]) (:fun (remove-action actions 1))))
    (is (= false (contains? (:actions @actions) 1)))
    (is (nil? (:fun (remove-action actions 2))))))

(deftest fire-test
  (let [actions (init-action-pack (mk-pool))]
    (add-action actions #{:event1} (fn [x y] "value1"))
    (add-action actions #{:event2} (fn [x y] "value2"))
    (add-action actions #{:event2} (fn [x y] "value3"))

    (is (= {} (fire actions :unknown-event "no-data")))
    (is (= "value1" @((fire actions :event1 "no-data") 1)))

    (let [res (fire actions :event2 "no-data")]
      (is (= #{2 3} (set (keys res))))
      (is (= #{"value2" "value3"} (set (map deref (vals res))))))))
