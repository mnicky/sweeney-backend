(ns sweeney-backend.feeds-test
  (:require [clojure.java.io :as io])
  (:require [clojure.java.jdbc :as jdbc])
  (:use clojure.test
        sweeney-backend.feeds
        sweeney-backend.dbpool)
  (:import sweeney_backend.feeds.Story))

(def rss-file "./test/resources/test-rss1.xml")

(def test-db {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname ":memory:"
              :user "test"
              :password "test"})

(def test-db-pool (delay (db-pool test-db)))

(defn create-test-tables
  [db-conn]
  (jdbc/with-connection db-conn
    (jdbc/create-table :stories
                       [:id :integer]
                       [:feed_id :integer]
                       [:feed_type :text]
                       [:title :text]
                       [:url :text]
                       [:description :text]
                       [:published_at :date])))

(use-fixtures :once (fn [x] (create-test-tables @test-db-pool) (x)))

(deftest parse-feed-test
  (let [feed (io/file rss-file)]
    (is (instance? java.io.File feed))
    (is (.exists feed))

    (let [parsed (parse-feed feed)
          article (first parsed)]
      (is (= 25 (count parsed)))
      (is (= 5 (count article)))
      (is (= (:feed_type article) "RssFeed"))
      (is (= (:title article) "Opera zvyšuje stabilitu, oddělila pluginy do vlastních procesů"))
      (is (= (:url article) "http://www.root.cz/zpravicky/opera-zvysuje-stabilitu-oddelila-pluginy-do-vlastnich-procesu/#utm_source=rss&utm_medium=text&utm_campaign=rss"))
      (is (= (:description article) "Vyšla nová verze internetového prohlížeče Opera 12. Kromě nové podpory snadné změny vzhledu je zde i množství technických věcí, které nejsou na první pohled vidět. Například rychlejší start i s množstvím panelů, oddělení pluginů do samostatných..."))
      (is (= (:published_at article) #inst "2012-06-15T14:53:13.000-00:00")))))

(deftest save-story-test
  (let [story (Story. "test-feed-type" "test-feed-title" "test-feed-url" "test-feed-description" #inst "2012-01-01T12:00")]
    (save-story @test-db-pool story 12)
    (jdbc/with-connection @test-db-pool
      (jdbc/with-query-results res
        ["SELECT * FROM stories LIMIT 1"]
        (is (= (first res)
               {:id nil
                :feed_id 12
                :feed_type "test-feed-type"
                :title "test-feed-title"
                :url "test-feed-url"
                :description "test-feed-description"
                :published_at (.getTime #inst "2012-01-01T12:00")} ))))))
