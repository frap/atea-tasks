(ns duct2.handler.example-test
  (:require [clojure.test :refer :all]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [integrant.core :as ig]
            [duct2.handler.example :as example]))

(def handler
  (ig/init-key :duct2.handler/example {}))

(deftest smoke-test
  (testing "task page exists"
    (-> (session handler)
        (visit "/hello")
        (has (status? 200) "page exists"))))
