angular.module('radsticksApp')
  .service 'Auth', ($http, Notifications) ->
    data =
      username: ''
      token: ''

    reset = () ->
      data.username = ''
      data.token = ''
      Notifications.resetAll()

    login = (username, password) ->
      reset()

      $http(
        method: 'POST'
        url: '/api/auth'
        data: {username: username, password: password}
        headers: { 'Accept': 'application/json' }
      )
        .success (payload, status, headers, config) ->
          console.log payload
          console.log status

          if payload.token == null
            Notifications.error(
              'Error, authentication failed'
            )
          else
            if status == 201
              data.username = payload.username
              data.token = payload.token
              Notifications.success('Logged in as ' + data.username)
            else
              Notifications.error(
                'Error, authentication failed'
              )

        .error (payload, status, headers, config) ->
          console.log 'ERROR'
          console.log status
          Notifications.error(
            'Error, authentication failed'
          )

    return {
      data: data
      login: login
    }

