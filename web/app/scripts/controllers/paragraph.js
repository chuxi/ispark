'use strict';

/**
 * @ngdoc function
 * @name webApp.controller:ParagraphCtrl
 * @description
 * # ParagraphCtrl
 * Controller of the webApp
 */
angular.module('webApp')
  .controller('ParagraphCtrl', function ($scope) {
    $scope.awesomeThings = [
      'HTML5 Boilerplate',
      'AngularJS',
      'Karma'
    ];
  });
