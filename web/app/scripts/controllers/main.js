'use strict';

/**
 * @ngdoc function
 * @name webApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the webApp
 */
angular.module('webApp')
  .controller('MainCtrl', function ($scope, $rootScope, WebSocket, $window) {



    var send = function(data) {

    };

    $rootScope.$on('sendNewEvent', function(event, data) {
      if (!event.defaultPrevented) {

      }
    });



  });
