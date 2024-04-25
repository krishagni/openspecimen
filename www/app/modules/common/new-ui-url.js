
angular.module('os.common')
  .directive('osNewUiUrl', function($parse, VueApp) {
    return {
      restrict: 'A',
      
      link: function(scope, element, attrs) {
        var urlObj = $parse(attrs.osNewUiUrl)(scope);
        var url = urlObj.url;
        angular.forEach(urlObj.params,
          function(value, key) {
            url = url.replaceAll(':' + key, value);
          }
        );

        element.attr('href', VueApp.getVueViewUrl(url, urlObj.query));
      }
    }
  });
