angular.module('os.biospecimen.specimen')
  .controller('BulkEditSpecimensCtrl', function(
      $scope, $timeout, $parse, hasSde, cpDict, customFields,
      Specimen, SpecimensHolder, PvManager, Util, Alerts) {

      var EXCLUSION_LIST = [
        'specimen.label',
        'specimen.barcode',
        'specimen.storageLocation',
        'specimen.lineage',
        'specimen.parentLabel'
      ];

      var spmnIds, ctx;

      function init() {
        $scope.specimen = new Specimen();

        var spmnFields = [];
        angular.forEach(cpDict.fields.concat(customFields),
          function(field) {
            if (field.name.indexOf('specimen') != 0 ||         // not specimen field
                field.name.indexOf('specimen.events') == 0 ||  // event field
                EXCLUSION_LIST.indexOf(field.name) != -1) {    // excluded field
              return;
            }

            spmnFields.push(field);
          }
        );

        ctx = $scope.ctx = {
          editedFields: [
            {
              obj: {specimen: {}},
              opts: {}
            }
          ],
          fields: spmnFields
        };

        spmnIds = (SpecimensHolder.getSpecimens() || []).map(function(spmn) { return spmn.id; });
        if (spmnIds.length > 100) {
          Alerts.error('specimens.edit_limit_maxed', {count: spmns.length});
          $scope.back();
          return;
        }

        SpecimensHolder.setSpecimens(null);

        if (spmnFields.length == 0) {
          loadPvs();
        }
      }

      function loadPvs() {
        $scope.biohazards = PvManager.getPvs('specimen-biohazard');
        $scope.specimenStatuses = PvManager.getPvs('specimen-status');
      }

      function updateSpecimens(toSave) {
        Specimen.bulkEdit(toSave).then(
          function(result) {
            $scope.back();
          }
        );
      };

      function hasBlankValues(obj) {
        return Object.keys(obj).some(
          function(key) {
            if (obj[key] == null) {
              return true;
            } else if (typeof obj[key] == 'object') {
              return hasBlankValues(obj[key]);
            }

            return false;
          }
        );
      }

      $scope.onFieldSelect = function(idx, field) {
        if (!field) {
          $scope.removeField(idx);
          return;
        }

        ctx.editedFields[idx].field = undefined;
        $timeout(
          function() {
            ctx.editedFields[idx].field = field
            $parse(field.name).assign(ctx.editedFields[idx].obj, null);

            var len = ctx.editedFields.length;
            if (ctx.editedFields[len - 1].field) {
              ctx.editedFields.push({field: undefined, obj: {specimen: {}}, opts: {}});
            }
          },
          0
        );
      }

      $scope.removeField = function(idx) {
        ctx.editedFields.splice(idx, 1);
      }

      $scope.bulkUpdate = function() {
        var toSave = {};

        if (ctx.fields.length > 0) {
          var edited = false;
          angular.forEach(ctx.editedFields,
            function(editedField) {
              if (!editedField.field || !editedField.field.name) {
                return;
              }

              edited = true;
              Util.merge(editedField.obj.specimen, toSave, true);
            }
          );

          if (!edited) {
            return;
          }

          toSave = {detail: toSave, ids: spmnIds};
          if (hasBlankValues(toSave.detail)) {
            Util.showConfirm({
              title: 'common.clear_field_values_q',
              confirmMsg: 'common.confirm_clear_field_values',
              ok: function() {
                updateSpecimens(toSave);
              }
            });
          } else {
            updateSpecimens(toSave);
          }
        } else {
          toSave = {detail: $scope.specimen, ids: spmnIds};
          updateSpecimens(toSave);
        }

      }

      init();
    }
  )
