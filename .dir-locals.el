((clojure-mode
   ;; Some code style settings I am trying
   (clojure-indent-style . :always-align)
   (indent-tabs-mode . nil)
   (fill-column . 80)

   ;; prefer clojure-cli over other build tools
   (cider-preferred-build-tool . clojure-cli)

   ;; ask to load the specified aliases, please note that more than one
   ;; alias can be specified
   (cider-clojure-cli-global-options . "-A:dev/utils:dev/nrepl"))) 

