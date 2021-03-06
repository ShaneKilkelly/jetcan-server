(ns jetcan-server.api-user-spec
  (:require [jetcan-server.test-utils :as util]
            [speclj.core :refer :all]
            [peridot.core :refer :all]
            [jetcan-server.handler :refer :all]
            [jetcan-server.db.user :as user]
            [cheshire.core :refer [generate-string
                                   parse-string]]))


(describe
  "user creation"

  (before
   (do (util/reset-db!)
       (util/populate-users!)))

  (it "should allow a user to be created when params are correct,
       and current user is admin"
    (let [request-body
          "{\"id\":\"qwer@example.com\",
            \"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body
                               :headers {:auth_token
                                         util/user-one-token}))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response)
                 201))
      (should (contains? response-json :userProfile))
      (should (map? (response-json :userProfile)))
      (let [profile (response-json :userProfile)]
        (should== [:id :name :admin :created :disabled]
                  (keys profile))
        (should (= "qwer@example.com"
                   (profile :id)))
        (should (= "Qwer"
                   (profile :name)))
        (should-contain :admin profile)
        (should= false
                 (profile :admin))
        (should (contains? profile :created))
        (should (string? (profile :created)))
        (should (contains? profile :disabled))
        (should= false (profile :disabled)))))

  (it "should fail when current user is not admin"
    (let [request-body
          "{\"id\":\"qwer@example.com\",
            \"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body
                               :headers {:auth_token ;; non-admin user
                                         util/user-two-token}))
          response (:response request)]
      (should (= "text/plain"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 401))
      (should (= "Not authorized." (response :body)))))

  (it "should fail when id already exists"
    (let [request-body
          "{\"id\":\"userone@example.com\",
            \"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body
                               :headers {:auth_token
                                         util/user-one-token}))
          response (:response request)]
      (should (= "text/plain"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 403))
      (should (= "Forbidden." (response :body)))))

  (it "should fail when id is not valid"
    (let [request-body
          "{\"id\":3,
            \"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body
                               :headers {:auth_token
                                         util/user-one-token}))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (map? (response-json :errors)))))

  (it "should fail when id is missing"
    (let [request-body
          "{\"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body
                               :headers {:auth_token
                                         util/user-one-token}))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (map? (response-json :errors)))))

  (it "should fail when password is missing"
    (let [request-body
          "{\"id\":\"qwer2@example.com\",
            \"name\": \"Qwer2\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body
                               :headers {:auth_token
                                         util/user-one-token}))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (map? (response-json :errors)))))

  (it "should fail when name is missing"
    (let [request-body
          "{\"id\":\"qwer2@example.com\",
            \"password\": \"password2\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body
                               :headers {:auth_token
                                         util/user-one-token}))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (map? (response-json :errors))))))


(describe
  "user profile api, reads"

  (it "should not allow a profile to be read with no auth_token"
      (let [request (-> (session app)
                        (request "/api/user/userone@example.com"
                                 :request-method :get))
            response (:response request)]
        (should= 401 (response :status))
        (should-not= "application/json;charset=UTF-8"
                     (get (:headers response) "Content-Type"))))

  (it "should not allow a profile to be read when not the current user"
      (let [request (-> (session app)
                        (request "/api/user/usertwo@example.com"
                                 :request-method :get
                                 :headers {:auth_token
                                           util/good-token}))
            response (:response request)]
        (should= 401 (response :status))
        (should-not= "application/json;charset=UTF-8"
                     (get (:headers response) "Content-Type"))))

  (it "should return profile when auth_token is supplied and is user"
      (let [request (-> (session app)
                        (request "/api/user/userone@example.com"
                                 :request-method :get
                                 :headers {:auth_token
                                           util/good-token}))
            response (:response request)
            profile (parse-string (response :body) true)]
        (should= 200 (response :status))
        (should (map? profile))
        (should== [:id :name :created :admin :disabled] (keys profile))
        (should= "userone@example.com" (profile :id))
        (should= "User One" (profile :name))
        (should-be string? (profile :created))
        (should= true (profile :admin))
        (should (contains? profile :disabled))
        (should= false (profile :disabled)))))


