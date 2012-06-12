(defproject sweeney-backend "0.1.0-SNAPSHOT"
  :description "Backend for Sweeney."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                   [org.clojure/core.incubator "0.1.0"]
                   [overtone/at-at "1.0.0"]
                   [org.clojars.scsibug/feedparser-clj "0.4.0"]]
  :java-source-paths ["src/java"]
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"])
