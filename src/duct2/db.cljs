(ns duct2.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]))


;; -- Spec --------------------------------------------------------------------
;;
;; This is a clojure.spec specification for the value in app-db. It is like a
;; Schema. See: http://clojure.org/guides/spec
;;
;; The value in app-db should always match this spec. Only event handlers
;; can change the value in app-db so, after each event handler
;; has run, we re-check app-db for correctness (compliance with the Schema).
;;
;; How is this done? Look in events.cljs and you'll notice that all handlers
;; have an "after" interceptor which does the spec re-check.
;;
;; None of this is strictly necessary. It could be omitted. But we find it
;; good practice.

(s/def ::id int?)
(s/def ::work string?)
(s/def ::tag string?)
(s/def ::done boolean?)
(s/def ::task (s/keys :req-un [::id ::work ::done]
                      :opt-un [::tag]))
(s/def ::tasks (s/and                                       ;; should use the :kind kw to s/map-of (not supported yet)
                 (s/map-of ::id ::task)                     ;; in this map, each task is keyed by its :id
                 #(instance? PersistentTreeMap %)           ;; is a sorted-map (not just a map)
                 ))
(s/def ::showing                                            ;; what tasks are shown to the user?
  #{:all                                                    ;; all tasks are shown
    :active                                                 ;; only tasks whose :done is false
    :done                                                   ;; only tasks whose :done is true
    })
(s/def ::db (s/keys :req-un [::tasks ::showing]))

;; -- Default app-db Value  ---------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db
;; Unless, of course, there are tasks in the LocalStore (see further below)
;; Look in `core.cljs` for  "(dispatch-sync [:initialise-db])"
;;

(def default-value                                          ;; what gets put into app-db by default.
  {:tasks   (sorted-map)                                    ;; an empty list of tasks. Use the (int) :id as the key
   :showing :all})                                          ;; show all tasks


;; -- Local Storage  ----------------------------------------------------------
;;
;; Part of the taskmvc challenge is to store tasks in LocalStorage, and
;; on app startup, reload the tasks from when the program was last run.
;; But the challenge stipulates to NOT  load the setting for the "showing"
;; filter. Just the tasks.
;;

(def ls-key "atea-tasks")                          ;; localstore key
(defn tasks->local-store
  "Puts tasks into localStorage"
  [tasks]
  (.setItem js/localStorage ls-key (str tasks)))     ;; sorted-map writen as an EDN map


;; register a coeffect handler which will load a value from localstore
;; To see it used look in events.clj at the event handler for `:initialise-db`
(re-frame/reg-cofx
  :local-store-tasks
  (fn [cofx _]
      "Read in tasks from localstore, and process into a map we can merge into app-db."
      (assoc cofx :local-store-tasks
             (into (sorted-map)
                   (some->> (.getItem js/localStorage ls-key)
                            (cljs.reader/read-string)       ;; stored as an EDN map.
                            )))))
