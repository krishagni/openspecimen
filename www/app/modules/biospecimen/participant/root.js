
angular.module('os.biospecimen.participant.root', ['os.biospecimen.models'])
  .controller('ParticipantRootCtrl', function(
    $scope, $timeout, cpr, hasSde, hasDict, sysDict, cpDict,
    lookupFieldsCfg, headers, participantSpmnsViewState, aliquotQtyReq,
    pendingSpmnsDispInterval, barcodingEnabled, spmnBarcodesAutoGen,
    additionalLabelAutoGen, ParticipantSpecimensViewState, AuthorizationService, Specimen) {

    function init() {
      $scope.cpr = $scope.object = cpr;
      $scope.entityType = 'Participant';
      $scope.extnState = 'participant-detail.extensions.';

      $scope.fieldsCtx = {
        hasSde: hasSde, hasDict: hasDict,
        sysDict: sysDict, cpDict: cpDict,
        lookupFields: lookupFieldsCfg.fields,
        headers: headers
      };

      $scope.pendingSpmnsDispInterval = +pendingSpmnsDispInterval.value;
      $scope.barcodingEnabled = barcodingEnabled;
      $scope.spmnBarcodesAutoGen = spmnBarcodesAutoGen;
      $scope.additionalLabelAutoGen = additionalLabelAutoGen;
      $scope.aliquotQtyReq = aliquotQtyReq;

      initAuthorizationOpts();

      $scope.rootCtx = {
        participantSpmnsViewState: participantSpmnsViewState,
        showTree: ParticipantSpecimensViewState.showTree
      };

      $scope.$on('participantSpecimensUpdated',
        function(e, data) {
          participantSpmnsViewState.specimensUpdated();

          data = data || {};
          if (data.inline == true && $scope.rootCtx.showTree) {
            $scope.rootCtx.showTree = false;
            $timeout(function() { $scope.rootCtx.showTree = true; });
          }
        }
      );
    }

    function initAuthorizationOpts() {
      var sites = $scope.cp.cpSites.map(function(cpSite) {
        return cpSite.siteName;
      });

      if ($scope.global.appProps.mrn_restriction_enabled) {
        sites = sites.concat($scope.cpr.getMrnSites());
      }

      // Participant Authorization Options
      $scope.participantResource = {
        updateOpts: {
          resource: 'ParticipantPhi',
          operations: ['Update'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        deleteOpts: {
          resource: 'ParticipantPhi',
          operations: ['Delete'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        }
      };

      $scope.visitResource = {
        updateOpts: {
          resource: 'Visit',
          operations: ['Update'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        deleteOpts: {
          resource: 'Visit',
          operations: ['Delete'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        }
      };

      // Specimen Authorization Options
      $scope.specimenResource = {
        allReadOpts: {
          resources: ['Specimen'],
          operations: ['Read'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        allUpdateOpts: {
          resources: ['Specimen'],
          operations: ['Update'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        updateOpts: {
          resources: ['Specimen', 'PrimarySpecimen'],
          operations: ['Update'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        deleteOpts: {
          resources: ['Specimen', 'PrimarySpecimen'],
          operations: ['Delete'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        }
      }

      // Specimen Tree Authorization Options
      var update = AuthorizationService.isAllowed($scope.specimenResource.updateOpts);
      var allUpdate = AuthorizationService.isAllowed($scope.specimenResource.allUpdateOpts);
      var del = AuthorizationService.isAllowed($scope.specimenResource.deleteOpts);
      var store = AuthorizationService.isAllowed({sites: sites, resource: 'StorageContainer', operations: ['Read']});
      $scope.specimenAllowedOps = {allUpdate: allUpdate, update: update, delete: del, store: store};

      // Surgical Pathology Report Authorization Options
      $scope.sprResource = {
        readOpts: {
          resource: 'SurgicalPathologyReport',
          operations: ['Read'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        updateOpts: {
          resource: 'SurgicalPathologyReport',
          operations: ['Update'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        deleteOpts: {
          resource: 'SurgicalPathologyReport',
          operations: ['Delete'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        lockOpts: {
          resource: 'SurgicalPathologyReport',
          operations: ['Lock'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        },

        unlockOpts: {
          resource: 'SurgicalPathologyReport',
          operations: ['Unlock'],
          cp: $scope.cpr.cpShortTitle,
          sites: sites
        }
      }
    }

    $scope.toggleTree = function() {
      $scope.rootCtx.showTree = !$scope.rootCtx.showTree;
      ParticipantSpecimensViewState.showTree = $scope.rootCtx.showTree;
      $scope.$broadcast('osToggleParticipantSpecimenTree', {show: $scope.rootCtx.showTree});
    }

    init();
  });
