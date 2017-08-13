(ns duct2.events
  (:require
    [duct2.db    :refer [default-value tasks->local-store]]
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path trim-v
                           after debug]]
    [cljs.spec.alpha     :as s]))


;; -- Interceptors --------------------------------------------------------------
;;

(defn check-and-throw
  "throw an exception if db doesn't match the spec"
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "Task spec check failed: " (s/explain-str a-spec db)) {}))))

;; Event handlers change state, that's their job. But what happens if there's
;; a bug which corrupts app state in some subtle way? This interceptor is run after
;; each event handler has finished, and it checks app-db against a spec.  This
;; helps us detect event handler bugs early.
(def check-spec-interceptor (after (partial check-and-throw :duct2.db/db)))

;; this interceptor stores tasks into local storage
;; we attach it to each event handler which could update tasks
(def ->local-store (after tasks->local-store))

;; Each event handler can have its own set of interceptors (middleware)
;; But we use the same set of interceptors for all event handlers related
;; to manipulating tasks.
;; A chain of interceptors is a vector.
(def task-interceptors [check-spec-interceptor               ;; ensure the spec is still valid
                        (path :tasks)                        ;; 1st param to handler will be the value from this path
                        ->local-store                        ;; write tasks to localstore
                        (when ^boolean js/goog.DEBUG debug)  ;; look in your browser console for debug logs
                        trim-v])                             ;; removes first (event id) element from the event vec


;; -- Helpers -----------------------------------------------------------------

(defn allocate-next-id
  "Returns the next task id.
  Assumes tasks are sorted.
  Returns one more than the current largest id."
  [tasks]
  ((fnil inc 0) (last (keys tasks))))


;; -- Event Handlers ----------------------------------------------------------

;; usage:  (dispatch [:initialise-db])
(reg-event-fx                     ;; on app startup, create initial state
  :initialise-db                  ;; event id being handled
  [(inject-cofx :local-store-tasks)  ;; obtain tasks from localstore
   check-spec-interceptor]                                  ;; after the event handler runs, check that app-db matches the spec
  (fn [{:keys [db local-store-tasks]} _]                    ;; the handler being registered
    {:db (assoc default-value :tasks local-store-tasks)}))  ;; all hail the new state


;; usage:  (dispatch [:set-showing  :active])
(reg-event-db                     ;; this handler changes the task filter
  :set-showing                    ;; event-id

  ;; this chain of two interceptors wrap the handler
  [check-spec-interceptor (path :showing) trim-v]

  ;; The event handler
  ;; Because of the path interceptor above, the 1st parameter to
  ;; the handler below won't be the entire 'db', and instead will
  ;; be the value at a certain path within db, namely :showing.
  ;; Also, the use of the 'trim-v' interceptor means we can omit
  ;; the leading underscore from the 2nd parameter (event vector).
  (fn [old-keyword [new-filter-kw]]  ;; handler
    new-filter-kw))                  ;; return new state for the path


;; usage:  (dispatch [:add-task  "Finish comments"])
(reg-event-db                     ;; given the text, create a new task
  :add-task

  ;; The standard set of interceptors, defined above, which we
  ;; apply to all tasks-modifiing event handlers. Looks after
  ;; writing tasks to local store, etc.
  task-interceptors

  ;; The event handler function.
  ;; The "path" interceptor in `task-interceptors` means 1st parameter is :tasks
  (fn [tasks [text]]
    (let [id (allocate-next-id tasks)]
      (assoc tasks id {:id id :title text :done false}))))


(reg-event-db
  :toggle-done
  task-interceptors
  (fn [tasks [id]]
    (update-in tasks [id :done] not)))


(reg-event-db
  :save
  task-interceptors
  (fn [tasks [id title]]
    (assoc-in tasks [id :title] title)))


(reg-event-db
  :delete-task
  task-interceptors
  (fn [tasks [id]]
    (dissoc tasks id)))


(reg-event-db
  :clear-completed
  task-interceptors
  (fn [tasks _]
    (->> (vals tasks)                ;; find the ids of all tasks where :done is true
         (filter :done)
         (map :id)
         (reduce dissoc tasks))))    ;; now delete these ids


(reg-event-db
  :complete-all-toggle
  task-interceptors
  (fn [tasks _]
    (let [new-done (not-every? :done (vals tasks))]   ;; work out: toggle true or false?
      (reduce #(assoc-in %1 [%2 :done] new-done)
              tasks
              (keys tasks)))))
