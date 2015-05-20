'use strict';

describe('Controller: InterpreterCtrl', function () {

  // load the controller's module
  beforeEach(module('webApp'));

  var InterpreterCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    InterpreterCtrl = $controller('InterpreterCtrl', {
      $scope: scope
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(scope.awesomeThings.length).toBe(3);
  });
});
