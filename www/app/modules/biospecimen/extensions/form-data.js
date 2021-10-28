
angular.module('os.biospecimen.extensions')
  .directive('osFormData', function(ApiUrls, ExtensionsUtil, Util) {
    return {
      restrict: 'E',

      template: '<div ng-include src="tmplUrl"></div>',

      scope: {
        data: '='
      },

      link: function(scope, element, attrs) {
        scope.filesUrl = ApiUrls.getBaseUrl() + 'form-files';
        scope.tmplUrl = 'modules/biospecimen/extensions/form-data.html';

        var customTmpl = ExtensionsUtil.getViewTmpl(scope.data.name);
        if (customTmpl) {
          scope.tmplUrl = customTmpl;
        }

        var longerCaptionFields  = 0, totalFields = 0;
        angular.forEach(scope.data.fields,
          function(field) {
            if (field.type == 'subForm') {
              angular.forEach(field.value,
                function(sfData) {
                  angular.forEach(sfData.fields,
                    function(sfField) {
                      if (sfField.type == 'textArea' || sfField.type == 'stringTextField') {
                        sfField.displayValue = Util.linkify(sfField.value);
                      }
                    }
                  );
                }
              );

              return;
            }

            if (field.caption && field.caption.length >= 30) {
              ++longerCaptionFields;
            }

            if (field.type == 'textArea' || field.type == 'stringTextField') {
              field.displayValue = Util.linkify(field.value);
            }

            ++totalFields;
          }
        );

        scope.verticalLayout = (longerCaptionFields * 100 / totalFields >= 30);
      }
    };
  });
