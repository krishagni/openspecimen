angular.module('os.administrative.form.entities', ['os.common'])
  .factory('FormEntityReg', function(osDisplayList) {
    var list = osDisplayList();

    list.addItem({name: 'Participant', key: 'entities.participant_reg_form', caption: ''});
    list.addItem({name: 'CommonParticipant', key: 'entities.participant_form', caption: '', allCps: true});
    list.addItem({name: 'SpecimenCollectionGroup', key: 'entities.visit_form', caption: ''});
    list.addItem({name: 'Specimen', key: 'entities.specimen_form', caption: ''});
    list.addItem({name: 'SpecimenEvent', key: 'entities.specimen_event', caption: '', allCps: true});

    list.addItem({
      name: 'ParticipantExtension',
      caption: '', key: 'entities.part_reg_extension',
      multipleRecs: false
    });

    list.addItem({
      name: 'VisitExtension',
      caption: '', key: 'entities.visit_extension',
      multipleRecs: false
    });

    list.addItem({
      name: 'SpecimenExtension',
      caption: '', key: 'entities.specimen_extension',
      multipleRecs: false
    });

    list.addItem({
      name: 'SiteExtension',
      caption: '', key: 'entities.site_extension',
      allCps: true, multipleRecs: false
    });

    list.addItem({
      name: 'CollectionProtocolExtension',
      caption: '', key: 'entities.cp_extension',
      allCps: true, multipleRecs: false
    });

    list.addItem({
      name: 'DistributionProtocolExtension',
      caption: '', key: 'entities.dp_extension',
      allCps: true, multipleRecs: false
    });

    list.addItem({
      name: 'DpRequirementExtension',
      caption: '', key: 'entities.dp_req_extension',
      allCps: true, multipleRecs: false
    });

    list.addItem({
      name: 'OrderExtension',
      caption: '', key: 'entities.order_extension',
      allCps: true, multipleRecs: false, allowEdits: false
    });

    list.addItem({
      name: 'StorageContainerExtension',
      caption: '', key: 'entities.container_extension',
      allCps: true, multipleRecs: false
    });

    list.addItem({
      name: 'User',
      caption: '', key: 'entities.user_forms',
      allCps: true, multipleRecs: true
    });

    list.addItem({
      name: 'UserProfile',
      caption: '', key: 'entities.user_profile_forms',
      allCps: true, multipleRecs: true
    });

    return {
      getEntities: list.getItems,

      addEntity: list.addItem
    }
  });
