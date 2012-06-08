; Inspired by dispatch example from ClojureScript One, available at:
; https://github.com/brentonashworth/one/blob/master/src/lib/cljs/one/dispatch.cljs
; Thanks!

(ns sweeney-backend.events
  (:use [clojure.core.incubator :only (dissoc-in)])
  (:import [java.util.concurrent Callable Executors TimeUnit TimeoutException]))

(defn cpu-count
  "Returns the number of CPUs on this machine."
  []
  (.availableProcessors (Runtime/getRuntime)))

(defn mk-pool
  "Creates and return a new thread pool. The `type` can be:

  :fixed  - ThreadPool with fixed number of Threads, as specified in `option`
            (defaults to number of available CPUs + 2). This thread pool
            has unbounded task queue.

  :cached - ThreadPool with unbounded maximal number of threads. The threads
            will be terminated if they have been idle for more than the
            keep-alive time in milliseconds, as specified in `option`
            (defaults to 15000ms).

  :single - ThreadPool with only single thread and unbounded task queue."
  ([]
    (mk-pool :fixed))
  ([type]
    (mk-pool type nil))
  ([type option]
    (case type
      :fixed (Executors/newFixedThreadPool (or option (+ (cpu-count) 2)))
      :cached (let [pool (Executors/newCachedThreadPool)]
                 (.setKeepAliveTime pool (or option 15000) TimeUnit/MILLISECONDS)
                 pool)
      :single (Executors/newSingleThreadExecutor)
      (throw (RuntimeException. (str "Unsupported pool type: " type "."))))))

(defn shutdown!
  "Executes all previously submitted tasks, shutdowns the thread `pool` and
  returns it."
  [pool]
  (.shutdown pool)
  pool)

(defn shutdown-now!
  "Shutdowns the thread `pool` immediately, stopping all executing tasks and
  returns it."
  [pool]
  (.shutdownNow pool)
  pool)

(defn shutdown?
  "Returns whether the thread `pool` has been already shut down."
  [pool]
  (.isShutdown pool))

(defn to-future
  "Takes a java.util.concurrent.Future and returns a Clojure future made
  from it."
  {:static true}
  [fut]
  (reify
    clojure.lang.IDeref
    (deref [_] (.get fut))
    clojure.lang.IBlockingDeref
    (deref
     [_ timeout-ms timeout-val]
     (try (.get fut timeout-ms TimeUnit/MILLISECONDS)
          (catch TimeoutException e
            timeout-val)))
    clojure.lang.IPending
    (isRealized [_] (.isDone fut))
    java.util.concurrent.Future
      (get [_] (.get fut))
      (get [_ timeout unit] (.get fut timeout unit))
      (isCancelled [_] (.isCancelled fut))
      (isDone [_] (.isDone fut))
      (cancel [_ interrupt?] (.cancel fut interrupt?))))

(defn to-callable
  "Takes a function of zero arguments and returns
  a java.util.concurrent.Callable made from it."
  {:static true}
  [f]
  (reify
    Callable
    (call [_] (f))))

(defn submit
  "Submits function `f` for execution by thread `pool` and returns a future
  representing the value returned by the function."
  [pool f]
  (to-future
    (.submit pool (to-callable f))))

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
          [action-id (submit pool #(fun event-id event-data))])))))
