(ns atomist.main
  (:require [atomist.api :as api]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [goog.string :as gstring]
            [cljs.test :refer [deftest is run-tests async]]
            [atomist.local-runner :as lr]
            [atomist.promise :as p]
            [atomist.cljs-log :as log]
            [atomist.json :as json]))

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

(deftest handler-tests
  (lr/set-env :prod-github-auth)
  (async done
    (enable-console-print!)
    (go
     (is (= :done
            (<! (-> (lr/fake-command-handler "T29E48P34" "command" "raw message" "C0128TZHVGE" "U2ATJPCSK")
                    (assoc :fake-handler (fn [& args]
                                           (let [m (js->clj (first args) :keywordize-keys true)]
                                             (when (= "application/x-atomist-slack+json" (:content_type m))
                                               (is
                                                (= [{:id "callback" :command "callback"}]
                                                   (map #(dissoc % :parameters) (:actions m))))
                                               (is
                                                (= "callback" (-> m :body (json/->obj) :blocks (json/->obj) first :accessory :action_id)))))))
                    (lr/call-event-handler handler)
                    (p/from-promise)))))
     (done))))
