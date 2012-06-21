(ns sweeney-backend.events-test
  (:require [sweeney-backend.threadpool :as threadpool])
  (:use clojure.test
        sweeney-backend.events))

(deftest add-action-test
  (let [actions (init-action-pack (threadpool/t-pool))]
    (is (= 0 (:last-id @actions)))

    (is (= 1 (add-action actions identity (fn [x y] "example-action") "description")))
    (is (contains? (:actions @actions) 1))
    (is (= 1 (:last-id @actions)))

    (let [newly-added ((:actions @actions) 1)]
      (is (= identity (:event-pred newly-added)))
      (is (= "description" (:desc newly-added))))

    (is (= 2 (add-action actions identity (fn [x y] "example-action"))))
    (is (contains? (:actions @actions) 2))
    (is (= 2 (:last-id @actions)))))

(deftest remove-action-test
  (let [actions (init-action-pack (threadpool/t-pool))]
    (add-action actions identity (fn [x y] "example-action"))
    (is (contains? (:actions @actions) 1))

    (is (= (get-in @actions [:actions 1 :fun]) (:fun (remove-action actions 1))))
    (is (= false (contains? (:actions @actions) 1)))
    (is (nil? (:fun (remove-action actions 2))))))

(deftest fire-test
  (let [actions (init-action-pack (threadpool/t-pool))]
    (add-action actions #{:event1} (fn [x y] "value1"))
    (add-action actions #{:event2} (fn [x y] "value2"))
    (add-action actions #{:event2} (fn [x y] "value3"))

    (is (= {} (fire actions :unknown-event "no-data")))
    (is (= "value1" @((fire actions :event1 "no-data") 1)))

    (let [res (fire actions :event2 "no-data")]
      (is (= #{2 3} (set (keys res))))
      (is (= #{"value2" "value3"} (set (map deref (vals res))))))))
