(ns atomist.main
  (:require [atomist.api :as api]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [goog.string :as gstring]))

(defn command-handler [{:keys [raw_message source] :as request}]
  (go
   (<! (api/block-message
        request
        [{:type "section"
          :text {:type "mrkdwn"
                 :text (gstring/format "%s just sent the command `%s`"
                                       (-> source :slack :user :name)
                                       raw_message)}
          :accessory {:type "button"
                      :atomist/action {:id :callback :parameters []}
                      :text {:type "plain_text"
                             :text "send callback"}
                      :value "you'll get this data back in the callback!"}}]))))

(defn button-callback [{:keys [source] :as request}]
  (go
   (<! (api/block-message
        request
        [{:type "section"
          :text {:type "mrkdwn"
                 :text (gstring/format "%s just clicked the callback button"
                                       (-> source :slack :user :name))}
          :accessory {:type "button"
                      :atomist/action {:id :delete :parameters []}
                      :text {:type "plain_text"
                             :text "send callback"}
                      :value "you'll get this data back in the callback!"}}]))))

(defn button-delete-message [request]
  (go
   (<! (api/delete-message request))))

(def ^:export handler
  (api/command-handlers
   {:command command-handler
    :callback button-callback
    :delete button-delete-message}))