(describe
  "user profile api, writes"

  (it "should forbid an update without auth token"
      (let [request-body
            "{\"name\": \"OTHER NAME\"}"
            request (-> (session app)
                        (content-type "application/json")
                        (request "/api/user/userone@example.com"
                                 :request-method :post
                                 :body request-body))
            response (request :response)]
        (should= 401 (response :status))
        (should-be string? (response :body))
        (should= "Not authorized." (response :body))))

  (it "should forbid an update to another users profile"
      (let [request-body
            "{\"name\": \"OTHER NAME\"}"
            request (-> (session app)
                        (content-type "application/json")
                        (request "/api/user/userone@example.com"
                                 :request-method :post
                                 :body request-body
                                 :headers {:auth_token
                                           util/user-two-token}))
            response (request :response)]
        (should= 401 (response :status))
        (should-be string? (response :body))
        (should= "Not authorized." (response :body))))

  (it "should allow admin to update another users profile"
      (let [request-body
            "{\"name\": \"OTHER NAME\"}"
            request (-> (session app)
                        (content-type "application/json")
                        (request "/api/user/usertwo@example.com"
                                 :request-method :post
                                 :body request-body
                                 :headers {:auth_token
                                           util/user-one-token}))
            response (request :response)]
        (should= 200 (response :status))
        (let [response-json (parse-string (response :body) true)]
          (should-be map? response-json)
          (should-not-contain :errors response-json)
          (should-contain :id response-json)
          (should-contain :name response-json)
          (should-contain :created response-json)
          (should= "usertwo@example.com" (response-json :id))
          (should= "OTHER NAME" (response-json :name)))))

  (it "should be an error if name is omitted"
      (let [request-body
            "{\"zzz\": \"OTHER NAME\"}"
            request (-> (session app)
                        (content-type "application/json")
                        (request "/api/user/userone@example.com"
                                 :request-method :post
                                 :body request-body
                                 :headers {:auth_token
                                           util/good-token}))
            response (request :response)
            response-json (parse-string (response :body) true)]
        (should= 400 (response :status))
        (should-be map? response-json)
        (should-contain :errors response-json)
        (should== {:name ["is invalid" "can't be blank"]}
                  (response-json :errors))))

  (it "should be an error if specified id does not exist"
      (let [request-body
            "{\"name\": \"OTHER NAME\"}"
            request (-> (session app)
                        (content-type "application/json")
                        (request "/api/user/a_bad_id"
                                 :request-method :post
                                 :body request-body
                                 :headers {:auth_token
                                           util/good-token}))
            response (request :response)]
        (should= 404 (response :status))
        (should= "Resource not found." (:body response))))

  (it "should be an error if name is not a string"
      (let [request-body
            "{\"name\": 42}"
            request (-> (session app)
                        (content-type "application/json")
                        (request "/api/user/userone@example.com"
                                 :request-method :post
                                 :body request-body
                                 :headers {:auth_token
                                           util/good-token}))
            response (request :response)
            response-json (parse-string (response :body) true)]
        (should= 400 (response :status))
        (should-be map? response-json)
        (should-contain :errors response-json)
        (should== {:name ["is invalid"]} (response-json :errors))))

  (it "should update profile to new values with good auth token"
      (let [old-profile (user/get-profile "userone@example.com")
            request-body
            "{\"name\": \"OTHER NAME\"}"
            request (-> (session app)
                        (content-type "application/json")
                        (request "/api/user/userone@example.com"
                                 :request-method :post
                                 :body request-body
                                 :headers {:auth_token
                                           util/good-token}))
            response (request :response)
            response-json (parse-string (response :body) true)]
        (should= 200 (response :status))
        (should-be map? response-json)
        (should-not-contain :errors response-json)
        (should-contain :id response-json)
        (should-contain :name response-json)
        (should-contain :created response-json)
        (should= "userone@example.com" (response-json :id))
        (should= "OTHER NAME" (response-json :name))
        (should-not= (old-profile :name) (response-json :name)))))


(describe "user list reads"

  (before
   (do (util/reset-db!)
       (util/populate-users!)))

  (it "should allow an admin user to get a list of all users"
      (let [request (-> (session app)
                        (request "/api/user"
                                 :request-method :get
                                 :headers {:auth_token
                                           util/user-one-token}))
            response (:response request)
            profiles (parse-string (:body response) true)]
        (should= 200 (:status response))
        (should= 2 (count profiles))
        (should== ["userone@example.com" "usertwo@example.com"]
                  (map :id profiles))
        (doseq [user profiles]
          (should== [:id :name :admin :created :disabled]
                    (keys user)))))

  (it "should forbid reading list of users if current user is not admin"
      (let [request (-> (session app)
                        (request "/api/user"
                                 :request-method :get
                                 :headers {:auth_token util/user-two-token}))
            response (:response request)]
        (should= 401 (response :status))
        (should-not= "application/json;charset=UTF-8"
                     (get (:headers response) "Content-Type"))))

  (it "should forbid reading list of users if no auth-token is submitted"
      (let [request (-> (session app)
                        (request "/api/user"
                                 :request-method :get))
            response (:response request)]
        (should= 401 (response :status))
        (should-not= "application/json;charset=UTF-8"
                     (get (:headers response) "Content-Type")))))


