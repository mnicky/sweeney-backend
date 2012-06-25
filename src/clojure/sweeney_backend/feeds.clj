(ns sweeney-backend.feeds
  (:require [sweeney-backend.utils :as utils])
  (:require [feedparser-clj.core :as parser])
  (:require [clojure.java.jdbc :as jdbc]))

(defrecord Story [feed_type title url description published_at])

(defn save-story
  "Adds the `story` to the database with specified `feed-id` using `db-connection`."
  [db-connection story feed-id]
    (jdbc/with-connection
      db-connection
      (jdbc/transaction
        (jdbc/insert-record :stories (assoc story :feed_id feed-id)))))

(defn parse-feed
  "Parses the feed at the given `url` and returns lazy seq of its entries
  as instances of Story."
  [url]
  (let [feed (parser/parse-feed url)
        {:keys [entries published-date]} feed]
    (for [e entries]
      (let [feed-published (or published-date (utils/now))
            {:keys [title link description contents published-date updated-date]} e]
        (Story/create {:feed_type "RssFeed"
                       :title title
                       :url link
                       :description (or (:value (first contents)) (:value description) "")
                       :published_at (utils/to-sql-date (or updated-date published-date feed-published))})))))

(defn find-feed-by-url
  "Returns map of columns of rss_feed with given `url` using given `db-connection`"
  [db-connection url]
  (jdbc/with-connection db-connection
    (jdbc/with-query-results res
      [(str "SELECT id, url, title, link, image "
            "FROM rss_feeds "
            "WHERE url=? "
            "LIMIT 1")
       url]
      (first res))))
