(ns clojure-line-bot.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [cheshire.core :refer [parse-string generate-string]]
            [clj-http.client :as http-client]
            [taoensso.timbre :as timbre :refer [error]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.adapter.jetty :as jetty]))

(def line-channel-token (env :line-channel-token))
(def line-api-endpoint "https://api.line.me/v2/bot")
(def line-api-reply-path "/message/reply")

(defn reply [to-user-id reply-token message]
  (let [body (generate-string {:to to-user-id
                               :replyToken reply-token
                               :messages [{:type "text"
                                           :text message}]})]
    (try
      (http-client/post (str line-api-endpoint line-api-reply-path)
                        {:body body
                         :headers {"Authorization" (str "Bearer " line-channel-token)}
                         :content-type :json})
      (catch Exception e
        (let [exception-data (.getData e)
              status (:status exception-data)
              message (:body exception-data)]
          (error (format "status %d. %s" status message)))))))

(defroutes app-routes
  (POST "/linebot/callback" {body :body}
    (->> (parse-string (slurp body) true)
         :events
         (filter #(and
                   (= (:type %) "message")
                   (= (get-in % [:message :type]) "text")))
         (map #(reply (get-in % [:source :userId])
                      (:replyToken %)
                      (get-in % [:message :text])))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes (assoc-in api-defaults
                                      [:params :urlencoded] false)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
