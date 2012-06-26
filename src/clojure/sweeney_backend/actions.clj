(ns sweeney-backend.actions
  (:require [overtone.at-at :as at]
            [clojure.java.jdbc :as jdbc]
            [sweeney-backend.events :as events]
            [sweeney-backend.feeds :as feeds])
  (:import [sweeney_backend.feeds Feed Story]))

(defn check-feed
  "Performs check of the feed with the specified `url`. This check will:
    - parse the feed
    - add this feed to the database if not present
    - add all stories of this feed to the database if not present

  Returns the `feed_id` of the checked feed."
  [db url]
  (let [{:keys [info stories]} (feeds/parse-feed url)
        feed-id (or (:id (feeds/find-feed-by-url db url))
                    ((keyword "last_insert_rowid()") (feeds/add-feed db info)))]
    (doseq [story stories]
      (when-not (feeds/find-story-by-url db (:url story))
        (feeds/add-story db story feed-id)))
    feed-id))

(defn avg-story-period
  "Returns average period of publishing new stories for the feed with given
  `feed-id` in milliseconds, using the `last-n` stories from the database
  with specified `db` connection."
  [db feed-id last-n]
  {:pre [(not (zero? last-n))]}
  (let [times (jdbc/with-connection db
                (jdbc/with-query-results res
                  ["SELECT published_at FROM stories WHERE feed_id=? ORDER BY published_at DESC LIMIT ?" feed-id (inc last-n)]
                  (mapcat vals (vec res))))]
    (/ (reduce + (map - times (rest times))) last-n)))
