angular.module('openspecimen')
  .factory('LocationChangeListener', function($window, $injector, $translate, $timeout, $rootScope) {
    var isLocationChangeAllowed = true;

    function init() {
      $window.onbeforeunload = function() {
        if (!isLocationChangeAllowed) {
          return $translate.instant('common.confirm_navigation');
        }
      }
    }

    function allowChange() {
      isLocationChangeAllowed = true;
    }

    function preventChange() {
      isLocationChangeAllowed = false;
    }

    function onChange(event) {
      if (!isLocationChangeAllowed && !confirm($translate.instant('common.confirm_navigation'))) {
        event.preventDefault();
      } else {
        allowChange();
      }
    }

    function currentState() {
      var st  = $injector.get('$state');
      var stp = $injector.get('$stateParams');

      var sname = (st.current || {}).name;
      var params = '';
      angular.forEach(stp,
        function(value, key) {
          if (value) {
            params += key + '=' + value;
          }
        }
      );

      return sname + '#' + params;
    }

    function back() {
      allowChange();

      var sci = $rootScope.stateChangeInfo;
      var fromState = sci && sci.fromState;
      if (fromState && fromState.name && fromState.data && fromState.data.vueView) {
        $injector.get('$state').go(sci.fromState.name, sci.fromParams);
      } else if ($window.history.length == 1) {
        $injector.get('$state').go('home');
      } else {
        $window.history.back();
      }
    }

    init();

    return {
      allowChange: allowChange,

      preventChange: preventChange,

      onChange: onChange,

      back: back
    };
  })
