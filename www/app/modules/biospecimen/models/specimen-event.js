angular.module('os.biospecimen.models.specimenevent', ['os.common.models'])
  .factory('SpecimenEvent', function(osModel, $http, Form, CollectionProtocol) {
    var SpecimenEvent = osModel('specimen-events');

    var sysEvents = [
      'SpecimenDisposalEvent',
      'SpecimenDistributedEvent',
      'SpecimenTransferEvent',
      'SpecimenShipmentShippedEvent',
      'SpecimenShipmentReceivedEvent',
      'SpecimenReturnEvent',
      'SpecimenChildrenEvent'
    ];

    var editableEvents = [
      'SpecimenCollectionEvent',
      'SpecimenReceivedEvent',
      'SpecimenDisposalEvent',
      'SpecimenTransferEvent'
    ];

    var disabledFields = {
      'SpecimenTransferEvent': [
        'fromContainer', 'fromRow', 'fromCol', 'fromDimensionTwo', 'fromDimensionOne', 'fromPosition',
        'toContainer', 'toRow', 'toCol', 'toDimensionTwo', 'toDimensionOne', 'toPosition',
        'status'
      ]
    }
    
    SpecimenEvent.getEvents = function() {
      return new CollectionProtocol({id: -1}).getForms(['SpecimenEvent']);
    }

    SpecimenEvent.save = function(formId, data) {
      return $http.post(SpecimenEvent.url() + '/' + formId, data);
    }

    SpecimenEvent.addSysEvent = function(eventName) {
      if (sysEvents.indexOf(eventName) == -1) {
        sysEvents.push(eventName);
      }
    }

    SpecimenEvent.isSysEvent = function(eventName) {
      return sysEvents.indexOf(eventName) != -1;
    }

    SpecimenEvent.isEditable = function(eventName) {
      return editableEvents.indexOf(eventName) != -1;
    }

    SpecimenEvent.getDisabledFields = function(eventName) {
      return disabledFields[eventName] || [];
    }
    
    return SpecimenEvent;
  });
