
angular.module('openspecimen')
  .controller('NewStuffCtrl', function($scope, $timeout, $document, $http, $sce, ApiUrls, User, SettingUtil) {
    var ctx, loginCtx;

    function init() {
      loginCtx = $scope.userCtx; // from the signed-in controller
      ctx = $scope.ctx = { loading: true, notes: null };
      SettingUtil.getSetting('training', 'release_notes').then(
        function(notesLink) {
          ctx.notesLink = notesLink.value;
        }
      );
    }

    function hidePopover() {
      var popups = $document.find('div.popover');
      for (var i = 0; i < popups.length; ++i) {
        var popupEl = angular.element(popups[i]);
        if (popupEl.find('.os-new-stuff').length > 0) {
          popupEl.scope().$hide();
          break;
        }
      }
    }

    $scope.close = function() {
      User.saveUiState({notesRead: ui.os.global.appProps.build_commit_revision}).then(
        function(savedState) {
          angular.extend(loginCtx, {state: savedState, showNewStuff: false});
          hidePopover();
        }
      );
    }

    init();
  });
