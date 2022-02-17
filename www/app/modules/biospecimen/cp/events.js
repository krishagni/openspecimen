
angular.module('os.biospecimen.cp.events', ['os.biospecimen.models'])
  .controller('CpEventsCtrl', function(
     $scope, $state, $stateParams, cp, events, mrnAccessRestriction,
     Alerts, CollectionProtocolEvent, PvManager, Util) {

    var pvsLoaded = false;

    var copyFrom = undefined;

    function init() {
      $scope.cp = cp;
      $scope.events = events;
      $scope.mode = undefined;
      $scope.mrnAccessRestriction = mrnAccessRestriction;
         
      $scope.event = {};
      $scope.selected = {};
    }

    function loadPvs() {
      if (pvsLoaded) {
        return;
      }

      $scope.eventPointUnits     = PvManager.getPvs('interval-units');
      pvsLoaded = true;
    }

    function loadSpecimenRequirements(event, reload) {
      $scope.selected = event;
      $state.go('cp-detail.specimen-requirements', {eventId: event.id}, {reload: reload});
    }

    $scope.selectEvent = function(event) { 
      loadSpecimenRequirements(event);
    };

    $scope.showAddEvent = function() {
      $scope.event = new CollectionProtocolEvent({collectionProtocol: cp.title, eventPointUnit: 'DAYS'});
      $scope.mode = 'add';
      loadPvs();
    };

    $scope.showEditEvent = function(evt) {
      $scope.event = angular.copy(evt);
      $scope.mode = undefined;
      loadSpecimenRequirements(evt);
      loadPvs();
    };

    $scope.showCopyEvent = function(evt) {
      $scope.event = angular.copy(evt);

      copyFrom = $scope.event.id;
      delete $scope.event.code;
      delete $scope.event.eventLabel;
      delete $scope.event.id;

      $scope.mode = 'copy';
      loadPvs();
    };

    $scope.revertEdit = function() {
      $scope.mode = undefined;
      $scope.event = {};
    };

    $scope.addEvent = function() {
      var ret = undefined;
      if ($scope.mode == 'add') {
        ret = $scope.event.$saveOrUpdate();
      } else {
        ret = $scope.event.copy(copyFrom);
      }

      ret.then(
        function(result) {
          events.push(result);
          $scope.event = {};
          $scope.mode = undefined;
          loadSpecimenRequirements(result);
          cp.draftMode = true;
        }
      );
    };

    $scope.editEvent = function() {
      $scope.event.$saveOrUpdate().then(
        function(result) {
          for (var i = 0; i < events.length; ++i) {
            if (events[i].id == result.id) {
              events[i] = result;
              break;
            }
          }
          $scope.event = {};
          loadSpecimenRequirements(result);
          cp.draftMode = true;
        }
      );
    };

    $scope.deleteEvent = function(evt) {
      Util.showConfirm({
        templateUrl: 'modules/biospecimen/cp/event_delete.html',
        event: evt,
        ok: function() {
          evt.delete().then(
            function() {
              var idx = events.indexOf(evt);
              events.splice(idx, 1);
              if (events.length > 0) {
                $scope.selectEvent(events[0]);
              }

              cp.draftMode = true;
            }
          );
        }
      });
    }

    $scope.closeEvent = function(evt) {
      Util.showConfirm({
        templateUrl: 'modules/biospecimen/cp/event_close.html',
        event: evt,
        ok: function() {
          var toClose = angular.copy(evt);
          toClose.activityStatus = 'Closed';

          toClose.$saveOrUpdate().then(
            function(closedEvt) {
              angular.extend(evt, closedEvt);
              loadSpecimenRequirements(evt, true);
              cp.draftMode = true;
            }
          );
        }
      });
    }

    $scope.reopenEvent = function(evt) {
      var toOpen = angular.copy(evt);
      toOpen.activityStatus = 'Active';

      toOpen.$saveOrUpdate().then(
        function(openedEvt) {
          angular.extend(evt, openedEvt);
          loadSpecimenRequirements(evt, true);
          cp.draftMode = true;
        }
      );
    }

    init();
  });
