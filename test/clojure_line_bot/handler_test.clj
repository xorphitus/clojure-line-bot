(ns clojure-line-bot.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure-line-bot.handler :refer :all]))

(deftest test-validate-signature
  (let [signature "3q8QXTAGaey18yL8FWTqdVlbMr6hcuNvM4tefa0o9nA="
        content "{}"]
    (is (true? (validate-signature content signature))))
  (let [signature "596359635963"
        content "{}"]
    (is (false? (validate-signature content signature)))))
