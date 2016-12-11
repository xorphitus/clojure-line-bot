(defproject clojure-line-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [cheshire "5.6.3"]
                 [environ "1.1.0"]
                 [clj-http "2.3.0"]
                 [pandect "0.6.1"]
                 [com.taoensso/timbre "4.7.4"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.1.0"]]
  :ring {:handler clojure-line-bot.handler/app}
  :uberjar-name "clojure-line-bot-standalone.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}
   :test {:env {:line-channel-secret "SECRET"}}})
