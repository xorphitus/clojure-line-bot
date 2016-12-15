(ns clojure-line-bot.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [cheshire.core :refer [parse-string generate-string]]
            [clj-http.client :as http-client]
            [pandect.algo.sha256 :refer :all]
            [taoensso.timbre :as timbre :refer [error info]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.adapter.jetty :as jetty])
  (:import (java.util Base64)
           (java.security MessageDigest)))

(def line-channel-token (env :line-channel-token))
(def line-channel-secret (env :line-channel-secret))
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

(defn validate-signature [content signature]
  (let [hash (sha256-hmac-bytes content line-channel-secret)
        decoded-signature (.. Base64 getDecoder (decode signature))]
    (. MessageDigest isEqual hash decoded-signature)))

(def line-events
  {"message" {"text" #(reply (get-in % [:source :userId])
                             (:replyToken %)
                             (get-in % [:message :text]))
              :else #(info (str "messageだけどtext以外が来たよ" %))}
   :else #(info (str "message以外が来たよ" %))})

(defn route-line-events [events]
  (map (fn [event]
         (let [ev-type (:type event)]
           (if-let [handler (get line-events ev-type)]
             (if (or (not= ev-type "message") (fn? handler))
               (handler event)
               (let [sub-type (get-in event [:message :type])
                     sub-events handler]
                 (if-let [sub-handler (get sub-events sub-type)]
                   (sub-handler event)
                   ((:else sub-events) event))))
             ((:else line-events) event))))
       events))

(defroutes app-routes
  (POST "/linebot/callback" {body :body headers :headers}
    (let [content (slurp body)]
      (if (validate-signature content (get headers "x-line-signature"))
        (->> (parse-string content true)
             :events
             route-line-events)
        {:status 400
         :headers {}
         :body "bad request"}))))

(def app
  (wrap-defaults app-routes (assoc-in api-defaults
                                      [:params :urlencoded] false)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
