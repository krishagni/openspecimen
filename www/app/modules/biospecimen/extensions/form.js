
angular.module('os.biospecimen.extensions', ['os.biospecimen.models'])
  .directive('osDeForm', function(
    $http, $rootScope, $parse, Form, ApiUrls, ExtensionsUtil,
    LocationChangeListener, SettingUtil) {

    return {
      restrict: 'A',
      controller: function() {
        this.form = null;

        this.validate = function() {
          return this.form.validate();
        }

        this.evaluateSkipLogic = function() {
          if (this.form) {
            this.form.evaluateSkipLogic();
          }
        }

        this.getFormData = function() {
          var attrValues = [];
          angular.forEach(this.form.getValue(), function(value, key) {
            attrValues.push({name: key, value: value});
          });

          return {attrs: attrValues};
        }
      },

      link: function(scope, element, attrs, ctrl) {
        var parentForm = attrs.parentForm && scope[attrs.parentForm];
        if (parentForm) {
          parentForm.osExtnCtrls = parentForm.osExtnCtrls || [];
          parentForm.osExtnCtrls.push(ctrl);
        }

        if (!!attrs.ctrl) {
          var parts = attrs.ctrl.split("\.")
          var obj = scope;
          angular.forEach(parts, function(part) {
            obj = obj[part];
          });
          obj.ctrl = ctrl;
        }

        var onceRendered = false;
        scope.$watch(attrs.opts, function(opts, oldVal) {
          if (!opts || onceRendered) {
            return;
          }

          var baseUrl = Form.url();
          var filesUrl = ApiUrls.getBaseUrl() + 'form-files';
          var hdrs = {
            'X-OS-API-TOKEN': $http.defaults.headers.common['X-OS-API-TOKEN'],
            'X-OS-SURVEY-TOKEN': $http.defaults.headers.common['X-OS-SURVEY-TOKEN']
          };

          var args = {
            id             : opts.formId,
            formDiv        : element,
            formDef        : opts.formDef,
            formDefUrl     : baseUrl + '/:formId/definition?maxPvs=100',
            formDataUrl    : baseUrl + '/:formId/data/:recordId',
            formSaveUrl    : baseUrl + '/:formId/data',
            formPvUrl      : baseUrl + '/:formId/permissible-values',
            fileUploadUrl  : opts.filesUploadUrl || filesUrl,
            fileDownloadUrl: ExtensionsUtil.getFileDownloadUrl,
            formData       : opts.formData,
            recordId       : opts.recordId,
            dateFormat     : $rootScope.global.queryDateFmt.format,
            appData        : {formCtxtId: opts.formCtxtId, objectId: opts.objectId},
            onSaveSuccess  : opts.onSave,
            onSaveError    : opts.onError,
            onValidationError: opts.onValidationError,
            onCancel       : opts.onCancel,
            onPrint        : opts.onPrint,
            onDelete       : opts.onDelete,
            showActionBtns : opts.showActionBtns,
            showSaveNext   : opts.showSaveNext,
            showSaveDraft  : opts.showSaveDraft,
            showPanel      : opts.showPanel,
            customHdrs     : hdrs,
            skipLogicFieldValue   : opts.skipLogicFieldValue ||
              function(attrName) {
                return ctrl.extendedObj ? ctrl.extendedObj.attrValue(attrName) : undefined
              },
            disableFields  : opts.disableFields || []
          };

          args.allowHtmlCaptions = ui.os.appProps.allowHtmlMarkup;
          ctrl.form = new edu.common.de.Form(args);
          ctrl.form.render();
          LocationChangeListener.preventChange();
          addWatchForDomChanges(opts);

          onceRendered = true;
        }, true);

        if (attrs.extendedObj) {
          ctrl.extendedObj = $parse(attrs.extendedObj)(scope);
          scope.$watch(attrs.extendedObj,
            function(extendedObj) {
              ctrl.extendedObj = extendedObj;
              ctrl.evaluateSkipLogic();
            },
            true
          );
        }

        function addWatchForDomChanges(opts) {
          if (opts.labelAlignment == 'horizontal') {
            var domChange = scope.$watch(
              function() {
                return element.children().length;
              },

              function(length) {
                if (length > 0) {
                  domChange(); // kill the watch
                  alignLabelsHorizontally();
                }
              }
            );
          }
        }

        function alignLabelsHorizontally() {
          element.find(".col-xs-8").each(
            function(index) {
              angular.element(this).removeClass("col-xs-8").addClass("col-xs-12");
            }
          );

          element.find('label.control-label').each(
            function(index) {
              angular.element(this).addClass("col-xs-3");
              angular.element(this).siblings().wrapAll("<div class='col-xs-6'></div>");
            }
          );

          element.find('.de-note').each(
            function() {
              angular.element(this).addClass("col-xs-offset-3 col-xs-8");
            }
          );

          element.find('.de-sf-content').closest('.col-xs-6')
            .removeClass('col-xs-6').addClass('col-xs-9')
            .css({'padding-left': '30px'});
        }
      }
    }
  });
