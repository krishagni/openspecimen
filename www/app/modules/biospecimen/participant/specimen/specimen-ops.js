angular.module('os.biospecimen.specimen')
  .directive('osSpecimenOps', function(
    $state, $rootScope, $modal, $q, $window, Util, DistributionProtocol, DistributionOrder, Specimen, ExtensionsUtil,
    SpecimensHolder, Alerts, DeleteUtil, SpecimenLabelPrinter, ParticipantSpecimensViewState,
    AuthorizationService, SettingUtil) {

    var SPMN_TBR = 'To be Received';

    function initOpts(scope, element, attrs) {
      scope.title = attrs.title || 'specimens.ops';
      if (scope.cp) {
        SettingUtil.getSetting('biospecimen', 'coordinator_role_name').then(
          function(setting) {
            scope.isCoordinator = (setting.value == AuthorizationService.getRole(scope.cp));
          }
        );
      }

      if (!scope.resourceOpts) {
        var cpShortTitle = scope.cp && scope.cp.shortTitle;
        var sites = undefined;
        if (scope.cp) {
          sites = scope.cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
          if ($rootScope.global.appProps.mrn_restriction_enabled && scope.cpr) {
            sites = sites.concat(scope.cpr.getMrnSites());
          }
        }

        scope.resourceOpts = {
          containerReadOpts: {resource: 'StorageContainer', operations: ['Read']},
          orderCreateOpts: {resource: 'Order', operations: ['Create']},
          shipmentCreateOpts: {resource: 'ShippingAndTracking', operations: ['Create']},
          allSpecimenUpdateOpts: {
            cp: cpShortTitle,
            sites: sites,
            resource: 'Specimen',
            operations: ['Update']
          },
          specimenUpdateOpts: {
            cp: cpShortTitle,
            sites: sites,
            resources: ['Specimen', 'PrimarySpecimen'],
            operations: ['Update']
          },
          specimenDeleteOpts: {
            cp: cpShortTitle,
            sites: sites,
            resources: ['Specimen', 'PrimarySpecimen'],
            operations: ['Delete']
          }
        };
      }

      initAllowSpecimenTransfers(scope);
      initAllowDistribution(scope);
    }

    function initAllowSpecimenTransfers(scope) {
      scope.allowSpmnTransfers = AuthorizationService.isAllowed(scope.resourceOpts.containerReadOpts) &&
        AuthorizationService.isAllowed(scope.resourceOpts.specimenUpdateOpts);
    }

    function initAllowDistribution(scope) {
      if (!AuthorizationService.isAllowed(scope.resourceOpts.orderCreateOpts)) {
        scope.allowDistribution = false;
        return;
      }

      if (!scope.cp) {
        scope.allowDistribution = true;
        return;
      }

      if (!!scope.cp.distributionProtocols && scope.cp.distributionProtocols.length > 0) {
        scope.allowDistribution = true;
      } else {
        DistributionProtocol.getCount({cp: scope.cp.shortTitle, excludeExpiredDps: true}).then(
          function(resp) {
            scope.allowDistribution = (resp.count > 0);
          }
        );
      }
    }

    function getDp(scope, hideDistributeBtn, distList) {
      return $modal.open({
        templateUrl: 'modules/biospecimen/participant/specimen/distribute.html',
        controller: function($scope, $modalInstance) {
          var ctx;

          function init() {
            ctx = $scope.ctx = {
              distList: distList,
              clearListId: scope.cart && scope.cart.id,
              defDps: undefined,
              dps: [],
              dp: undefined,
              hideDistributeBtn: hideDistributeBtn
            };
          }

          function loadDps(searchTerm) {
            var cpShortTitle;
            if (scope.cp) {
              if (scope.cp.distributionProtocols && scope.cp.distributionProtocols.length > 0) {
                ctx.dps = scope.cp.distributionProtocols;
                if (!ctx.defDps) {
                  ctx.defDps = ctx.dps;
                  if (ctx.dps.length == 1) {
                    ctx.dp = ctx.dps[0];
                  }
                }

                return;
              }

              cpShortTitle = scope.cp.shortTitle;
            }

            if (ctx.defDps && (!searchTerm || ctx.defDps.length < 100)) {
              ctx.dps = ctx.defDps;
              return;
            }

            var filterOpts = {activityStatus: 'Active', query: searchTerm, excludeExpiredDps: true, cp: cpShortTitle};
            DistributionProtocol.query(filterOpts).then(
              function(dps) {
                if (!searchTerm && !ctx.defDps) {
                  ctx.defDps = dps;
                  if (dps.length == 1) {
                    ctx.dp = dps[0];
                  }
                }

                ctx.dps = dps;
              }
            );
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }

          $scope.distribute = function() {
            $scope.ctx.distribute = true;
            $modalInstance.close($scope.ctx);
          }

          $scope.editOrder = function() {
            $scope.ctx.distribute = true;
            $scope.ctx.editOrder = true;
            $modalInstance.close($scope.ctx);
          }

          $scope.reserve = function() {
            $modalInstance.close($scope.ctx);
          }

          $scope.loadDps = loadDps;

          init();
        },

        size: 'lg'
      }).result;
    }

    function selectDpAndDistributeSpmns(scope, specimens, hideDistributeBtn, distList) {
      getDp(scope, hideDistributeBtn, distList).then(
        function(details) {
          if (details.distribute) {
            if (specimens) {
              SettingUtil.getSetting('administrative', 'max_order_spmns_ui_limit').then(
                function(setting) {
                  var limit = (setting.value && +setting.value) || 100;
                  var r = specimens.find(function(spmn) { return !spmn.hasOwnProperty('label'); });
                  if (r) { // at least one specimen without label property
                    var spmnIds = specimens.map(function(spmn) {return spmn.id});
                    Specimen.getByIds(spmnIds, false, spmnIds.length > limit).then(
                      function(result) {
                        distributeSpmns(scope, details, result);
                      }
                    );

                    return;
                  }

                  distributeSpmns(scope, details, specimens);
                }
              );

              return;
            }

            distributeSpmns(scope, details, specimens);
          } else {
            reserveSpmns(scope, details, specimens);
          }
        }
      );
    }

    function distributeSpmns(scope, details, specimens) {
      var specimenListId;
      if ((!specimens || specimens.length == 0) && scope.cart && scope.cart.id >= 0) {
        specimenListId = scope.cart.id;
      }

      if (details.editOrder) {
        SpecimensHolder.setSpecimens(specimens, details);
        navTo(
          scope,
          'order-addedit',
          {
            dpId: details.dp.id,
            clearFromCart: details.clearListId,
            clearCart: details.clearListMode,
            specimenListId: specimenListId
          }
        );
        return;
      }

      //
      // direct distribution
      //
      var dp = details.dp;
      new DistributionOrder({
        name: dp.shortTitle + '_' + Util.toBeDateTime(new Date(), true),
        distributionProtocol: dp,
        requester: dp.principalInvestigator,
        siteName: dp.defReceivingSiteName,
        specimenList: {id: specimenListId},
        clearListId: details.clearListId,
        clearListMode: details.clearListMode,
        orderItems: getOrderItems(specimens, details.printLabels),
        comments: details.comments,
        status: 'EXECUTED'
      }).$saveOrUpdate().then(
        function(createdOrder) {
          if (createdOrder.completed) {
            Alerts.success('orders.creation_success', createdOrder);
          } else {
            Alerts.info('orders.more_time');
          }

          ParticipantSpecimensViewState.specimensUpdated(scope, {inline: true});
          scope.initList();
        },

        function(errResp) {
          Util.showErrorMsg(errResp);
        }
      );
    }

    function getOrderItems(specimens, printLabel) {
      return (specimens || []).map(
        function(specimen) {
          return {
            specimen: specimen,
            quantity: specimen.availableQty,
            status: 'DISTRIBUTED_AND_CLOSED',
            printLabel: printLabel
          }
        }
      );
    }

    function reserveSpmns(scope, details, specimens) {
      var request = {
        dpId: details.dp.id,
        comments: details.comments,
        specimens: specimens.map(function(spmn) { return {id: spmn.id }; })
      };

      details.dp.reserveSpecimens(request).then(
        function(resp) {
          Alerts.success('orders.specimens_reserved', {count: resp.updated});
          ParticipantSpecimensViewState.specimensUpdated(scope, {inline: true});
          scope.initList();
        }
      );
    }

    function navTo(scope, toState, toParams) {
      if (scope.beforeNav) {
        scope.beforeNav({navTo: toState});
      }

      $state.go(toState, toParams);
    }

    return {
      restrict: 'E',

      replace: true,

      scope: {
        cp: '=?',
        cpr: '=?',
        specimens: '&',
        initList: '&',
        resourceOpts: '=?',
        cart: '=?',
        beforeNav: '&'
      },

      templateUrl: 'modules/biospecimen/participant/specimen/specimen-ops.html',

      link: function(scope, element, attrs) {
        scope.dropdownRight = attrs.hasOwnProperty('dropdownRight');

        initOpts(scope, element, attrs);

        function gotoView(state, params, msgCode, anyStatus, excludeExtensions, vueView) {
          var selectedSpmns = scope.specimens({anyStatus: anyStatus});
          if (!selectedSpmns || selectedSpmns.length == 0) {
            Alerts.error('specimen_list.' + msgCode);
            return;
          }

          var specimenIds = selectedSpmns.map(function(spmn) {return spmn.id});
          if (vueView) {
            $window.localStorage['selectedSpecimenIds'] = JSON.stringify(specimenIds);
            navTo(scope, state, params);
            return;
          }

          Specimen.getByIds(specimenIds, excludeExtensions != true).then(
            function(spmns) {
              angular.forEach(spmns, function(spmn) { ExtensionsUtil.createExtensionFieldMap(spmn, true); });
              SpecimensHolder.setSpecimens(spmns);
              navTo(scope, state, params);
            }
          );
        }

        function showError(error, spmns) {
          Alerts.error(error, {specimens: spmns.map(function(s) { return !s.label ? s.id : s.label; }).join(', ')});
          return true;
        }

        scope.editSpecimens = function() {
          var spmns = scope.specimens({anyStatus: true});
          if (!spmns || spmns.length == 0) {
            Alerts.error('specimen_list.no_specimens_to_edit');
            return;
          }

          if (spmns.length > 100) {
            Alerts.error('specimens.edit_limit_maxed', {count: spmns.length});
            return;
          }

          SpecimensHolder.setSpecimens(spmns);
          navTo(scope, 'specimen-bulk-edit');
        }

        scope.printSpecimenLabels = function() {
          var spmns = scope.specimens({anyStatus: true});
          if (!spmns || spmns.length == 0) {
            Alerts.error('specimens.no_specimens_for_print');
            return;
          }

          var parts = [Util.formatDate(Date.now(), 'yyyyMMdd_HHmmss')];
          if (scope.cpr) {
            parts.unshift(scope.cpr.ppid);
            parts.unshift(scope.cpr.cpShortTitle);
          } else if (scope.cp) {
            parts.unshift(scope.cp.shortTitle);
          }

          var outputFilename = parts.join('_') + '.csv';
          var specimenIds = spmns.map(function(s) { return s.id; });
          SpecimenLabelPrinter.printLabels({specimenIds: specimenIds}, outputFilename);
        }

        scope.deleteSpecimens = function() {
          var spmns = scope.specimens({anyStatus: true});
          if (!spmns || spmns.length == 0) {
            Alerts.error('specimens.no_specimens_for_delete');
            return;
          }

          var specimenIds = spmns.map(function(spmn) { return spmn.id; });
          var opts = {
            confirmDelete: 'specimens.delete_specimens_heirarchy',
            successMessage: 'specimens.specimens_hierarchy_deleted',
            onBulkDeletion: function() {
              ParticipantSpecimensViewState.specimensUpdated(scope, {inline: true});
              scope.initList();
            },
            askReason: true
          }
          DeleteUtil.bulkDelete({bulkDelete: Specimen.bulkDelete}, specimenIds, opts);
        }

        scope.closeSpecimens = function() {
          var specimensToClose = scope.specimens({anyStatus: false});
          if (specimensToClose.length == 0) {
            Alerts.error('specimens.no_specimens_for_close');
            return;
          }

          $modal.open({
            templateUrl: 'modules/biospecimen/participant/specimen/close.html',
            controller: 'SpecimenCloseCtrl',
            resolve: {
              specimens: function() {
                return specimensToClose;
              }
            }
          }).result.then(
            function() {
              ParticipantSpecimensViewState.specimensUpdated(scope, {inline: true});
              scope.initList();
            }
          );
        };

        scope.distributeSpecimens = function(distAll) {
          var selectedSpmns = undefined;
          if (!scope.cart || !distAll) {
            selectedSpmns = scope.specimens({anyStatus: false});
            if (!selectedSpmns || selectedSpmns.length == 0) {
              Alerts.error('specimen_list.no_specimens_for_distribution');
              return;
            }
          }

          selectDpAndDistributeSpmns(scope, selectedSpmns, false, scope.cart && distAll);
        }

        scope.reserveSpecimens = function() {
          var selectedSpmns = scope.specimens({anyStatus: false});
          if (!selectedSpmns || selectedSpmns.length == 0) {
            Alerts.error('specimen_list.no_specimens_for_reservation');
            return;
          }

          selectDpAndDistributeSpmns(scope, selectedSpmns, true);
        }

        scope.shipSpecimens = function() {
          gotoView('shipment-addedit', {shipmentId: -1}, 'no_specimens_for_shipment', undefined, undefined, true);
        }

        scope.receiveSpecimens = function() {
          var selectedSpmns = scope.specimens();
          if (!selectedSpmns || selectedSpmns.length == 0) {
            $state.go('receive-specimens', {event: 'SpecimenReceivedEvent'});
            return;
          }

          var specimenIds = selectedSpmns.map(function(spmn) {return spmn.id});
          Specimen.getByIds(specimenIds, true).then(
            function(spmns) {
              var nonPrimarySpmns = spmns.filter(function(spmn) { return spmn.lineage != 'New'; });
              if (nonPrimarySpmns.length > 0) {
                return showError('specimens.non_primary_receive_na', nonPrimarySpmns);
              }

              var closedSpmns = spmns.filter(function(spmn) { return spmn.activityStatus != 'Active'; });
              if (closedSpmns.length > 0) {
                return showError('specimens.closed_edit_na', closedSpmns);
              }

              var ncSpmns = spmns.filter(function(spmn) { return spmn.status != 'Collected'; });
              if (ncSpmns.length > 0) {
                return showError('specimens.not_collected', ncSpmns);
              }

              var rcvdSpmns = spmns.filter(function(spmn) { return spmn.receivedEvent.receivedQuality != SPMN_TBR; });
              if (rcvdSpmns.length > 0) {
                return showError('specimens.already_received', rcvdSpmns);
              }

              angular.forEach(spmns, function(spmn) { ExtensionsUtil.createExtensionFieldMap(spmn, true); });
              SpecimensHolder.setSpecimens(spmns);
              $state.go('receive-specimens', {event: 'SpecimenReceivedEvent'});
            }
          );
        }

        scope.createAliquots = function() {
          gotoView('specimen-bulk-create-aliquots', {}, 'no_specimens_to_create_aliquots');
        }

        scope.createDerivatives = function() {
          gotoView('specimen-bulk-create-derivatives', {}, 'no_specimens_to_create_derivatives');
        }

        scope.addEvent = function() {
          gotoView('bulk-add-event', {}, 'no_specimens_to_add_event');
        }

        scope.transferSpecimens = function() {
          gotoView('bulk-transfer-specimens', {}, 'no_specimens_to_transfer');
        }

        scope.retrieveSpecimens = function() {
          var selectedSpmns = scope.specimens();
          if (!selectedSpmns || selectedSpmns.length == 0) {
            Alerts.error('specimen_list.no_specimens_to_retrieve');
            return;
          }

          var thatScope = scope;
          $modal.open({
            templateUrl: 'modules/biospecimen/participant/specimen/retrieve.html',
            controller: function($scope, $modalInstance) {
              var input = $scope.input = {transferTime: new Date().getTime()};

              $scope.cancel = function() {
                $modalInstance.dismiss('cancel');
              }

              $scope.retrieve = function() {
                var spmnsToUpdate = selectedSpmns.map(
                  function(spmn) {
                    return {
                      id: spmn.id,
                      storageLocation: {},
                      transferTime: input.transferTime,
                      transferComments: input.transferComments
                    };
                  }
                );
                Specimen.bulkUpdate(spmnsToUpdate).then(
                  function(updatedSpmns) {
                    ParticipantSpecimensViewState.specimensUpdated(thatScope, {inline: true});
                    thatScope.initList();
                    $modalInstance.dismiss('cancel');
                  }
                );
              }
            }
          });
        }
      }
    };
  });
