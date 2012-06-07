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

(defn init-actions
  "Returns an empty map of actions that will be executed in the specified
  thread `pool`."
  [pool]
  (atom {:actions {} :last-id 0 :threadpool pool}))

(defn add-action
  "Registers function `f` as an action. The action will be invoked when
  an event that satisfies the `event-pred` will be fired. If the `max-count`
  parameter is specified, the action will be removed from actions after it
  was invoked exactly `max-count` times.

  Returns the id assigned to the action."
  ([actions event-pred f]
    (add-action actions event-pred f nil))
  ([actions event-pred f max-count]
    (let [action {:event-pred event-pred
                  :fn f
                  :max-count max-count
                  :count 0}]
      (:last-id (swap! actions #(let [id (inc (:last-id %))]
                                    (-> % (assoc-in [:actions id] action)
                                    (assoc-in [:last-id] id))))))))

(defn remove-action
  "Deletes action with specified `id` from `actions` and returns it. If action
  with specified `id` doesn't exist, returns nil."
  [actions id]
  (let [removed (get-in @actions [:actions id])]
    (swap! actions dissoc-in [:actions id])
    removed))
