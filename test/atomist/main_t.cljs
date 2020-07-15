(ns atomist.main-t
  (:require [atomist.main :refer [handler]]
            [cljs.core.async :refer [<!] :refer-macros [go]]
            [cljs.test :refer [deftest is run-tests async]]
            [atomist.local-runner :as lr]
            [atomist.promise :as p]
            [atomist.json :as json]))

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

(comment
 (run-tests))