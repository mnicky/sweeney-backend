(ns sweeney-backend.actions-test
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc])
  (:use clojure.test
        sweeney-backend.actions
        sweeney-backend.dbpool))

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

(defn check-feed-test-fn
  []
  (let [feed (io/file rss-file)]
    (is (instance? java.io.File feed))
    (is (.exists feed))

    (is (number? (:feed-id (check-feed @test-db-pool feed))))
    (is (instance? Boolean (:checked-before (check-feed @test-db-pool feed))))

    (jdbc/with-connection @test-db-pool
      (jdbc/with-query-results res ["SELECT * FROM rss_feeds"]
        (do
          (is (= 1 (count res)))
          (let [info (first res)]
            (is (= (:id info) 1))
            (is (= (:url info) "./test/resources/test-rss1.xml"))
            (is (= (:title info) "Root.cz - zprávičky"))
            (is (= (:link info) "http://www.root.cz/zpravicky/"))
            (is (= (:image info) "http://ii.iinfo.cz/r/rss-88x31.gif"))))))

    (jdbc/with-connection @test-db-pool
      (jdbc/with-query-results res ["SELECT * FROM stories ORDER BY id"]
        (do
          (is (= 25 (count res)))
          (let [story (first res)]
            (is (= (:id story) 1))
            (is (= (:feed_id story) 1))
            (is (= (:feed_type story) "RssFeed"))
            (is (= (:title story) "Opera zvyšuje stabilitu, oddělila pluginy do vlastních procesů"))
            (is (= (:url story) "http://www.root.cz/zpravicky/opera-zvysuje-stabilitu-oddelila-pluginy-do-vlastnich-procesu/#utm_source=rss&utm_medium=text&utm_campaign=rss"))
            (is (= (:description story) "Vyšla nová verze internetového prohlížeče Opera 12. Kromě nové podpory snadné změny vzhledu je zde i množství technických věcí, které nejsou na první pohled vidět. Například rychlejší start i s množstvím panelů, oddělení pluginů do samostatných..."))
            (is (= (:published_at story) (.getTime #inst "2012-06-15T14:53:13.000-00:00")))))))))

(deftest check-feed-test-1 (check-feed-test-fn))

;the same test, to test whether check-feed ignores feeds and stories if already present
(deftest check-feed-test-2 (check-feed-test-fn))

(deftest avg-story-period-test
  (let [feed (io/file rss-file)]
    (is (instance? java.io.File feed))
    (is (.exists feed))

    (check-feed @test-db-pool feed)
    (is (= 13882000/3 (avg-story-period @test-db-pool 1 3)))))
