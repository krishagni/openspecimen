
import http from '@/common/services/HttpClient.js';
import i18n from '@/common/services/I18n.js';
import util from '@/common/services/Util.js';

class Form {
  async getDefinition(formId) {
    const formDef = await http.get('forms/' + formId + '/definition', {maxPvs: 100});
    return this._addFormId(formDef);
  }

  async downloadDefinition(formId) {
     http.downloadFile(http.getUrl('forms/' + formId + '/definition-zip'));
  }

  async getDefinitionByName(formName) {
    const formDef = await http.get('forms/definition', {name: formName, maxPvs: 100});
    return this._addFormId(formDef);
  }

  async getPvs(formId, fieldName, query) {
    const params = {controlName: fieldName, searchString: query};
    return http.get('forms/' + formId + '/permissible-values', params);
  }

  async getPvsByName(formName, fieldName, query) {
    const params = {formName, controlName: fieldName, searchString: query};
    return http.get('forms/permissible-values', params);
  }

  async getRecord(record, opts) {
    return http.get('forms/' + record.formId + '/data/' + record.recordId, opts);
  }

  async saveOrUpdateRecord(record) {
    record.appData = record.appData || {};
    let formId = record.appData.formId;
    if (!formId) {
      alert('Unknown form ID. Maybe a bug in the UI form setup. Contact the system administrator');
      alert(JSON.stringify(record));
      return;
    }

    if (record.id) {
      return http.put('forms/' + formId + '/data', record);
    } else {
      return http.post('forms/' + formId + '/data', record);
    }
  }

  async deleteRecord(record) {
    return http.delete('forms/' + record.formId + '/data/' + record.recordId);
  }

  async getLatestRecord(formId, entityType, objectId) {
    return http.get('forms/' + formId + '/latest-record', {entityType, objectId});
  }

  async getForms(filterOpts) {
    return http.get('forms', filterOpts || {});
  }

  async getFormsCount(filterOpts) {
    return http.get('forms/count', filterOpts || {});
  }

  async createForm(formDef) {
    return http.put('forms/-1', formDef);
  }

  async deleteForm(formId) {
    return http.delete('forms/' + formId);
  }

  async getDependents(formId) {
    return http.get('forms/' + formId + '/dependent-entities');
  }

  async deleteForms(formIds) {
    return http.delete('forms', {}, {id: formIds});
  }

  async getRevisions(formId) {
    return http.get('forms/' + formId + '/revisions');
  }

  async getRevisionDef(formId, rev) {
    return http.get('forms/' + formId + '/revisions/' + rev + '/definition');
  }

  async downloadRevision(formId, rev) {
    http.downloadFile(http.getUrl('forms/' + formId + '/revisions/' + rev + '/definition-zip'));
  }

  async getFormAssociations(formId) {
    return http.get('forms/' + formId + '/contexts');
  }

  saveAssociation(association) {
    const {formId} = association;
    return http.put('forms/' + formId + '/contexts', [association]);
  }

  deleteAssociation(associationId) {
    return http.delete('forms/contexts/' + associationId);
  }

  getAssociationRevisions(formId) {
    return http.get('forms/' + formId + '/context-revisions');
  }

  addAssociationType(entityType, entityTypeDef) {
    this.entityTypes[entityType] = entityTypeDef;
  }

  getEntityTypes() {
    this._initEntityTypeLabels();
    return util.clone(Object.values(this.entityTypes));
  }

  getEntityType(type) {
    if (type.lastIndexOf('-') != -1) {
      type = type.substring(0, type.lastIndexOf('-'));
    }

    this._initEntityTypeLabels();
    return util.clone(this.entityTypes[type] || {entityType: type});
  }

  isCpBasedAssociationType(entityType) {
    const type = this.entityTypes[entityType];
    return type && type.cpBased;
  }

  isInstituteBasedAssociationType(entityType) {
    const type = this.entityTypes[entityType];
    return type && type.instituteBased;
  }

  isAssociationEditAllowed(entityType) {
    const type = this.entityTypes[entityType];
    return type && type.allowEdits;
  }

