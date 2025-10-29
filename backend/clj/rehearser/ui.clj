(ns rehearser.ui
  (:require
   [rehearser.ui.index :as index]
   [rehearser.ui.rehearsals :as rehearsals]
   [rehearser.ui.tunes :as tunes]))

(def routes
  [["/index.html"
    {:get {:handler index/html}}]
   ["/rehearsals.html"
    {:get {:handler rehearsals/rehearsal-index-page}
     :post {:parameters {:form {:title string?}}
            :handler rehearsals/rehearsal-post!}}]
   ["/rehearsals/:id/rehearsal.html"
    {:get {:parameters {:path {:id int?}}}
     :handler rehearsals/rehearsal-page}]
   ["/rehearsals/:rehearsal-id/entry/:id/entry.html"
    {:get {:parameters {:path {:rehearsal-id int?
                               :id int?}}
           :handler rehearsals/entry-page}
     :post {:parameters {:path {:rehearsal-id int?
                                :id int?}
                         :form [:map
                                [:remarks {:optional true} string?]
                                [:variant-id {:optional true} int?]
                                [:exercise-id {:optional true} int?]]}
           :handler rehearsals/entry-put!}}]
   ["/rehearsals/:rehearsal-id/new-entry.html"
    {:get {:parameters {:path {:rehearsal-id int?}}
           :handler rehearsals/entry-add-page}
     :post {:parameters {:path {:rehearsal-id int?}
                         :form {:remarks string?
                                :variant-id int?
                                :exercise-id int?}}
            :handler rehearsals/entry-add!}}]
   ["/rehearsals/:id/close.html"
    {:post {:parameters {:path {:id int?}}
            :handler rehearsals/rehearsal-close!}}]
   ["/rehearsals/:id/open.html"
    {:post {:parameters {:path {:id int?}}
            :handler rehearsals/rehearsal-open!}}]
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
            :handler tunes/tune-post!}}]])
