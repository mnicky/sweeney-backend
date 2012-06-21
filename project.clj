(defproject sweeney-backend "0.1.0-SNAPSHOT"
  :description "Backend for Sweeney."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                   [org.clojure/core.incubator "0.1.0"]
                   [overtone/at-at "1.0.0"]
                   [org.clojars.scsibug/feedparser-clj "0.4.0"]
                   [korma "0.3.0-beta11"]
                   [c3p0/c3p0 "0.9.1.2"]
                   ;[com.jolbox/bonecp "0.7.1.RELEASE"]
                   [postgresql "9.1-901-1.jdbc4"]]
  :profiles {:dev {:dependencies [[org.xerial/sqlite-jdbc "3.7.2"]]
                     ;:warn-on-reflection true
                     }}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clojure"])

