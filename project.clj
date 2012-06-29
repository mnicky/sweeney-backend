(defproject sweeney-backend "0.1.0-SNAPSHOT"
  :description "Backend for Sweeney."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :comments "the same as Clojure"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/core.incubator "0.1.0"]
                 [overtone/at-at "1.0.0"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [wakeful "0.3.3"]
                 [ring/ring-jetty-adapter "1.1.1"]
                 [clj-json "0.5.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [org.ccil.cowan.tagsoup/tagsoup "1.2.1"]]
  :main sweeney-backend.core
  :profiles {:dev {:dependencies [[org.xerial/sqlite-jdbc "3.7.2"]]
                     ;:warn-on-reflection true
                     }}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clojure"])
