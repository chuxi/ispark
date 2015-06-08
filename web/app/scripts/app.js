'use strict';

/**
 * get the websocket info
 */
function getPort() {
  var port = Number(location.port);
  if (location.protocol !== 'https:' && (port === undefined || port === 0)) {
    port = 80;
  } else if (location.protocol === 'https:' && (port === undefined || port === 0)) {
    port = 443;
  } else if (port === 3333 || port === 9000) {
    port = 8080;
  }
  return port + 1;
}

function getWebsocketProtocol() {
  var protocol = 'ws';
  if (location.protocol === 'https:') {
    protocol = 'wss';
  }
  return protocol;
}

function getRestApiBase() {
  var port = Number(location.port);
  if (port === 'undefined' || port === 0) {
    port = 80;
    if (location.protocol === 'https:') {
      port = 443;
    }
  }

  if (port === 3333 || port === 9000) {
    port = 8080;
  }
  return location.protocol+'//'+location.hostname+':'+port;
}


/**
 * @ngdoc overview
 * @name webApp
 * @description
 * # webApp
 *
 * Main module of the application.
 */
angular
  .module('webApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'ngWebSocket',
    'ui.ace'
  ])
  .factory('WebSocket', function ($websocket) {
    return $websocket(getWebsocketProtocol() + '://' + location.hostname + ':' + getPort())
  })
  .config(function ($routeProvider) {

    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .when('/about', {
        templateUrl: 'views/about.html',
        controller: 'AboutCtrl'
      })
      //.when('/interpreter', {
      //  templateUrl: 'views/interpreter.html',
      //  controller: 'InterpreterCtrl'
      //})
      //.when('/notebook/:noteId', {
      //  templateUrl: 'views/notebooks.html',
      //  controller: 'NotebookCtrl'
      //})
      //.when('/notebook/:noteId/paragraph/:paragraphId?', {
      //  templateUrl: 'views/notebooks.html',
      //  controller: 'NotebookCtrl'
      //})
      .otherwise({
        redirectTo: '/'
      });
  });
