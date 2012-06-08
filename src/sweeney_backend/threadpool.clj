(ns sweeney-backend.threadpool
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

(defn terminated?
  "Returns whether the thread `pool` has already terminated (shutdown and
  all tasks ended."
  [pool]
  (.isTerminated pool))

(defn active-count
  "Returns the approximate number of threads
  that are actively executing tasks."
  [pool]
  (.getActiveCount pool))

(defn completed-count
  "Returns the approximate total number of tasks
  that have completed execution."
  [pool]
  (.getCompletedTaskCount pool))

(defn total-count
  "Returns the approximate total number of tasks
  that have ever been submitted for execution."
  [pool]
  (.getTaskCount pool))

(defn queued-count
  "Returns the number of tasks that have been submitted and
  are waiting for execution."
  [pool]
  (.size (.getQueue pool)))

(defn current-size
  "Returns the current number of threads in the pool."
  [pool]
  (.getPoolSize pool))

(defn core-size
  "Returns the core number of threads, which are the threads that aren't
  terminated even when they are idle."
  [pool]
  (.getCorePoolSize pool))

(defn largest-size
  "Returns the largest number of threads that
  have ever simultaneously been in the pool."
  [pool]
  (.getLargestPoolSize pool))

(defn max-size
  "Returns the maximum allowed number of threads."
  [pool]
  (.getMaximumPoolSize pool))

(defn keepalive-time
  "Returns the thread keep-alive time, which is the amount of time that
  threads in excess of the core pool size may remain idle before being
  terminated."
  [pool]
  (.getKeepAliveTime pool))

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
          (catch TimeoutException e timeout-val)))
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
