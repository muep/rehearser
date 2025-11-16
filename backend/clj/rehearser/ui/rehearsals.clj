(ns rehearser.ui.rehearsals
  (:require
   [rehearser.ui.rehearsals.detail :as detail]
   [rehearser.ui.rehearsals.entry :as entry]
   [rehearser.ui.rehearsals.entry-add :as entry-add]
   [rehearser.ui.rehearsals.index :as index]))

(def routes (concat detail/routes
                    entry/routes
                    entry-add/routes
                    index/routes))
