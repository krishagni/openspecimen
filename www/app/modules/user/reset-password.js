angular.module('openspecimen')
  .controller('ResetPasswordCtrl', function($scope, $state, $stateParams, $location, $translate, User, Setting)  {
  
    function init() {
      if (!$location.search().token) {
        $state.go('login');
        return;
      }

      $scope.response = {};
      $scope.passwordDetail = {
        loginName: $stateParams.loginName,
        resetPasswordToken: $location.search().token
      };

      $scope.passwdCtx = {};
      loadPasswdSettings();
    }

    function onResetPassword(result) {
      $scope.response.status = result.status;
      if (result.status == 'ok') {
        $scope.response.message = 'reset_password.password_updated';
      }
    }

    function loadPasswdSettings() {
      Setting.getPasswordSettings().then(
        function(setting) {
          $scope.passwdCtx = setting;
        }
      );
    }

    $scope.resetPassword = function() {
      User.resetPassword($scope.passwordDetail).then(onResetPassword);
    }

    init();
  });
