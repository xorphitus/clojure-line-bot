(ns clojure-line-bot.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.adapter.jetty :as jetty]))

(defroutes app-routes
  (POST "/linebot/callback" {body :body} body)
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc-in api-defaults
                                      [:params :urlencoded] false)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
