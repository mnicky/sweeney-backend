(ns sweeney-backend.actions
  (:require [overtone.at-at :as at]
            [sweeney-backend.events :as events]
            [sweeney-backend.feeds :as feeds])
  (:import [sweeney_backend.feeds Feed Story]))

(defn check-feed
  "Performs check of the feed with the specified `url`. This check will:
    - parse the feed
    - add this feed to the database if not present
    - add all stories of this feed to the database if not present"
  [db url]
  (let [{:keys [info stories]} (feeds/parse-feed url)
        feed-id (or (:id (feeds/find-feed-by-url db url))
                    ((keyword "last_insert_rowid()") (feeds/add-feed db info)))]
    (doseq [story stories]
      (when-not (feeds/find-story-by-url db (:url story))
        (feeds/add-story db story feed-id)))))
