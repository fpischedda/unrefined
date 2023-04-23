(ns build
  (:require [clojure.tools.build.api :as b]))

(def app 'fpsd/unrefined)
(def version (format "0.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name app) version))
(def extension-out-dir "extension-build")
(def extension-file (format "%s-%s.zip" (name app) version))

(defn clean_ [path]
  (b/delete {:path path}))

(defn apply-prod-settings [env]
  (b/copy-file {:src (format "%s/env/%s.js" extension-out-dir env)
                :target (format "%s/settings.js" extension-out-dir)})
  (clean_ (format "%s/env" extension-out-dir)))

(defn clean [_]
  (clean_ {:path "target"}))

(defn extension [_]
  (println "Building browser extension...")

  (clean_ extension-out-dir)

  (b/copy-dir {:src-dirs ["browser-extension"]
               :target-dir extension-out-dir})
  
  (apply-prod-settings "production")

  (clean_ (format "%s/.editorconfig" extension-out-dir))

  (b/zip {:src-dirs [extension-out-dir]
          :zip-file extension-file})

  (println "Done building browser extension."))


(defn uber [_]
  (clean_ "target")

  (println "Building standalone uberfile...")

  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})

  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})

  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'fpsd.unrefined.server})

  (println "Done building standalone uberfile."))
