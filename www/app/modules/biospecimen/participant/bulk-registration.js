angular.module('os.biospecimen.participant.bulkregistration', ['os.biospecimen.models'])
  .controller('BulkRegistrationCtrl', function(
    $scope, $state, currentUser, events, cp, mrnAccessRestriction, CollectionProtocolRegistration, Alerts) {

    function init() {
      $scope.ctx = {
        mrnAccessRestriction: mrnAccessRestriction,
        cpEvents: events,
        cpSites: cp.cpSites.map(function(cs) { return cs.siteName; }),
        cpId: cp.id,
        selection: {
          events: []
        },
        kitDetail: {
          sendingDate : new Date(),
          sender: currentUser
        }
      };
    }

    $scope.toggleAllEventSelect = function() {
      $scope.ctx.selection.events = [];
      if ($scope.ctx.selection.all) {
        $scope.ctx.selection.events = [].concat($scope.ctx.cpEvents);
      }

      angular.forEach($scope.ctx.cpEvents,
        function(event) {
          event.selected = $scope.ctx.selection.all;
        }
      );
    }

    $scope.toggleEventSelect = function(event) {
      var events = $scope.ctx.selection.events;

      if (event.selected) {
        events.push(event);
      } else {
        var idx = events.indexOf(event);
        if (idx != -1) {
          events.splice(idx, 1);
        }
      }

      $scope.ctx.selection.all = (events.length == $scope.ctx.cpEvents.length);
    };

    $scope.registerParticipants = function(saveKit) {
      var bulkRegDetail = {
        cpId: $scope.ctx.cpId,
        regCount: $scope.ctx.regCount,
        collectionSite: $scope.ctx.collectionSite,
        events: angular.copy($scope.ctx.selection.events)
      }

      if (saveKit) {
        bulkRegDetail.kitDetail = $scope.ctx.kitDetail;
      }

      angular.forEach(bulkRegDetail.events, function(event) {
        delete event.selected;
      });

      CollectionProtocolRegistration.bulkRegistration(bulkRegDetail).then(
        function(cprList) {
          $state.go('participant-list', {cpId: $scope.cp.id});
          var key = !!saveKit ? 'participant.spmn_kit_created' : 'participant.participant_registered';
          Alerts.success(key, {numOfParticipants: bulkRegDetail.regCount});
        }
      )
    }

    $scope.passThrough = function() {
      return true;
    }

    init();
  });

