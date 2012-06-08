; Heavily inspired by dispatch example from ClojureScript One, available at:
; https://github.com/brentonashworth/one/blob/master/src/lib/cljs/one/dispatch.cljs
(ns sweeney-backend.events
  (:require [sweeney-backend.threadpool :as t])
  (:use [clojure.core.incubator :only [dissoc-in]]))

(defrecord ActionPack [actions last-id threadpool])
(defrecord Action [event-pred fun desc])

(defn init-action-pack
  "Returns an empty pack of actions that will be executed in the specified
  thread `pool`."
  [pool]
  (atom (ActionPack. {} 0 pool)))

(defn add-action
  "Registers function `f` as an action to the specified `action-pack`.
  The action will be invoked when an event that satisfies the `event-pred`
  will be fired.

  `event-pred` is a function of `event-id`.
  `f` is a function of `event-id` and `event-data`.
  `desc` is an optional string containing the description of the action.

  Returns the id assigned to the newly added action."
  ([action-pack event-pred f]
    (add-action action-pack event-pred f ""))
  ([action-pack event-pred f desc]
    (let [action (Action. event-pred f desc)]
      (:last-id (swap! action-pack #(let [id (inc (:last-id %))]
                                        (-> % (assoc-in [:actions id] action)
                                        (assoc-in [:last-id] id))))))))

(defn remove-action
  "Deletes action with specified `id` from `action-pack` and returns it.
  If action with specified `id` doesn't exist, returns nil."
  [action-pack id]
  (let [removed (get-in @action-pack [:actions id])]
    (swap! action-pack dissoc-in [:actions id])
    removed))

(defn fire
  "Raise an event with specified `event-id` and `event-data`. Actions
  registered in `action-pack` whose `event-pred` returns true for specified
  `event-id` will be submitted to the thread pool of the `action-pack`
  for execution.

  Returns map with ids of submitted actions as keys and futures representing
  results of their execution as values. If no actions were submitted, returns
  an empty map."
  [action-pack event-id event-data]
  (let [action-pack @action-pack
        matches (filter (fn [[action-id {event-pred :event-pred}]]
                           (event-pred event-id))
                         (:actions action-pack))
        pool (:threadpool action-pack)]
    (into {}
      (for [action matches]
        (let [[action-id {fun :fun}] action]
          [action-id (t/submit pool #(fun event-id event-data))])))))
