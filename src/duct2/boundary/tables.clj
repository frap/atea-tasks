(ns duct2.boundary.tables
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))

(defprotocol Tables
  (get-tables [db]))

(extend-protocol Tables
  duct.database.sql.Boundary
  (get-tables [{:keys [spec]}]
    (jdbc/query spec ["SELECT name FROM sqlite_master WHERE type = 'table'"])))