(describe "user disable"

  (before
   (do (util/reset-db!)
       (util/populate-users!)))

  (it "should allow an admin user to disable another user"
      (let [old-profile (user/get-profile "usertwo@example.com")
            request (util/api-json-request!
                     {:route "/api/user/usertwo@example.com/disabled"
                      :method :put
                      :body {:disabled true}
                      :headers {:auth_token
                                util/user-one-token}})
            response (request :response)
            response-json (parse-string (response :body) true)
            new-profile (response-json :userProfile)]
        (should= 200 (response :status))
        (should-be map? new-profile)
        (should= false (old-profile :disabled))
        (should= true (new-profile :disabled))))

  (it "should allow an admin user to re-enable another user"
      (let [old-profile (user/get-profile "usertwo@example.com")
            request (util/api-json-request!
                     {:route "/api/user/usertwo@example.com/disabled"
                      :method :put
                      :body {:disabled true}
                      :headers {:auth_token
                                util/user-one-token}})
            response (request :response)
            response-json (parse-string (response :body) true)
            new-profile (response-json :userProfile)]
        (should= 200 (response :status))
        (should= false (old-profile :disabled))
        (should= true (new-profile :disabled))
        ;; re-enable the user
        (let [old-profile (user/get-profile "usertwo@example.com")
              request (util/api-json-request!
                       {:route "/api/user/usertwo@example.com/disabled"
                        :method :put
                        :body {:disabled false}
                        :headers {:auth_token
                                  util/user-one-token}})
              response (request :response)
              response-json (parse-string (response :body) true)
              new-profile (response-json :userProfile)]
          (should= 200 (response :status))
          (should= true (old-profile :disabled))
          (should= false (new-profile :disabled)))))

  (it "should not allow a non-admin user to disable another user"
      (let [request (util/api-json-request!
                     {:route "/api/user/userone@example.com/disabled"
                      :method :put
                      :body {:disabled true}
                      :headers {:auth_token
                                util/user-two-token}})
            response (request :response)
            profile (user/get-profile "userone@example.com")]
        (should= 401 (response :status))
        (should= "Not authorized." (response :body))
        (should= false (profile :disabled))))

  (it "should not allow a non-admin user to re-enable another user"
      (do
        (user/update-user-disabled-status! "userone@example.com" true)
        (let [request (util/api-json-request!
                       {:route "/api/user/userone@example.com/disabled"
                        :method :put
                        :body {:disabled false}
                        :headers {:auth_token
                                  util/user-two-token}})
              response (request :response)
              profile (user/get-profile "userone@example.com")]
          (should= 401 (response :status))
          (should= "Not authorized." (response :body))
          (should= true (profile :disabled)))))

  (it "should not allow anything without an auth token"
      (let [request (util/api-json-request!
                      {:route "/api/user/usertwo@example.com/disabled"
                       :method :put
                       :body "{\"disabled\": true}"})
            response (request :response)]
        (should= 401 (response :status))
        (should-be string? (response :body))
        (should= "Not authorized." (response :body)))))



(describe "user admin change"

  (before
   (do (util/reset-db!)
       (util/populate-users!)))

  (it "should allow an admin user to make another user an admin"
      (let [old-profile (user/get-profile "usertwo@example.com")
            request (util/api-json-request!
                     {:route "/api/user/usertwo@example.com/admin"
                      :method :put
                      :body {:admin true}
                      :headers {:auth_token
                                util/user-one-token}})
            response (request :response)
            response-json (parse-string (response :body) true)
            new-profile (response-json :userProfile)]
        (should= 200 (response :status))
        (should-be map? new-profile)
        (should= false (old-profile :admin))
        (should= true (new-profile :admin))))

  (it "should allow an admin user to take admin rights away from another user"
      (let [old-profile (user/get-profile "usertwo@example.com")
            request (util/api-json-request!
                     {:route "/api/user/usertwo@example.com/admin"
                      :method :put
                      :body {:admin true}
                      :headers {:auth_token
                                util/user-one-token}})
            response (request :response)
            response-json (parse-string (response :body) true)
            new-profile (response-json :userProfile)]
        (should= 200 (response :status))
        (should= false (old-profile :admin))
        (should= true (new-profile :admin))
        ;; take admin away
        (let [old-profile (user/get-profile "usertwo@example.com")
              request (util/api-json-request!
                       {:route "/api/user/usertwo@example.com/admin"
                        :method :put
                        :body {:admin false}
                        :headers {:auth_token
                                  util/user-one-token}})
              response (request :response)
              response-json (parse-string (response :body) true)
              new-profile (response-json :userProfile)]
          (should= 200 (response :status))
          (should= true (old-profile :admin))
          (should= false (new-profile :admin)))))

  (it "should not allow a non-admin user to change admin status of another user"
      (let [request (util/api-json-request!
                     {:route "/api/user/userone@example.com/admin"
                      :method :put
                      :body {:admin false}
                      :headers {:auth_token
                                util/user-two-token}})
            response (request :response)
            profile (user/get-profile "userone@example.com")]
        (should= 401 (response :status))
        (should= "Not authorized." (response :body))
        (should= true (profile :admin))))

  (it "should not allow the root admin user to be changed"
      (do
        (user/create-admin! "admin"
                            "password"
                            "Admin")
        (let [request (util/api-json-request!
                       {:route "/api/user/admin/admin"
                        :method :put
                        :body {:admin false}
                        :headers {:auth_token
                                  util/user-one-token}})
              response (request :response)
              profile (user/get-profile "admin")]
          (should= 409 (response :status))
          (should= "Conflict." (response :body))
          (should= true (profile :admin)))))

  (it "should not allow anything without an auth token"
      (let [request (util/api-json-request!
                      {:route "/api/user/usertwo@example.com/admin"
                       :method :put
                       :body {:admin true}})
            response (request :response)]
        (should= 401 (response :status))
        (should-be string? (response :body))
        (should= "Not authorized." (response :body)))))



(run-specs)
