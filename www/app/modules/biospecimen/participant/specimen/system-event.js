
angular.module('os.biospecimen.specimen')
  .controller('SpecimenSystemEventViewCtrl', function($scope) {

    var evtCtx;

    function init() {
      evtCtx = $scope.evtCtx = {
        specimen: $scope.data.osEntity,
        event: $scope.data.name
      };

      if ($scope.data.name == 'SpecimenTransferEvent' || $scope.data.name == 'ContainerTransferEvent') {
        initTransferEventFields();
      } else {
        initGeneralEventFields();
      }
    }

    function initFields(obj, fields) {
      angular.forEach(fields,
        function(field) {
          if (field.type == 'subForm') {
            var sfList = [];
            angular.forEach(field.value,
              function(sfFields) {
                var element = {};
                initFields(element, sfFields.fields);
                sfList.push(element);
              }
            );
            obj[field.name] = sfList;
          } else if (field.type == 'userField' || field.type == 'siteField' || 
            field.type == 'pvField' || field.type == 'storageContainer') {
            obj[field.name] = {id: +field.value, name: field.displayValue};
          } else {
            obj[field.name] = field.value;
          }
        }
      );
    }

    function initGeneralEventFields() {
      initFields(evtCtx, $scope.data.fields);
    }

    function initTransferEventFields() {
      angular.extend(evtCtx, {
        fromLocation: {mode: 'ALL'},
        toLocation: {mode: 'ALL'},
        user: null,
        time: null,
        comments: null
      });

      angular.forEach($scope.data.fields,
        function(field) {
          if (field.name == 'fromSite') {
            evtCtx.fromSite = {id: +field.value, name: field.displayValue};
          } else if (field.name == 'fromContainer' && field.value) {
            evtCtx.fromLocation.name = field.displayValue;
            evtCtx.fromLocation.id = +field.value;
          } else if (field.name == 'fromRow') {
            evtCtx.fromLocation.positionY = field.value;
          } else if (field.name == 'fromCol' || field.name == 'fromColumn') {
            evtCtx.fromLocation.positionX = field.value;
          } else if (field.name == 'fromPosition') {
            evtCtx.fromLocation.position = field.value;
          } else if (field.name == 'toSite') {
            evtCtx.toSite = {id: +field.value, name: field.displayValue};
          } else if (field.name == 'toContainer' && field.value) {
            evtCtx.toLocation.name = field.displayValue;
            evtCtx.toLocation.id = +field.value;
          } else if (field.name == 'toRow') {
            evtCtx.toLocation.positionY = field.value;
          } else if (field.name == 'toCol' || field.name == 'toColumn') {
            evtCtx.toLocation.positionX = field.value;
          } else if (field.name == 'toPosition') {
            evtCtx.toLocation.position = field.value;
          } else if (field.name == 'user') {
            evtCtx.user = {id: +field.value, name: field.displayValue};
          } else if (field.name == 'time') {
            evtCtx.time = +field.value;
          } else if (field.name == 'comments' || field.name == 'reason') {
            evtCtx.comments = field.value;
          } else if (field.name == 'status') {
            evtCtx.status = field.value;
          }
        }
      );
    }

    init();
  });
