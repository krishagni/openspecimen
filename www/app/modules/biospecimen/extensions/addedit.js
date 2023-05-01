
angular.module('os.biospecimen.extensions.addedit-record', [])
  .controller('FormRecordAddEditCtrl', function(
    $scope, $state, $stateParams, $timeout,
    forms, records, formDef, postSaveFilters, viewOpts,
    LocationChangeListener, ExtensionsUtil, Alerts) {

    function init() {
      var recId = $stateParams.recordId;
      if (!!recId) {
        recId = parseInt(recId);
      }

      var nextForm = undefined;
      if (viewOpts.showSaveNext) {
        nextForm = getNextForm();
      }

      $scope.formOpts = {
        formId: $stateParams.formId,
        formDef: formDef,
        recordId: recId,
        formCtxtId: parseInt($stateParams.formCtxId),
        objectId: $scope.object.id,
        showActionBtns: viewOpts.showActionBtns == false ? false : true,
        showSaveNext: viewOpts.showSaveNext && !!nextForm,
        showSaveDraft: true,
        disableFields: viewOpts.disabledFields || [],

        onValidationError: function() {
          $timeout(function() { Alerts.error('common.form_validation_error'); }, 0);
        },

        onSave: function(formData, next) {
          angular.forEach(postSaveFilters, function(filter) {
            filter($scope.object, formDef.name, formData);
          });

          Alerts.success("extensions.record_saved");
          if (next && nextForm) {
            var params = angular.extend({}, $stateParams);
            params.formCtxId = nextForm.formCtxtId;
            params.formId = nextForm.formId;
            params.recordId = undefined;
            LocationChangeListener.allowChange();
            $state.go($state.current.name, params);
          } else {
            gotoRecsList(formData);
          }
        },

        onError: function(data) {
          //
          // timeout is mostly to trigger a digest cycle as onError is invoked
          // from a non-angular context by DE framework
          //
          $timeout(function() {
            if (data.responseJSON instanceof Array) {
              Alerts.errorText(data.responseJSON.map(function(err) { return err.message + " (" + err.code + ")"; }))
            } else {
              Alerts.errorText("Unknown error: " + data.responseText);
            }
          });
        },

        onCancel: function() {
          gotoRecsList();
        },

        onPrint: function(html) {
          alert(html);
        },

        onDelete: function() {
          var record = {recordId: recId, formId: $stateParams.formId, formCaption: formDef.caption}
          ExtensionsUtil.deleteRecord(record, gotoRecsList);
        }
      };
    }

    function getNextForm() {
      var nextForm = undefined;
      var anyForm = false;
      for (var i = 0; i < forms.length - 1; ++i) {
        var f = forms[i], nf = forms[i + 1];
        if (!anyForm && f.formId == $stateParams.formId) {
          anyForm = true;
        }

        if (anyForm && !nf.sysForm && (nf.multiRecord || nf.records.length == 0)) {
          nextForm = nf;
          break;
        }
      }

      return nextForm;
    }

    function gotoRecsList(savedFormData) {
      if (typeof viewOpts.goBackFn == 'function') {
        viewOpts.goBackFn(savedFormData);
        return;
      }

      reloadRecs().then(
        function() {
          LocationChangeListener.allowChange();
          var params = {formId: $stateParams.formId, formCtxtId: $stateParams.formCtxId, recordId: null}
          $state.go($scope.extnState + 'list', params);
        }
      );
    }

    function reloadRecs() {
      records.length = 0;
      return $scope.object.getRecords().then(
        function(dbRecs) {
          Array.prototype.push.apply(records, dbRecs);
          ExtensionsUtil.linkFormRecords(forms, records);
          return dbRecs;
        }
      );
    }

    init();
  });
