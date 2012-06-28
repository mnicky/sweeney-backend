(ns sweeney-backend.actions
  (:require [overtone.at-at :as at]
            [clojure.java.jdbc :as jdbc]
            [sweeney-backend.config :as config]
            [sweeney-backend.events :as events]
            [sweeney-backend.feeds :as feeds]))

;remake of sweeney-backend.events functions that work on the config/event-pool

(defn add-action
  "The same as events/add-action but works on the config/event-pool."
  ([event-pred f]
    (events/add-action config/event-pool event-pred f))
  ([event-pred f desc]
    (events/add-action config/event-pool event-pred f desc)))

(defn remove-action
  "The same as events/remove-action but works on the config/event-pool."
  [id]
  (events/remove-action config/event-pool id))

(defn remove-action-by-desc
  "The same as events/remove-action-by-desc but works on the config/event-pool."
  [desc]
  (events/remove-action-by-desc config/event-pool desc))

(defn fire
  "The same as events/fire but works on the config/event-pool."
  [event-id event-data]
  (events/fire config/event-pool event-id event-data))


;domain specific functions and actions

(defn check-feed
  "Performs check of the feed with the specified `url`. This check will:
    - parse the feed
    - add this feed to the database if not present
    - add all stories of this feed to the database if not present

  Returns the map, containing these keys:
    :feed_id        - a feed-id of the checked feed
    :checked-before - a boolean value, specifying whether the feed has been
                      already checked before
  "
  [db url]
  (let [{:keys [info stories]} (feeds/parse-feed url)
        feed-id-known (:id (feeds/find-feed-by-url db url))
        feed-id (or feed-id-known
                    (let [ret-val (feeds/add-feed db info)]
                      (or (:id ret-val) ((keyword "last_insert_rowid()") ret-val))))]
    (doseq [story stories]
      (when-not (feeds/find-story-by-url db (:url story))
        (feeds/add-story db story feed-id)))
    {:feed-id feed-id :checked-before (boolean feed-id-known)}))

(defn avg-story-period
  "Returns average period of publishing new stories for the feed with given
  `feed-id` in milliseconds, using the `last-n` stories from the database
  with specified `db` connection."
  [db feed-id last-n]
  {:pre [(not (zero? last-n))]}
  (let [times (jdbc/with-connection db
                (jdbc/with-query-results res
                  ["SELECT published_at FROM stories WHERE feed_id=? ORDER BY published_at DESC LIMIT ?" feed-id (inc last-n)]
                  (mapcat vals (vec res))))
        millis (if (number? (first times)) times (map #(.getTime %) times))] ;convert if not in milliseconds
    (/ (reduce + (map - millis (rest millis))) last-n)))

(defn check-feed-action
  "Checks the feed with given `url` and schedules the next check according
  to the average publishing period for that feed. Returns the feed_id
  of checked feed.

  Should be used as an action with `sweeney-backend.events` framework and
  not called directly.

  This function uses these configuration options:
    - config/db-pool
    - config/event-pool
    - config/scheduled-pool
    - config/last-n-stories
    - config/min-period
  "
  [event url]
  (let [{:keys [feed-id checked-before]} (check-feed @config/db-pool url)]
    (when-not checked-before
      (let [avg-period (avg-story-period @config/db-pool feed-id config/last-n-stories)
            period (if (< avg-period config/min-period) config/min-period avg-period)]
        (at/after period #(events/fire config/event-pool event url) config/scheduled-pool :desc (str "fire the check of " url))))
    feed-id))
