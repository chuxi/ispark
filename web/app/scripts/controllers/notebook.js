'use strict';

/**
 * @ngdoc function
 * @name webApp.controller:NotebookCtrl
 * @description
 * # NotebookCtrl
 * Controller of the webApp
 */
angular.module('webApp')
  .controller('NotebookCtrl', function ($scope) {
    $scope.awesomeThings = [
      'HTML5 Boilerplate',
      'AngularJS',
      'Karma'
    ];
  });
