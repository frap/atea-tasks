(ns duct2.handler.example
  (:require [compojure.core :refer :all]
            [duct2.boundary.todos :as todos]
            [clojure.java.io :as io]
            [integrant.core :as ig]))

(defmethod ig/init-key :duct2.handler/example [_ {:keys [db]}]
  (context "" []
    (GET "/" []
      {:body {:example "data"}}(io/resource "duct2/handler/example/example.html"))
    (GET "/sql/" []
      (pr-str (todos/list-todos db)))))
