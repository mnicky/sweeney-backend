(ns sweeney-backend.threadpool
  (:import [java.util.concurrent Callable Executors ThreadPoolExecutor TimeUnit TimeoutException]
            [sweeney_backend.threadpool ConfigurableThreadFactory]))

(defn cpu-count
  "Returns the number of CPUs on this machine."
  []
  (.availableProcessors (Runtime/getRuntime)))

(defn mk-pool
  "Creates and return a new thread pool. The `type` can be:

  :fixed  - Thread pool with fixed number of threads and unbounded task queue.
            This type accepts optional parameter :size - the number of
            threads (defaults to number of available CPUs + 2).

  :cached - Thread pool with unbounded maximal number of threads. The threads
            will be terminated if they have been idle for more than their
            keep-alive time.
            This type accepts optional parameter :keepalive - the keep-alive
            time in milliseconds (defaults to 15000ms). A value of zero will
            cause threads to terminate immediately after executing tasks.

  :own    - Thread pool with unbounded task queue which accepts these optional
            parameters:

              :size - the number of threads (defaults to number of available
                      CPUs + 2)

              :max  - maximal allowed number of threads (defaults to number
                      of available CPUs + 2, or `size` if it is greater)

              :keepalive - keep-alive time of threads (defaults to 15000ms).
                           Only threads above the `size` count will be
                           terminated after the keep-alive time.

  All thread pool types have also these optional parameters:

            :daemon - Specifies whether the threads in this thread pool
                      are daemon or not (default is false).
                      The JVM will shut down when all non-daemon threads
                      have terminated, so daemon threads are threads
                      whose existence has no impact on whether the JVM
                      continues to execute or shuts down.

            :prefix - Specifies the string prefix for the name of the
                      threads. This can be useful especially when debugging.

  If no `type` is specified, defaults to the :fixed type with default values
  for all available parameters.

  Examples:
            (mk-pool)

            (mk-pool :fixed)
            (mk-pool :fixed :daemon true :prefix \"event-manager\")
            (mk-pool :fixed :size 5)
            (mk-pool :fixed :size 8 :daemon true)

            (mk-pool :cached)
            (mk-pool :cached :prefix \"server\")
            (mk-pool :cached :keepalive 60000)

            (mk-pool :own :size 4 :max 8 :keepalive 30000 :daemon true)
  "
  ([]
    (mk-pool :fixed))
  ([type & {:keys [size max keepalive daemon prefix]
            :or    {size (+ (cpu-count) 2)
                    max  (+ (cpu-count) 2)
                    keepalive 15000
                    daemon false
                    prefix "sweeney-backend-threadpool"}}]
  {:pre [(< 0 size)]}
  (doto
    (case type
      :fixed (Executors/newFixedThreadPool size)
      :cached (doto (Executors/newCachedThreadPool)
                 (.setKeepAliveTime keepalive TimeUnit/MILLISECONDS))
      :own (let [max (if (<= size max) max size)]
              (doto (Executors/newFixedThreadPool size)
                (.setMaximumPoolSize max)
                (.setKeepAliveTime keepalive TimeUnit/MILLISECONDS)))
      (throw (RuntimeException. (str "Unsupported thread pool type: '" type "'."))))
    (.setThreadFactory (ConfigurableThreadFactory. prefix daemon)))))

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
  also all tasks ended."
  [pool]
  (.isTerminated pool))

(defn submitted-tasks
  "Returns the approximate total number of tasks
  that have ever been submitted for execution."
  [pool]
  (.getTaskCount pool))

(defn completed-tasks
  "Returns the approximate total number of tasks
  that have completed execution."
  [pool]
  (.getCompletedTaskCount pool))

(defn active-tasks
  "Returns the approximate number of threads
  that are actively executing tasks."
  [pool]
  (.getActiveCount pool))

(defn queued-tasks
  "Returns the number of tasks that have been submitted and
  are waiting for execution."
  [pool]
  (.size (.getQueue pool)))

(defn min-size
  "Returns the core number of threads, which are the threads that aren't
  terminated even when they are idle."
  [pool]
  (.getCorePoolSize pool))

(defn max-size
  "Returns the maximum allowed number of threads."
  [pool]
  (.getMaximumPoolSize pool))

(defn current-size
  "Returns the current number of threads in the pool."
  [pool]
  (.getPoolSize pool))

(defn peak-size
  "Returns the largest number of threads that
  have ever simultaneously been in the pool."
  [pool]
  (.getLargestPoolSize pool))

(defn keepalive-time
  "Returns the thread keep-alive time, which is the amount of time that
  threads in excess of the core pool size may remain idle before being
  terminated."
  [pool]
  (.getKeepAliveTime pool TimeUnit/MILLISECONDS))

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

(defmethod print-method ThreadPoolExecutor
  [p w]
  (.write w (str "#<ThreadPoolExecutor: "

                 (if (shutdown? p) "SHUTDOWN - " "")
                 (if (terminated? p) "and TERMINATED - " "")

                 "Tasks: "
                 (submitted-tasks p) " submitted, "
                 (completed-tasks p) " completed, "
                 (active-tasks p) " active, "
                 (queued-tasks p) " queued; "

                 "Threads: "
                 (min-size p) " min, "
                 (max-size p) " max, "
                 (current-size p) " current, "
                 (peak-size p) " peak; "

                 "Keep-alive time: "
                 (keepalive-time p) "ms"
                 ">"
            )))
