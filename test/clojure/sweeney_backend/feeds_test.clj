(ns sweeney-backend.feeds-test
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc])
  (:use clojure.test
        sweeney-backend.feeds
        sweeney-backend.dbpool)
  (:import [sweeney_backend.feeds Feed Story]))

(def rss-file "./test/resources/test-rss1.xml")

(def test-db {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname "test/resources/test-db"
              :user "test"
              :password "test"})

(def test-db-pool (delay (db-pool test-db)))

(defn drop-test-tables
  [db-conn]
  (jdbc/with-connection db-conn
      (jdbc/drop-table :rss_feeds)
      (jdbc/drop-table :stories)))

(defn create-test-tables
  [db-conn]
  (jdbc/with-connection db-conn
    (jdbc/transaction
      (jdbc/create-table :rss_feeds
                         [:id "INTEGER PRIMARY KEY AUTOINCREMENT"]
                         [:url :text]
                         [:title :text]
                         [:link :text]
                         [:image :text])
      (jdbc/create-table :stories
                         [:id "INTEGER PRIMARY KEY AUTOINCREMENT"]
                         [:feed_id :integer]
                         [:feed_type :text]
                         [:title :text]
                         [:url :text]
                         [:description :text]
                         [:published_at :date]))))

(use-fixtures :once
              (fn [x]
                (create-test-tables @test-db-pool)
                (try
                  (x)
                  (finally
                    (drop-test-tables @test-db-pool)))))

(deftest parse-feed-stories-test
  (let [feed (io/file rss-file)]
    (is (instance? java.io.File feed))
    (is (.exists feed))

    (let [stories (:stories (parse-feed feed))
          article (first stories)]
      (is (= 25 (count stories)))
      (is (= (:feed_type article) "RssFeed"))
      (is (= (:title article) "Opera zvyšuje stabilitu, oddělila pluginy do vlastních procesů"))
      (is (= (:url article) "http://www.root.cz/zpravicky/opera-zvysuje-stabilitu-oddelila-pluginy-do-vlastnich-procesu/#utm_source=rss&utm_medium=text&utm_campaign=rss"))
      (is (= (:description article) "Vyšla nová verze internetového prohlížeče Opera 12. Kromě nové podpory snadné změny vzhledu je zde i množství technických věcí, které nejsou na první pohled vidět. Například rychlejší start i s množstvím panelů, oddělení pluginů do samostatných..."))
      (is (= (.getTime (:published_at article)) (.getTime #inst "2012-06-15T14:53:13.000-00:00"))))))

(deftest parse-feed-info-test
  (let [feed (io/file rss-file)]
    (is (instance? java.io.File feed))
    (is (.exists feed))

    (let [info (:info (parse-feed feed))]
      (is (= (:url info) feed))
      (is (= (:title info) "Root.cz - zprávičky"))
      (is (= (:link info) "http://www.root.cz/zpravicky/"))
      (is (= (:image info) "http://ii.iinfo.cz/r/rss-88x31.gif")))))

(deftest add-story-test
  (let [story (Story. "test feed type" "test story title" "http://example.com/story" "test story description" #inst "2012-01-01T12:00")]
    (add-story @test-db-pool story 12)
    (jdbc/with-connection @test-db-pool
      (jdbc/with-query-results res
        ["SELECT * FROM stories WHERE feed_id=? LIMIT 1" 12]
        (is (= (dissoc (first res) :id)
               {:feed_id 12
                :feed_type "test feed type"
                :title "test story title"
                :url "http://example.com/story"
                :description "test story description"
                :published_at (.getTime #inst "2012-01-01T12:00")} ))))))

(deftest add-feed-test
  (let [feed (Feed. "http://testfeed.com/feed.xml" "testfeed title" "http://testfeed.com" "http://testfeed.com/feed.png")]
    (add-feed @test-db-pool feed)
    (jdbc/with-connection @test-db-pool
      (jdbc/with-query-results res
        ["SELECT * FROM rss_feeds WHERE link=? LIMIT 1" "http://testfeed.com"]
        (is (= (dissoc (first res) :id)
               {:url "http://testfeed.com/feed.xml"
                :title "testfeed title"
                :link "http://testfeed.com"
                :image "http://testfeed.com/feed.png"} ))))))

(deftest find-feed-by-url-test
  (jdbc/with-connection @test-db-pool
    (jdbc/insert-record :rss_feeds (assoc (Feed. "http://example.com/feed.xml"
                                                 "example title"
                                                 "http://example.com"
                                                 "http://example.com/feed.png")
                                          :id 87))
    (is (= 87 (:id (find-feed-by-url @test-db-pool "http://example.com/feed.xml"))))
    (is (= nil (find-feed-by-url @test-db-pool "http://nonexistent.com/feed.xml")))))

(deftest find-feed-by-id-test
  (jdbc/with-connection @test-db-pool
    (jdbc/insert-record :rss_feeds (assoc (Feed. "http://example2.com/feed2.xml"
                                                 "example2 title"
                                                 "http://example2.com"
                                                 "http://example2.com/feed2.png")
                                          :id 92))
    (is (= 92 (:id (find-feed-by-id @test-db-pool 92))))
    (is (= nil (find-feed-by-url @test-db-pool 999)))))

(deftest find-story-by-url-test
  (jdbc/with-connection @test-db-pool
    (jdbc/insert-record :stories (assoc (Story. "test feed type"
                                                "test story title"
                                                "http://test-story.com/story"
                                                "test story description"
                                                #inst "2012-01-01T12:00")
                                          :id 97))
    (is (= 97 (:id (find-story-by-url @test-db-pool "http://test-story.com/story"))))
    (is (= nil (find-story-by-url @test-db-pool "http://nonexistent.com/story")))))
