(ns rehearser.ui
  (:require
   [rehearser.ui.index :as index]
   [rehearser.ui.rehearsals :as rehearsals]
   [rehearser.ui.tunes :as tunes]))

(def routes
  (concat
   rehearsals/routes
   [["/index.html"
     {:get {:handler index/html}}]
    ["/rehearsals/:rehearsal-id/new-entry.html"
     {:get {:parameters {:path {:rehearsal-id int?}}
            :handler rehearsals/entry-add-page}
      :post {:parameters {:path {:rehearsal-id int?}
                          :form {:remarks string?
                                 :variant-id int?
                                 :exercise-id int?}}
             :handler rehearsals/entry-add!}}]
    ["/tunes.html"
     {:get {:handler tunes/tune-listing-page}}]
    ["/tunes/:id/tune.html"
     {:get {:parameters {:path {:id int?}}
            :handler tunes/tune-details-page}
      :post {:parameters {:path {:id int?}
                          :form {:title string?
                                 :description string?}}
             :handler tunes/tune-details-post}}]
    ["/tunes/new-tune.html"
     {:get {:handler tunes/tune-add-page}
      :post {:parameters {:form {:title string?
                                 :description string?}}
             :handler tunes/tune-post!}}]]))
