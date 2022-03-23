angular.module('os.biospecimen.participant')
  .controller('BulkEditParticipantsCtrl', function(
      $scope, $timeout, $parse, hasSde, cpDict, cp, customFields,
      CollectionProtocolRegistration, ParticipantsHolder, Util) {

      var EXCLUSION_LIST = [
        'cpr.ppid',
        'cpr.participant.firstName',
        'cpr.participant.lastName',
        'cpr.participant.middleName',
        'cpr.participant.emailAddress',
        'cpr.participant.birthDate',
        'cpr.participant.deathDate',
        'cpr.participant.empi',
        'cpr.participant.uid',
        'cpr.participant.pmis'
      ];

      var cprIds, ctx;

      function init() {
        var cprFields = [];
        angular.forEach(cpDict.fields.concat(customFields),
          function(field) {
            if (field.name.indexOf('cpr') != 0 ||              // not participant field
                EXCLUSION_LIST.indexOf(field.name) != -1) {    // excluded field
              return;
            }

            cprFields.push(field);
          }
        );

        ctx = $scope.ctx = {
          editedFields: [
            {
              obj: {cp: cp, cpr: {}},
              opts: {}
            }
          ],
          fields: cprFields
        };

        cprIds = (ParticipantsHolder.getParticipants() || []).map(function(cpr) { return cpr.id; });
        ParticipantsHolder.setParticipants(null);
      }

      function updateParticipants(toSave) {
        CollectionProtocolRegistration.bulkEdit(toSave).then(
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
            ctx.editedFields[idx] = {field: field, obj: {cp: cp, cpr: {}}, opts: {}};
            $parse(field.name).assign(ctx.editedFields[idx].obj, null);

            var len = ctx.editedFields.length;
            if (ctx.editedFields[len - 1].field) {
              ctx.editedFields.push({field: undefined, obj: {cp: cp, cpr: {}}, opts: {}});
            }
          },
          0
        );
      }

      $scope.removeField = function(idx) {
        ctx.editedFields.splice(idx, 1);
      }

      $scope.update = function() {
        var toSave = {};

        if (ctx.fields.length > 0) {
          var edited = false;
          angular.forEach(ctx.editedFields,
            function(editedField) {
              if (!editedField.field || !editedField.field.name) {
                return;
              }

              edited = true;
              Util.merge(editedField.obj.cpr, toSave, true);
            }
          );

          if (!edited) {
            return;
          }

          toSave = {detail: toSave, ids: cprIds};
          if (hasBlankValues(toSave.detail)) {
            Util.showConfirm({
              title: 'common.clear_field_values_q',
              confirmMsg: 'common.confirm_clear_field_values',
              ok: function() {
                updateParticipants(toSave);
              }
            });
          } else {
            updateParticipants(toSave);
          }
        } else {
          toSave = {detail: $scope.cpr, ids: cprIds};
          updateParticipants(toSave);
        }
      }

      init();
    }
  )
