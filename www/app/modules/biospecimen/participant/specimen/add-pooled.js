angular.module('os.biospecimen.specimen')
  .controller('AddPooledSpecimenCtrl', function(
    $scope, $filter, $state, cp, cpr, visit, extensionCtxt,
    hasDict, cpDict, userRole, barcodingEnabled, spmnBarcodesAutoGen,
    layout, onValueChangeCb, imagingEnabled, spmnCollFields, additionalLabelAutoGen,
    CollectionProtocolRegistration, Visit, Util, Specimen, PluginReg,
    CpConfigSvc, AuthorizationService, CollectSpecimensSvc, SpecimensHolder, Alerts) {

    var ctx;

    function init() {
      var specimens = SpecimensHolder.getSpecimens() || [];
      SpecimensHolder.setSpecimens(undefined);
      angular.forEach(specimens,
        function(specimen) {
          delete specimen.parent;
          delete specimen.children;
        }
      );

      var sites = cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
      ctx = $scope.ctx = {
        cp: cp,
        defCprs: undefined,
        cprs: [],
        cpr: cpr,
        visits: [],
        visit: visit,
        extensionCtxt: extensionCtxt,
        spmnFilters: {
          cpId: cp.id, cpShortTitle: cp.shortTitle, collectionStatus: ['Collected'], exactMatch: true
        },
        spmnErrorOpts: {
          code: 'specimens.specimen_not_found_cp',
          code_m: 'specimens.specimen_m_not_found_cp',
          params: {cpShortTitle: cp.shortTitle}
        },
        poolItems: specimens || [],
        specimen: new Specimen({lineage: 'New', pooledSpecimen: true, status: 'Collected'}),
        form: {},
        allowMultipleSpecimens: false,
        hasDict: hasDict, sysDict: [], cpDict: cpDict, 
        userRole: userRole, barcodingEnabled: barcodingEnabled,
        spmnBarcodesAutoGen: spmnBarcodesAutoGen, editMode: false,
        layout: layout, onValueChange: onValueChangeCb, mdInput: false,
        hasInfo: PluginReg.getTmpls('specimen-addedit', 'info').length > 0,
        imagingEnabled: imagingEnabled, spmnCollFields: spmnCollFields,
        storeSpmn: false, additionalLabelAutoGen: additionalLabelAutoGen,
        allSpmnUpdate: AuthorizationService.isAllowed({cp: cp.shortTitle, sites: sites, resource: 'Specimen', operations: ['Update']}),

        showSelectVisit: !cp.specimenCentric && (!visit || visit.id <= 0),
        inited: false
      };

      CpConfigSvc.getCommonCfg(cp.id, 'addSpecimen').then(
        function(cfg) {
          angular.extend(ctx, cfg || {});
          ctx.inited = true;
        }
      );

      if (ctx.showSelectVisit && cpr) {
        addCprDisplayLabel(cpr);
        $scope.onCprSelect(cpr);
      }
    }

    function addCprDisplayLabel(cpr) {
      cpr.displayLabel = cpr.ppid;

      var p = cpr.participant;
      if (p.firstName || p.lastName) {
        cpr.displayLabel += ' (' + p.firstName + (p.firstName && ' ') + p.lastName + ')';
      }
    }

    $scope.searchParticipants = function(searchTerm) {
      if (!searchTerm || searchTerm.trim().length == 0) {
        searchTerm = '';
        if (ctx.defCprs) {
          ctx.cprs = ctx.defCprs;
          return;
        }
      }

      CollectionProtocolRegistration.query({cpId: ctx.cp.id, searchStr: searchTerm}).then(
        function(cprs) {
          ctx.cprs = cprs;
          angular.forEach(cprs, function(cpr) { addCprDisplayLabel(cpr); });
          if (!searchTerm && !ctx.defCprs) {
            ctx.defCprs = cprs;
          }
        }
      );
    },

    $scope.onCprSelect = function(cpr) {
      ctx.cpr = cpr;
      ctx.visit = null;
      Visit.listFor(cpr.id, false, true).then(
        function(visits) {
          ctx.visits = visits.filter(
            function(visit) {
              return visit.status == 'Complete';
            }
          ).map(
            function(visit) {
              var date = $filter('date')(visit.visitDate, ui.os.global.dateFmt);
              if (visit.eventLabel) {
                visit.displayLabel = visit.eventLabel + ' (' + date + ')';
              } else {
                visit.displayLabel = 'Unplanned (' + date + ')';
              }

              return visit;
            }
          );
        }
      );
    },

    $scope.addSpecimens = function(specimens) {
      if (!specimens) {
        return false;
      }

      Util.addIfAbsent(ctx.poolItems, specimens, 'id');
      return true;
    }

    $scope.removeSpecimen = function(specimen) {
      var idx = ctx.poolItems.indexOf(specimen);
      ctx.poolItems.splice(idx, 1);
    }

    $scope.passThrough = function() {
      return true;
    }

    $scope.validateSpecimens = function(forward) {
      if (!forward) {
        return true;
      }

      if (!ctx.poolItems || ctx.poolItems.length < 2) {
        Alerts.error('specimens.select_min_to_create_pooled');
        return false;
      }

      return true;
    }

    $scope.gotoCollection = function() {
      var specimensToCollect = [];
      var spmns = ctx.form.ctrl.getSpecimens(true);
      if (spmns) {
        spmns[0].selected = true;
        spmns[0].status = 'Pending';
        spmns[0].specimensPool = ctx.poolItems;
        Array.prototype.push.apply(specimensToCollect, spmns);
      } else {
        return;
      }

      var opts  = {showCollVisitDetails: false};
      var state = {
        state: $state.get('visit-detail.overview'),
        params: {cpId: cp.id, cprId: ctx.cpr.id, visitId: ctx.visit.id}
      };

      if (cp.specimenCentric) {
        state = {state: $state.get('cp-specimens'), params: {cpId: cp.id}};
      }

      CollectSpecimensSvc.collect(state, ctx.visit, specimensToCollect, opts);
    }

    init();
  });
