(ns clojure-line-bot.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [cheshire.core :refer [parse-string]]
            [taoensso.timbre :as timbre :refer [info]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.adapter.jetty :as jetty]))

(defroutes app-routes
  (POST "/linebot/callback" {body :body}
    (->> (parse-string (slurp body) true)
         :events
         (filter #(and
                   (= (:type %) "message")
                   (= (get-in % [:message :type]) "text")))
         (map #(info (get-in % [:message :text])))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc-in api-defaults
                                      [:params :urlencoded] false)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
