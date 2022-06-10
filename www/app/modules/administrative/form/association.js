angular.module('os.administrative.form.formctxts', ['os.administrative.models'])
  .controller('FormCtxtsCtrl', function(
    $scope, $modalInstance, $translate,
    args, cpList, entities, currentUser, Alerts, Institute) {

    var reload = false;

    function init() {
      $scope.view = 'show_contexts';
      $scope.extnEntities = entities.filter(
        function(e) {
          return e.allowEdits !== false && (
            currentUser.admin ||
            (e.name == 'User' && currentUser.instituteAdmin) ||
            !e.allCps
          );
        }
      );

      $scope.form = args.form;
      $scope.cpList = cpList;
      $scope.institutes = [];

      var cpLevels = [
        'Participant', 'ParticipantExtension', 'SpecimenCollectionGroup', 'VisitExtension',
        'Specimen', 'SpecimenExtension', 'SpecimenEvent'
      ]

      var formCtxts = $scope.cpFormCtxts = args.formCtxts;
      var instituteIds = [];
      angular.forEach(formCtxts,
        function(fc) {
          var cpLevel = cpLevels.indexOf(fc.level) != -1;
          if (cpLevel && (!fc.collectionProtocol.id || fc.collectionProtocol.id == -1)) {
            fc.collectionProtocol.shortTitle = $translate.instant('form.all');
          } else if (!cpLevel) {
            fc.collectionProtocol.shortTitle = $translate.instant('form.na');
          }

          if ((fc.level == 'User' || fc.level == 'UserProfile') &&
              fc.entityId > 0 && instituteIds.indexOf(fc.entityId) == -1) {
            instituteIds.push(fc.entityId);
          }

          for (var i = 0; i < entities.length; i++) {
            var entityType = fc.level
            if (entityType.lastIndexOf('-') != -1) {
              entityType = entityType.substring(0, entityType.lastIndexOf('-'));
            }

            var entity = entities[i];
            if (entity.name == entityType) {
              fc.level = entity;
              break;
            }
          }
        }
      );

      if (instituteIds.length > 0) {
        Institute.query({id: instituteIds, maxResults: 10000}).then(
          function(institutes) {
            var institutesMap = {};
            angular.forEach(institutes,
              function(institute) {
                institutesMap[institute.id] = institute.name;
              }
            );

            angular.forEach(formCtxts,
              function(fc) {
                if (fc.level.name == 'User') {
                  fc.entityName = institutesMap[fc.entityId];
                }
              }
            );
          }
        );
      }

      $scope.cpFormCtxt = {
        allProtocols: false,
        isMultiRecord: false,
        selectedCps: [],
        selectedEntity: undefined
      }

      $scope.revisions = undefined;
    }


    $scope.loadInstitutes = function(searchTerm) {
      Institute.query({name: searchTerm}).then(
        function(institutes) {
          $scope.institutes = institutes;
        }
      );
    }

    $scope.enableAttach = function(formCtxt) {
      if (!formCtxt.selectedEntity) {
        return false;
      }

      if (formCtxt.selectedEntity.allCps) {
        return true;
      }

      return formCtxt.allProtocols || (formCtxt.selectedCps && formCtxt.selectedCps.length > 0);
    }

    $scope.attach = function(formCtxt) {
      var cpIds = [];
      var entityIds = [];
      if (formCtxt.allProtocols || formCtxt.selectedEntity.allCps) {
        if (formCtxt.selectedEntity.name == 'User' || formCtxt.selectedEntity.name == 'UserProfile') {
          if (!currentUser.admin) {
            entityIds = [currentUser.instituteId];
          } else if (formCtxt.allInstitutes) {
            entityIds = [-1];
          } else {
            for (var i = 0; i < formCtxt.institutes.length; ++i) {
              entityIds.push(formCtxt.institutes[i].id);
            }
          }
        } else {
          cpIds = [-1];
        }
      } else {
        for (var i = 0; i < formCtxt.selectedCps.length; ++i) {
          cpIds.push(formCtxt.selectedCps[i].id);
        }
      }

      var multipleRecs = formCtxt.selectedEntity.multipleRecs;
      if (multipleRecs != false) {
        multipleRecs = formCtxt.isMultiRecord;
      }

      var toSave = {
        form: $scope.form,
        cpIds: cpIds,
        entityIds: entityIds,
        entity: formCtxt.selectedEntity.name,
        isMultiRecord: multipleRecs,
        notifEnabled: formCtxt.notifEnabled,
        dataInNotif: formCtxt.dataInNotif,
        notifUserGroups: formCtxt.notifUserGroups
      }

      toSave = $scope.form.newFormContext(toSave);
      toSave.$saveOrUpdate().then(
        function(data) {
          Alerts.success("form.attached");
          $modalInstance.close(true);
        }
      );
    }

    $scope.confirmRemoveCtx = function(formCtx, $index) {
      $scope.view = 'confirm_remove';
      $scope.removeCtxData = {ctx: formCtx, idx: $index};
    };

    $scope.removeCtx = function() {
      var cpId = $scope.removeCtxData.ctx.collectionProtocol.id || -1;
      var entityId = $scope.removeCtxData.ctx.entityId;
      var entity = $scope.removeCtxData.ctx.level;
      var formContext = $scope.form.newFormContext({
        form: $scope.form,
        cpId: cpId,
        entityId: entityId,
        entityType: entity.name
      });

      formContext.$remove().then(
        function() {
          $scope.cpFormCtxts.splice($scope.removeCtxData.idx, 1);
          $scope.view = 'show_contexts';
          Alerts.success("form.association_deleted", $scope.removeCtxData.ctx);
          $scope.removeCtxData = {};
          $scope.revisions = undefined;
          reload = true;
        }
      );
    };

    $scope.cancelRemoveCtx = function() {
      $scope.view = 'show_contexts';
      $scope.removeCtxData = {};
    };

    $scope.showEditCtx = function(formCtx, $index) {
      $scope.view = 'edit_context';
      $scope.editCtxData = {ctx: angular.copy(formCtx), idx: $index};
    }

    $scope.editCtx = function() {
      var ctx = $scope.editCtxData.ctx;
      var cpIds = [];
      var entityIds = [];
      if (ctx.level.name == 'User' || ctx.level.name == 'UserProfile') {
        entityIds = [ctx.entityId || - 1];
      } else {
        cpIds = [ctx.collectionProtocol.id || -1];
      }

      var fc = $scope.form.newFormContext({
        form: $scope.form,
        cpIds: cpIds,
        entity: ctx.level.name,
        entityIds: entityIds,
        isMultiRecord: ctx.multiRecord,
        notifEnabled: ctx.notifEnabled,
        dataInNotif: ctx.dataInNotif,
        notifUserGroups: ctx.notifUserGroups
      });

      fc.$saveOrUpdate().then(
        function(data) {
          var cpFc = $scope.cpFormCtxts[$scope.editCtxData.idx];
          cpFc.multiRecord =  data[0].multiRecord;
          cpFc.notifEnabled = data[0].notifEnabled;
          cpFc.dataInNotif = data[0].dataInNotif;
          cpFc.notifUserGroups = data[0].notifUserGroups;

          $scope.cancelEditCtx();
          $scope.revisions = undefined;
        }
      );
    }

    $scope.cancelEditCtx = function() {
      $scope.view = 'show_contexts';
      $scope.editCtxData = {};
    }

    $scope.cancel = function() {
      $modalInstance.close(reload);
    }

    $scope.loadRevisions = function() {
      $scope.cpFormCtxt = {
        allProtocols: false,
        isMultiRecord: false,
        selectedCps: [],
        selectedEntity: undefined
      }

      if ($scope.revisions) {
        return;
      }

      $scope.form.getContextRevisions().then(
        function(revisions) {
          $scope.revisions = revisions;
          for (var i = 0; i < revisions.length; ++i) {
            var revision = revisions[i];
            var entityType = revision.entityType;
            if (entityType.lastIndexOf('-') != -1) {
              entityType = entityType.substring(0, entityType.lastIndexOf('-'));
            }

            for (var j = 0; j < entities.length; ++j) {
              var entity = entities[j];
              if (entity.name == entityType) {
                revision.entity = entity;
                break;
              }
            }
          }
        }
      );
    }

    init();
  });
