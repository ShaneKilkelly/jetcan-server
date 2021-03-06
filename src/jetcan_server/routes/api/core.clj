(ns jetcan-server.routes.api.core
  (:use compojure.core)
  (:require [liberator.core :refer [defresource]]
            [jetcan-server.auth :as auth]))


(defn get-current-user
  "Get the current user id from context, validating the auth_token header.
   Returns string username (email) if valid.
   Returns nil if the token is either invalid or not present"
  [context]
  (let [auth-token (get-in context [:request :headers "auth_token"])
        current-user (auth/validate-user auth-token)]
    current-user))


(defn is-authenticated?
  "Check if there is a valid auth token in context"
  [context]
  (not (nil? (get-current-user context))))
