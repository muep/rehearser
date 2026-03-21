(ns rehearser.ui
  (:require
   [rehearser.ui.import :as import]
   [rehearser.ui.index :as index]
   [rehearser.ui.rehearsals :as rehearsals]
   [rehearser.ui.signup :as signup]
   [rehearser.ui.tunes :as tunes]))

(def routes
  (concat
   import/routes
   index/routes
   rehearsals/routes
   signup/routes
   tunes/routes))