  isAssociationDeleteAllowed(entityType) {
    const type = this.entityTypes[entityType];
    return type && type.allowDelete;
  }

  _addFormId(formDef) {
    formDef.rows.forEach(
      row => row.forEach(
        (field) => {
          field.formId = formDef.id;
          if (field.type == 'subForm') {
            field.rows.forEach(
              sfRow => sfRow.forEach(
                sfField => {
                  sfField.formId = formDef.id;
                  sfField.fqn = field.name + '.' + sfField.name;
                }
              )
            );
          } else {
            field.fqn = field.name;
          }
        }
      )
    );

    return formDef;
  }

  _initEntityTypeLabels() {
    if (this.labelsInitialised) {
      return;
    }

    const entityTypes = Object.values(this.entityTypes);
    entityTypes.forEach(
      (entityType, idx) => {
        entityType.id = idx + 1;
        entityType.entityTypeLabel = i18n.msg('forms.entities.' + entityType.entityType);
      }
    );

    this.labelsInitialised = true;
  }

  labelsInitialised = false;

  entityTypes = {
    'CollectionProtocolExtension': {
      entityType: 'CollectionProtocolExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: true,
      cpBased: false,
      allCps: true
    },

    'Participant': {
      entityType: 'Participant',
      allowMultipleRecs: true,
      allowNotifs: true,
      allowEdits: true,
      allowDelete: true,
      cpBased: true
    },

    'CommonParticipant': {
      entityType: 'CommonParticipant',
      allowMultipleRecs: true,
      allowNotifs: true,
      allowEdits: true,
      allowDelete: true,
      cpBased: false,
      allCps: true
    },

    'ParticipantExtension': {
      entityType: 'ParticipantExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: true,
      cpBased: true,
      allCps: false
    },

    'SpecimenCollectionGroup': {
      entityType: 'SpecimenCollectionGroup',
      allowMultipleRecs: true,
      allowNotifs: true,
      allowEdits: true,
      allowDelete: true,
      cpBased: true
    },

    'VisitExtension': {
      entityType: 'VisitExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: true,
      cpBased: true,
      allCps: false
    },

    'Specimen': {
      entityType: 'Specimen',
      allowMultipleRecs: true,
      allowNotifs: true,
      allowEdits: true,
      allowDelete: true,
      cpBased: true
    },

    'SpecimenExtension': {
      entityType: 'SpecimenExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      cpBased: true,
      allowDelete: true,
      allCps: false
    },

    'SpecimenEvent': {
      entityType: 'SpecimenEvent',
      allowMultipleRecs: true,
      allowNotifs: true,
      allowEdits: true,
      allowDelete: true,
      cpBased: false,
      allCps: true
    },

    'SiteExtension': {
      entityType: 'SiteExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: true,
      cpBased: false,
      allCps: true
    },

    'DistributionProtocolExtension': {
      entityType: 'DistributionProtocolExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: true,
      cpBased: false,
      allCps: true
    },

    'DpRequirementExtension': {
      entityType: 'DpRequirementExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: true,
      cpBased: false,
      allCps: true
    },

    'OrderExtension': {
      entityType: 'OrderExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: false,
      cpBased: false,
      allCps: true,
      forbidAdd: true
    },

    'StorageContainerExtension': {
      entityType: 'StorageContainerExtension',
      allowMultipleRecs: false,
      allowNotifs: false,
      allowEdits: false,
      allowDelete: true,
      cpBased: false,
      allCps: true
    },

    'User': {
      entityType: 'User',
      allowMultipleRecs: true,
      allowNotifs: true,
      allowEdits: true,
      allowDelete: true,
      cpBased: false,
      allCps: true,
      instituteBased: true
    },

    'UserProfile': {
      entityType: 'UserProfile',
      allowMultipleRecs: true,
      allowNotifs: true,
      allowEdits: true,
      allowDelete: true,
      cpBased: false,
      allCps: true,
      instituteBased: true
    }
  };
}

export default new Form();
