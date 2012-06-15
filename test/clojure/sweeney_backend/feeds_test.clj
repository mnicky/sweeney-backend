(ns sweeney-backend.feeds-test
  (:require [clojure.java.io :as io])
  (:use clojure.test
        sweeney-backend.feeds))

(def rss-file "./test/resources/test-rss1.xml")

(deftest parse-feed-test
  (let [feed (io/file rss-file)]
    (is (instance? java.io.File feed))
    (is (.exists feed))

    (let [parsed (parse-feed feed)
          article (first parsed)]
      (is (= 25 (count parsed)))
      (is (= 5 (count article)))
      (is (= (:feed-type article) "RssFeed"))
      (is (= (:title article) "Opera zvyšuje stabilitu, oddělila pluginy do vlastních procesů"))
      (is (= (:url article) "http://www.root.cz/zpravicky/opera-zvysuje-stabilitu-oddelila-pluginy-do-vlastnich-procesu/#utm_source=rss&utm_medium=text&utm_campaign=rss"))
      (is (= (:description article) "Vyšla nová verze internetového prohlížeče Opera 12. Kromě nové podpory snadné změny vzhledu je zde i množství technických věcí, které nejsou na první pohled vidět. Například rychlejší start i s množstvím panelů, oddělení pluginů do samostatných..."))
      (is (= (:published-at article) #inst "2012-06-15T14:53:13.000-00:00")))))
