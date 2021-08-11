angular.module('os.biospecimen.specimen.bulkaddevent', ['os.biospecimen.models'])
  .controller('BulkAddEventCtrl', function(
    $scope, $translate, $q, currentUser, events, event, Form, SpecimensHolder,
    Specimen, SpecimenEvent, Alerts, Util, SpecimenUtil, SettingUtil) {

    var ctx;
    function init() {
      var spmns = SpecimensHolder.getSpecimens() || [];
      SpecimensHolder.setSpecimens(null);
      filterSpecimens(spmns);

      var formOpts = {};
      $scope.ctx = ctx = {
        events   : events,
        event    : event,
        specimens: spmns,
        op       : event && event.name == 'SpecimenReceivedEvent' ? 'EDIT' : 'ADD',
        mode     : event && event.name == 'SpecimenReceivedEvent' ? 'TABLE' : 'SINGLE',
        tabCtrl  : {},
        formCtrl : {},
        opts     : {
          appColumns        : [],
          idColumnLabel     : $translate.instant('specimens.title'),
          mode              : 'add',
          allowRowSelection : false,
          onValidationError : onValidationError,
          showActionBtns    : false,
          labelAlignment    : 'horizontal',
          records           : []
        },
        showVisit: showVisit(spmns),
        formDef  : undefined,
        mFormDef : undefined,
        formDefToUse: undefined,
        allowEventSelect: !event,
        spmnFilterOpts: {exactMatch: true, includeExtensions: false},
        records  : []
      };
    }

    function isAddOp() {
      return ctx.op == 'ADD';
    }

    function isTabMode() {
      return ctx.mode == 'TABLE';
    }

    function isCollRecvEvent(event) {
      return ['SpecimenCollectionEvent', 'SpecimenReceivedEvent'].indexOf(event.name) != -1;
    }

    function filterEvents() {
      var showCollRecvEvent =  !isAddOp();
      if (showCollRecvEvent) {
        showCollRecvEvent = ctx.specimens.every(function(spmn) { return spmn.lineage == 'New'; });
      }

      return events.filter(
        function(event) {
          return !event.sysForm || (showCollRecvEvent && isCollRecvEvent(event));
        }
      );
    }

    function getTableData() {
      var opts = [], optsMap = {};
      angular.forEach(ctx.specimens,
        function(spmn) {
          var opt = {
            key: {
              id: spmn.id,
              objectId: spmn.id,
              label: spmn.label
            },
            appColumnsData: {},
            records: []
          };
          opts.push(opt);
          optsMap[spmn.id] = opt;
        }
      );

      if (ctx.op == 'EDIT') {
        angular.forEach(ctx.records,
          function(record) {
            var spmnId = record.appData.objectId;
            optsMap[spmnId].records.push(record);
          }
        );
      }

      return opts;
    }

    function onValidationError() {
      Alerts.error('common.form_validation_error');
    }

    function showVisit(specimens) {
      return specimens.some(function(spmn) { return !spmn.$$specimenCentricCp; });
    }

    function removeRequiredAndDefValues(formDef) {
      var result = angular.copy(formDef);
      angular.forEach(result.rows,
        function(row) {
          angular.forEach(row,
            function(field) {
              field.defaultValue = field.defaultType = field.defaultChecked = undefined;
              field.validationRules = field.validationRules || [];
              field.defaultType = 'none';
              for (var i = 0; i < field.validationRules.length; ++i) {
                var rule = field.validationRules[i];
                if (rule.name == 'required') {
                  field.validationRules.splice(i, 1);
                  break;
                }
              }
            }
          )
        }
      );

      return result;
    }

    function setFormDefToUse() {
      if (isAddOp() || isTabMode()) {
        ctx.formDefToUse = ctx.formDef;
      } else {
        ctx.formDefToUse = ctx.mFormDef;
      }
    }

    function getEventRecords() {
      if (ctx.op == 'ADD' && ctx.mode == 'SINGLE') {
        var formData = ctx.formCtrl.ctrl.getFormData();
        var eventData = {};
        angular.forEach(formData.attrs,
          function(attr) {
            eventData[attr.name] = attr.value;
          }
        );

        var records = [];
        angular.forEach(ctx.specimens,
          function(specimen) {
            var record = angular.copy(eventData);
            record.appData = record.appData || {};
            record.appData.id = specimen.id;
            records.push(record);
          }
        );

        return records;
      } else if (ctx.op == 'EDIT' && ctx.mode == 'SINGLE') {
        var formData = ctx.formCtrl.ctrl.getFormData();
        angular.forEach(ctx.records,
          function(record) {
            record.appData.id = record.appData.objectId;
            angular.forEach(formData.attrs,
              function(attr) {
                if (!attr.value && attr.value !== 0) {
                  record[attr.name] = record.$$original[attr.name];
                } else {
                  record[attr.name] = attr.value;
                }
              }
            )
          }
        );

        return ctx.records;
      } else if (ctx.mode == 'TABLE') {
        return ctx.tabCtrl.ctrl.getData();
      }
    }

    function loadRecords() {
      var result = [];
      if (ctx.op != 'EDIT') {
        return result;
      }

      var spmnIds = ctx.specimens.map(function(spmn) { return spmn.id; });
      return Form.getLatestRecords(ctx.formId, 'SpecimenEvent', spmnIds).then(
        function(records) {
          angular.forEach(records,
            function(record) {
              record.$$original = angular.copy(record);
              result.push(record);
            }
          );

          return result;
        }
      );
    }

    function showError(error, spmns) {
      Alerts.error(error, {specimens: spmns.map(function(s) { return !s.label ? s.id : s.label; }).join(', ')});
    }

    function filterSpecimens(spmns) {
      var closedSpmns = spmns.filter(function(spmn) { return spmn.activityStatus != 'Active'; });
      if (closedSpmns.length > 0) {
        showError('specimens.closed_edit_na', closedSpmns);
        return false;
      }

      var ncSpmns = spmns.filter(function(spmn) { return spmn.status != 'Collected'; });
      if (ncSpmns.length > 0) {
        showError('specimens.not_collected', ncSpmns);
        return false;
      }

      if (event && event.name == 'SpecimenReceivedEvent') {
        var nonPrimarySpmns = spmns.filter(function(spmn) { return spmn.lineage != 'New'; });
        if (nonPrimarySpmns.length > 0) {
          showError('specimens.non_primary_receive_na', nonPrimarySpmns);
          return false;
        }
      }

      return true;
    }

    //
    // View functions
    //
    $scope.passThrough = function() {
      return true;
    }

    $scope.addSpecimens = function(specimens) {
      if (!specimens) {
        return false;
      }

      if (!filterSpecimens(specimens)) {
        return false;
      }

      if (!ctx.showVisit) {
        ctx.showVisit = showVisit(specimens);
      }

      Util.addIfAbsent(ctx.specimens, specimens, 'id');
      return true;
    }
    
    $scope.removeSpecimen = function(index) {
      ctx.specimens.splice(index, 1);
      ctx.showVisit = showVisit(ctx.specimens);
    }

    $scope.validateSpecimens = function() {
      if (event && event.name == 'SpecimenReceivedEvent') {
        ctx.formId = event.formId;
        $scope.initEventDetailsView();
      }

      return true;
    }

    $scope.initOptionsView = function() {
      ctx.events = filterEvents();
    }

    $scope.initEventDetailsView = function() {
      var formDefQ = Form.getDefinition(ctx.formId);
      var recordsQ = loadRecords();
      var settingsQ = SettingUtil.getSetting('administrative', 'allow_spmn_relabeling').then(
        function(setting) {
          return setting.value == 'true' || setting.value == true;
        }
      );

      ctx.noEvents = false; ctx.noEventsFor = [];
      $q.all([formDefQ, recordsQ, settingsQ]).then(
        function(resps) {
          var formDef = resps[0];
          var records = resps[1];
          var allowSpmnRelabeling = resps[2];

          ctx.formDef = formDef;
          if (event && event.name == 'SpecimenReceivedEvent') {
            if (allowSpmnRelabeling) {
              formDef.rows.splice(0, null,
                [{
                  name: 'label',
                  udn: 'label',
                  caption: $translate.instant('specimens.new_label'),
                  type: 'stringTextField',
                  validationRules: []
                }]
              );
            }

            var time = new Date().getTime();
            var userId = currentUser.id;
            angular.forEach(records,
              function(record) {
                record.time = time;
                record.user = userId;
              }
            );
          }

          ctx.mFormDef = removeRequiredAndDefValues(formDef);
          setFormDefToUse();
          ctx.opts.formId  = ctx.formId;
          ctx.opts.formDef = ctx.formDefToUse;

          ctx.records = records;
          if (!records || records.length == 0) {
            ctx.noEvents = (ctx.op == 'EDIT');
            ctx.op = 'ADD';
            setFormDefToUse();
            ctx.opts.formId  = ctx.formId;
            ctx.opts.formDef = ctx.formDefToUse;
          } else if (ctx.op == 'EDIT' && records.length != ctx.specimens.length) {
            var spmnIds = records.map(function(rec) { return rec.appData.objectId; });
            ctx.noEventsFor = ctx.specimens.filter(function(spmn) { return spmnIds.indexOf(spmn.id) == -1 })
              .map(function(spmn) { return spmn.label; }).join(', ');
          }

          if (ctx.mode == 'TABLE') {
            ctx.opts.tableData = getTableData();
          }

          ctx.showForm = true;
        }
      );
    }

    $scope.onEventSelect = function() {
      ctx.records = undefined;
    }

    $scope.onOpSelect = function() {
      ctx.records = undefined;
      ctx.events = filterEvents();
      if (ctx.formId && !ctx.events.some(function(event) { event.id == ctx.formId; })) {
        ctx.formId = undefined;
      }
    }

    $scope.onModeSelect = function() {
    }

    $scope.saveEvent = function(addMore) {
      var records = getEventRecords();
      SpecimenEvent.save(ctx.formId, records).then(
        function(savedData) {
          Alerts.success("specimens.bulk_events.events_saved");

          if (addMore) {
            ctx.showForm = false;
            ctx.formDef = undefined;
            ctx.mFormDef = undefined;
            ctx.records = [];
            ctx.opts.tableData = undefined;
            ctx.opts.formDef = undefined;
            $scope.eventWizard.previous()
          } else {
            $scope.back();
          }
        }
      );
    }

    $scope.copyFirstToAll = function () {
      ctx.tabCtrl.ctrl.copyFirstToAll();
    }
    
    init();
  });
