(ns radsticks.api-spec
  (:require [radsticks.test-utils :as util]
            [speclj.core :refer :all]
            [peridot.core :refer :all]
            [radsticks.handler :refer :all]
            [cheshire.core :refer [generate-string
                                   parse-string]]))


(describe
  "main route"

  (it "should respond correctly on home route"
    (let [response (:response
                     (-> (session app)
                         (request "/")))]
      (should (= (response :status) 200))))

  (it "should return 404 on unknown route"
    (let [response (:response
                     (-> (session app)
                         (request "/invalid/route")))]
      (should (= (:status response) 404)))))


(describe
  "user api"

  (before
   (do (util/drop-database!)
       (util/populate-users!)))

  (it "should allow a user to be created when params are correct"
    (let [request-body
          "{\"email\":\"qwer@example.com\",
            \"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 201))
      (should (contains? response-json :userProfile))
      (should (map? (response-json :userProfile)))
      (let [profile (response-json :userProfile)]
        (should (= "qwer@example.com" (profile :email)))
        (should (= "Qwer" (profile :name)))
        (should (contains? profile :created))
        (should (string? (profile :created))))))

  (it "should fail when email already exists"
    (let [request-body
          "{\"email\":\"userone@example.com\",
            \"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body))
          response (:response request)]
      (should (= "text/plain"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 403))
      (should (= "Forbidden." (response :body)))))

  (it "should fail when email is not valid"
    (let [request-body
          "{\"email\":\"dippitydoo\",
            \"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors)))))

  (it "should fail when email is missing"
    (let [request-body
          "{\"password\":\"password3\",
            \"name\": \"Qwer\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors)))))

  (it "should fail when password is missing"
    (let [request-body
          "{\"email\":\"qwer2@example.com\",
            \"name\": \"Qwer2\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors)))))

  (it "should fail when name is missing"
    (let [request-body
          "{\"email\":\"qwer2@example.com\",
            \"password\": \"password2\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/user"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 400))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors))))))


(describe
  "auth api"

  (before
   (do (util/drop-database!)
       (util/populate-users!)))

  (it "should issue a token when credentials are correct"
    (let [request-body
          "{\"email\":\"userone@example.com\",
            \"password\":\"password1\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/auth"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (= (:status response) 201))
      (should (contains? response-json :token))
      (should (string? (response-json :token)))
      (should (< 0 (count (response-json :token))))
      (should (= "userone@example.com" (response-json :email)))))

  (it "should fail to authenticate when email is unknown"
    (let [request-body
          "{\"email\":\"gooser@example.com\",
            \"password\":\"lol\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/auth"
                               :request-method :post
                               :body request-body))
          response (:response request)]
      (should (= "text/plain"
                 (get (:headers response) "Content-Type")))
      (should (not (= (:status response) 201)))
      (should (= (:status response) 403))
      (should (= "Forbidden." (response :body)))))

  (it "should fail to authenticate when password is incorrect"
    (let [request-body
          "{\"email\":\"userone@example.com\",
            \"password\":\"lol\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/auth"
                               :request-method :post
                               :body request-body))
          response (:response request)]
      (should (= "text/plain"
                 (get (:headers response) "Content-Type")))
      (should (not (= (:status response) 201)))
      (should (= (:status response) 403))
      (should (= "Forbidden." (response :body)))))

  (it "should fail when user email is not submitted"
    (let [request-body
          "{\"derp\":\"userone@example.com\",
            \"password\":\"password1\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/auth"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (not (= (:status response) 201)))
      (should (= (:status response) 400))
      (should (not (contains? response-json :token)))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors)))))

  (it "should fail when password is not submitted"
    (let [request-body
          "{\"email\":\"userone@example.com\",
            \"derp\":\"password1\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/auth"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (not (= (:status response) 201)))
      (should (= (:status response) 400))
      (should (not (contains? response-json :token)))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors)))))

  (it "should fail when the supplied email is not a string"
    (let [request-body
          "{\"email\":[1,2,3],
            \"password\":\"password1\"}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/auth"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (not (= (:status response) 201)))
      (should (= (:status response) 400))
      (should (not (contains? response-json :token)))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors)))))

  (it "should fail when password is not a string"
    (let [request-body
          "{\"email\":\"userone@example.com\",
            \"password\":true}"
          request (-> (session app)
                      (content-type "application/json")
                      (request "/api/auth"
                               :request-method :post
                               :body request-body))
          response (:response request)
          response-json (parse-string (response :body) true)]
      (should (= "application/json;charset=UTF-8"
                 (get (:headers response) "Content-Type")))
      (should (not (= (:status response) 201)))
      (should (= (:status response) 400))
      (should (not (contains? response-json :token)))
      (should (contains? response-json :errors))
      (should (vector? (response-json :errors))))))


(run-specs)

