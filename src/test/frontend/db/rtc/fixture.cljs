(ns frontend.db.rtc.fixture
  (:require [cljs.test :as t]
            [cljs.core.async :as async :refer [<! >! chan go]]
            [frontend.db.rtc.mock :as rtc-mock]
            [frontend.db.rtc.core :as rtc-core]
            [frontend.test.helper :as test-helper]))

(def *test-rtc-state (atom nil))

(defn- init-state-helper
  []
  (let [data-from-ws-chan (chan (async/sliding-buffer 100))
        ws (rtc-mock/mock-websocket data-from-ws-chan)]
    (assoc (rtc-core/init-state ws data-from-ws-chan)
           :*auto-push-client-ops? (atom false))))

(defn- <start-rtc-loop
  []
  (go
    (let [graph-uuid "e56287f0-44de-487d-8b9f-02e91ec57d98" ; just random generated
          repo test-helper/test-db
          state (init-state-helper)
          loop-started-ch (chan)]
      (reset! *test-rtc-state state)
      (rtc-core/<loop-for-rtc state graph-uuid repo :loop-started-ch loop-started-ch)
      (<! loop-started-ch))))


(def start-and-stop-rtc-loop-fixture
  {:before
   #(t/async done
             (go
               (<! (<start-rtc-loop))
               (prn :<started-rtc-loop)
               (done)))
   :after
   #(t/async done
             (go
               (when-let [stop-rtc-loop-chan (some-> (:*stop-rtc-loop-chan @*test-rtc-state) deref)]
                 (prn :stopping-rtc-loop)
                 (>! stop-rtc-loop-chan true))
               (reset! *test-rtc-state nil)
               (done)))})