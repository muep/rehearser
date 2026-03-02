(ns rehearser.ui
  (:require
   [rehearser.ui.index :as index]
   [rehearser.ui.rehearsals :as rehearsals]
   [rehearser.ui.signup :as signup]
   [rehearser.ui.tunes :as tunes]))

(def routes
  (concat
   index/routes
   rehearsals/routes
   signup/routes
   tunes/routes))
