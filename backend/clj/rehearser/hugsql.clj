(ns rehearser.hugsql
  (:require
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as next-adapter]))

(defn def-db-fns [file]
  (hugsql/def-db-fns file {:adapter (next-adapter/hugsql-adapter-next-jdbc)}))
