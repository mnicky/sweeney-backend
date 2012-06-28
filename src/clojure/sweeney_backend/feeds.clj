(ns sweeney-backend.feeds
  (:require [sweeney-backend.utils :as utils]
            [feedparser-clj.core :as parser]
            [clojure.java.jdbc :as jdbc]))

(defrecord Story [feed_type title url description published_at])
(defrecord Feed [url title link image])

(defn add-story
  "Adds the `story` to the database with specified `feed-id` using `db-connection`."
  [db-connection story feed-id]
    (jdbc/with-connection db-connection
      (jdbc/transaction
        (jdbc/insert-record :stories (assoc story :feed_id feed-id)))))

(defn add-feed
  "Adds the `story` to the database with specified `feed-id` using `db-connection`."
  [db-connection feed]
    (jdbc/with-connection db-connection
      (jdbc/transaction
        (jdbc/insert-record :rss_feeds feed))))

(defn parse-feed
  "Parses the feed at the given `url` and returns map with this format:
    :info - information about the feed as instance of Feed
    :stories - lazy seq of feed's entries as instances of Story"
  [url]
  {:pre [(or (string? url) (instance? java.io.File url))]}
  (let [feed (parser/parse-feed url)
        {:keys [entries image link published-date title uri]} feed
        info (Feed/create {:url url
                           :title title
                           :link (or link uri)
                           :image (:url image)})]
    {:stories (for [e entries]
                (let [feed-published (or published-date (utils/now))
                      {:keys [title link description contents published-date updated-date]} e]
                  (Story/create {:feed_type "RssFeed"
                                 :title title
                                 :url link
                                 :description (or (:value (first contents)) (:value description) "")
                                 :published_at (utils/to-timestamp (or updated-date published-date feed-published))})))
     :info info}))

(defn find-feed-by-url
  "Returns map of columns of `rss_feeds` table with given `url` using given
  `db-connection`. If no rss feed with given `url` exists, returns nil."
  [db-connection url]
  (jdbc/with-connection db-connection
    (jdbc/with-query-results res
      ["SELECT id, url, title, link, image FROM rss_feeds WHERE url = ? LIMIT 1" url]
      (first res))))

(defn find-story-by-url
  "Returns map of columns of `stories` table with given `url` using given
  `db-connection`. If no story with given `url` exists, returns nil."
  [db-connection url]
  (jdbc/with-connection db-connection
    (jdbc/with-query-results res
      ["SELECT id, feed_id, feed_type, title, url, description, published_at FROM stories WHERE url = ? LIMIT 1" url]
      (first res))))
