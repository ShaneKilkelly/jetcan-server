(ns jetcan-server.test-utils
  (:require [jetcan-server.db.core :refer [db-spec]]
            [jetcan-server.db.user :as user]
            [jetcan-server.auth :as auth]
            [jetcan-server.db.snippet :as snippet]
            [peridot.core :refer :all]
            [jetcan-server.handler :as server]
            [cheshire.core :as json]
            [yesql.core :refer [defquery]]))


(defn load-queries []
  (do
    (defquery -teardown-db! "jetcan_server/reset_data.sql")))
(load-queries)


(defn reset-db! []
  (do (-teardown-db! db-spec)))


(defn populate-users! []
  (do
    (user/create-admin! "userone@example.com"
                        "password1"
                        "User One")
    (user/create! "usertwo@example.com"
                  "password2"
                  "User Two")))


(defn populate-snippets! []
  (do
    (snippet/create! "userone@example.com"
                     "content one"
                     ["one" "two"])))


(def user-one-token
  (auth/authenticate-user "userone@example.com"
                          "password1"))


(def user-two-token
  (auth/authenticate-user "usertwo@example.com"
                          "password2"))


(def good-token
  user-one-token)


(def expired-token
  (str
   "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
   ".eyJlbWFpbCI6InVzZXJvbmVAZXhhbXBsZS5"
   "jb20iLCJuYW1lIjoiVXNlciBPbmUiLCJleHA"
   "iOjEzODQ5NDY4MzcsIm5iZiI6MTM5Mjg5NTY"
   "zN30.hcUu-CZXy_GD9lTkzetonBuWEAcdhy5ydSVxaCbnqnE"))


(def invalid-user-token
  (str
  "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
  ".eyJlbWFpbCI6Im5vdGF1c2VyQGV4YW1wbGU"
  "uY29tIiwibmFtZSI6Ik5vdCBBIFVzZXIiLCJ"
  "leHAiOjE0MDA1ODU4NDUsIm5iZiI6MTM5Mjg"
  "5NjI0NX0.qPkV_umXSEAQ45jRV6cSYCYMQwpz618jXIxxZhS0kYg"))


(defn api-json-request!
  "Make an HTTP request to the server"
  [{:keys [route method body & headers]}]
  (-> (session server/app)
      (content-type "application/json")
      (request route
               :request-method method
               :body (json/generate-string body)
               :headers (or headers {}))))
