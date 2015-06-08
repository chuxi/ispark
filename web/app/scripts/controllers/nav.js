'use strict';

/**
 * @ngdoc function
 * @name webApp.controller:NavCtrl
 * @description
 * # NavCtrl
 * Controller of the webApp
 */
angular.module('webApp')
  .controller('NavCtrl', function ($scope, $rootScope, $routeParams, $http) {
    $scope.notes = ['note1', 'note2'];

    $scope.createNewNote = function() {

      $http.get(getRestApiBase() + '/hello')
        .success(function(data) {
          console.log(data);
        });



      //$rootScope.$emit('sendNewEvent', {op: 'NEW_NOTE'})
    }

  });
