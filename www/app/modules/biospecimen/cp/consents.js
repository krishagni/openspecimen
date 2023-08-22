
angular.module('os.biospecimen.cp.consents', ['os.biospecimen.models'])
  .controller('CpConsentsCtrl', function($scope, $state, $q, $modal, cp, hasEc, consentTiers, DeleteUtil, Alerts,
    AuthorizationService, ConsentStatement, CollectionProtocol) {

    var defStmts;

    function init() {
      $scope.cp = cp;
      $scope.consentCtx = {
        hasEc: hasEc,
        allowEdit: AuthorizationService.isAllowed($scope.cpResource.updateOpts),
        tiers: initConsentTiers(consentTiers),
        stmts: []
      };

      if (!hasEc) {
        loadConsentStmts();
      }
    }

    function initConsentTiers(consentTiers) {
      angular.forEach(consentTiers, initConsentTier);
      return consentTiers;
    }

    function initConsentTier(consentTier) {
      return addDisplayValue(consentTier, consentTier.statementCode, consentTier.statement);
    }

    function loadConsentStmts(searchString) {
      if (defStmts && !searchString) {
        $scope.consentCtx.stmts = defStmts;
        return;
      }

      if (defStmts && defStmts.length < 100) {
        return;
      }

      ConsentStatement.query({searchString: searchString}).then(
        function(stmts) {
          $scope.consentCtx.stmts = stmts;
          angular.forEach(stmts,
            function(stmt) {
              addDisplayValue(stmt, stmt.code, stmt.statement);
            }
          );

          if (!searchString) {
            defStmts = stmts;
          }
        }
      );
    }

    function addDisplayValue(obj, code, statement) {
      return angular.extend(obj, {itemKey: code, displayValue: statement + ' (' + code + ')'});
    }

    function onAddRemove(consentTier) {
      cp.draftMode = true;
      if (!consentTier) {
        return;
      }

      return initConsentTier(consentTier);
    }

    function onAddRmConsentCp(savedCp) {
      var consentsSource = null;
      if (!savedCp.consentsSource) {
        cp.consentsSource = null;
        consentsSource = cp;
      } else {
        consentsSource = cp.consentsSource = savedCp.consentsSource;
      }

      if (!hasEc) {
        consentsSource.getConsentTiers().then(
          function(tiers) {
            $scope.consentCtx.tiers = initConsentTiers(tiers);
          }
        );
      }

      cp.draftMode = true;
    }

    $scope.loadConsentStmts = loadConsentStmts;

    $scope.listChanged = function(action, stmt) {
      if (action == 'add') {
        return cp.newConsentTier({statementCode: stmt.itemKey}).$saveOrUpdate().then(onAddRemove);
      } else if (action == 'update') {
        return cp.newConsentTier({id: stmt.id, statementCode: stmt.displayValue}).$saveOrUpdate().then(onAddRemove);
      } else if (action == 'remove') {
        var deferred = $q.defer();
        var opts = {
          confirmDelete: 'cp.delete_consent_tier',
          onDeletion: function() { onAddRemove(); deferred.resolve(true); },
          onDeleteFail: function() { deferred.reject(); }
        }

        stmt.cpShortTitle = cp.shortTitle;
        DeleteUtil.delete(stmt, opts);
        return deferred.promise;
      }

      return undefined;
    };

    $scope.updateConsentsWaived = function() {
      $scope.cp.updateConsentsWaived().then(
        function(result) {
          Alerts.success("cp.consents_waived_updated", {waived: result.consentsWaived});
          $scope.cp.draftMode = true;
        }
      );
    }

    $scope.showSelectConsentsCp = function() {
      $modal.open({
        templateUrl: 'modules/biospecimen/cp/set-consents-cp.html',
        controller: function($scope, $modalInstance) {
          var mctx = $scope.mctx = {
            defCps: null,
            cps: [],
            selectedCp: angular.copy(cp.consentsSource || {}),
            cp: cp
          };

          $scope.searchCps = function(query) {
            if (mctx.defCps && (!query || mctx.defCps.length < 100)) {
              mctx.cps = mctx.defCps;
              return;
            }

            CollectionProtocol.query({query: query, onlyParticipantConsentCps: true}).then(
              function(cps) {
                mctx.cps = cps.filter(function(icp) { return icp.id != cp.id; });
                if (!query) {
                  mctx.defCps = mctx.cps;
                }
              }
            );
          }

          $scope.submit = function() {
            $scope.submitting = true;
            cp.updateConsentsSource(mctx.selectedCp || {}).then(
              function(savedCp) {
                $modalInstance.close(savedCp);
              },

              function() {
                $scope.submitting = false;
              }
            );
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('close');
          }
        },
      }).result.then(
        function(savedCp) {
          onAddRmConsentCp(savedCp);
        }
      );
    }

    $scope.resetConsentsCp = function() {
      cp.updateConsentsSource({}).then(
        function(savedCp) {
          onAddRmConsentCp(savedCp);
        }
      );
    }

    init();
  });
