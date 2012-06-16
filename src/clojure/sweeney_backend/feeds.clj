(ns sweeney-backend.feeds
  (:require [sweeney-backend.utils :as utils])
  (:require [feedparser-clj.core :as parser])
  (:require [korma.core :refer :all]))

(defrecord Story [feed-type title url description published-at])

(defn save-story
  "Add the story to the database with specified feed-id."
  [story feed-id]
  (let [{:keys [feed-type title url description published-at]} story]
    (insert :stories (values {:feed_id feed-id
                              :feed_type feed-type
                              :title title
                              :url url
                              :description description
                              :published_at published-at}))))

(defn parse-feed
  "Parses the feed at the given url and returns lazy seq of its entries."
  [url]
  (let [feed (parser/parse-feed url)
        {:keys [entries published-date]} feed]
    (for [e entries]
      (let [feed-published (or published-date (utils/now))
            {:keys [title link description contents published-date updated-date]} e]
        (Story/create {:feed-type "RssFeed"
                       :title title
                       :url link
                       :description (or (:value (first contents)) (:value description) "")
                       :published-at (utils/to-sql-date (or updated-date published-date feed-published))})))))
