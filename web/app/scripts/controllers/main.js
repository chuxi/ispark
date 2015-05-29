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

    $scope.WebSocketWaitingList = [];

    var send = function(data) {
      if (WebSocket.currentState() !== 'OPEN') {
        $scope.WebSocketWaitingList.push(data);
      } else {
        console.log('Send >> %o, %o', data.op, data);
        WebSocket.send(JSON.stringify(data));
      }
    };

    $rootScope.$on('sendNewEvent', function(event, data) {
      if (!event.defaultPrevented) {
        send(data);
        event.preventDefault();
      }
    });



  });
