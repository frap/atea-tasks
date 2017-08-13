(ns duct2.boundary.todos
  (:require [clojure.java.jdbc :as jdbc]
            [duct.database.sql]))


(defprotocol Todos
  (create-todo [db todo])
  (list-todos  [db])
  (fetch-todo  [db id]))


(extend-protocol Todos
  duct.database.sql.Boundary
  (create-todo [{db :spec} todo]
    (val (ffirst (jdbc/insert! db :todos todo))))
  (list-todos [{db :spec}]
    (jdbc/query db ["SELECT * FROM todos"]))
  (fetch-todo [{db :spec} id]
    (first (jdbc/query db ["SELECT * FROM todos WHERE id = ?" id]))))
