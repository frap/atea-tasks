(defproject duct2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [duct/core "0.5.2"]
                 [duct/module.logging "0.2.0"]
                 [duct/module.web "0.5.5"]
                 [duct/module.cljs "0.2.3"]
                 [duct/module.sql "0.2.2"]
                 [org.xerial/sqlite-jdbc "3.19.3"]
                 [reagent "0.7.0"]
                 [re-frame "0.9.4"]

                 ]
  :plugins [[duct/lein-duct "0.9.2"]]
  :main ^:skip-aot duct2.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user
                         :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.2.0"]
                                   [re-frisk "0.4.5"]
                                   [eftest "0.3.1"]
                                   [kerodon "0.8.0"]]}})
