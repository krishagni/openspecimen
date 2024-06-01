
import authSvc from '@/common/services/Authorization.js';
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc from '@/biospecimen/services/Cpr.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import routerSvc from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import visitSvc from '@/biospecimen/services/Visit.js';
import util from '@/common/services/Util.js';

import matchingTab from '@/biospecimen/schemas/participants/matching-participants.js';

export default class CpViewContext {
  cpId = null;

  cprDictQ = null;

  visitDictQ = null;

  visitsTabQ = null;

  specimenDictQ = null;

  cp = null;

  cpSites = null;

  access = null;

  accessBasedOnMrn = false;

  coordinatorRole = null;

  wfIds = {};

  constructor(cp, {accessBasedOnMrn, coordinatorRole}) {
    this.cp = cp;
    this.cpId = cp.id;
    this.cpSites = cp.cpSites.map(({siteName}) => siteName);
    this.accessBasedOnMrn = accessBasedOnMrn;
    this.coordinatorRole = coordinatorRole;
    this._loadAccessRights();
  }

  getCp() {
    return this.cp;
  }

  getCpEvents() {
    if (!this.cpEventsQ) {
      this.cpEventsQ = cpSvc.getCpes(this.cpId)
        .then(events => events.filter(event => event.activityStatus == 'Active'));
    }

    return this.cpEventsQ;
  }

  getCprDict(dataEntry) {
    if (!this.cprDictQ) {
      this.cprDictQ = cprSvc.getDict(this.cpId);
    }

    return this.cprDictQ.then(fields => dataEntry ? fields.filter(field => field.name.indexOf('calc') != 0) : fields);
  }

  getConsentsDict() {
    if (!this.consentsDictQ) {
      this.consentsDictQ = cpSvc.getDictFor(this.cpId, ['consents']);
    }

    return this.consentsDictQ;
  }

  getCprAddEditLayout() {
    return this.getCprDict(true).then(dict => cprSvc.getLayout(this.cpId, dict));
  }

  isTwoStepEnabled() {
    return settingSvc.getSetting('biospecimen', 'two_step_patient_reg')
      .then(settings => util.isTrue(settings[0].value));
  }

  isAddPatientOnLookupFailEnabled() {
    return settingSvc.getSetting('biospecimen', 'add_patient_on_lookup_fail')
      .then(settings => util.isTrue(settings[0].value));
  }

  async getLockedParticipantFields(source) {
    return cpSvc.getLockedFields(-1, 'participant', source);
  }

  async getSelectMatchTabSchema() {
    return matchingTab;
  }

  async getParticipantForms(context) {
    const {cpr} = context;
    const promises = [
      cprSvc.getForms(cpr),
      cprSvc.getFormDataEntryRules(this.cpId),
      cprSvc.getFormsOrderSpec(this.cpId)
    ];

    return Promise.all(promises).then(
      ([forms, rules, orderSpec]) => this._sortForms(this._getMatchingForms(forms, rules, context), orderSpec)
    );
  }

  async getSurveyForms() {
    const {edcSurveySvc} = window.osSvc;
    if (edcSurveySvc) {
      return edcSurveySvc.getSurveys({cpId: this.cpId});
    }

    return [];
  }

  getVisitDict(dataEntry) {
    if (!this.visitDictQ) {
      this.visitDictQ = visitSvc.getDict(this.cpId);
    }

    return this.visitDictQ.then(fields => dataEntry ? fields.filter(field => field.name.indexOf('calc') != 0) : fields);
  }

  async getVisitAddEditLayout() {
    return this.getVisitDict(true).then(dict => visitSvc.getLayout(this.cpId, dict));
  }

  getVisitsTab() {
    if (!this.visitsTabQ) {
      this.visitsTabQ = visitSvc.getVisitsTab(this.cpId);
    }

    return this.visitsTabQ;
  }

  async isMultiVisitsCollectionAllowed() {
    const value = await cpSvc.getWorkflowProperty(this.cpId || -1, 'common', 'allowMultiVisitsCollection');
    return value == true || value == 'true' || value == 1 || value == '1';
  }

  getOccurredVisitsTabFields() {
    return this.getVisitsTab().then(
      (visitsTab) => {
        const tabFields = util.clone(visitsTab.occurred || []);
        if (tabFields.length == 0) {
          Array.prototype.push.apply(tabFields, visitSvc.getDefaultOccurredVisitsTabFields());
        } else if (!tabFields[0].href) {
          tabFields[0].href = ({rowObject: {visit}}) => this._getVisitUrl(visit)
        }

        for (let field of tabFields) {
          if (field.name.indexOf('calc') == 0 && field.displayExpr) {
            field.value = (row) => exprUtil.eval({...row, fns: util.fns()}, field.displayExpr);
          }
        }

        if (visitsTab.hideCollectionStats != true && visitsTab.hideCollectionStats != 'true') {
          tabFields.push({
            type: 'component',
            captionCode: 'visits.collection_stats',
            component: 'os-visit-specimen-collection-stats',
            data: (rowObject) => rowObject,
            uiStyle: {
              width: '150px'
            }
          });
        }

        if (visitsTab.hideUtilisationStats != true && visitsTab.hideUtilisationStats != 'true') {
          tabFields.push({
            type: 'component',
            captionCode: 'visits.utilisation_stats',
            component: 'os-visit-specimen-utilisation-stats',
            data: (rowObject) => rowObject,
            uiStyle: {
              width: '150px'
            }
          });
        }

        return tabFields;
      }
    );
  }

  getMissedVisitsTabFields() {
    return this.getVisitsTab().then(
      (visitsTab) => {
        const tabFields = util.clone(visitsTab.missed || []);
        if (tabFields.length == 0) {
          Array.prototype.push.apply(tabFields, visitSvc.getDefaultMissedVisitsTabFields());
        } else if (!tabFields[0].href) {
          tabFields[0].href = ({rowObject: {visit}}) => this._getVisitUrl(visit)
        }

        return tabFields;
      }
    );
  }

  getPendingVisitsTabFields() {
    return this.getVisitsTab().then(
      (visitsTab) => {
        const tabFields = util.clone(visitsTab.pending || []);
        if (tabFields.length == 0) {
          Array.prototype.push.apply(tabFields, visitSvc.getDefaultPendingVisitsTabFields());
        } else if (!tabFields[0].href) {
          tabFields[0].href = ({rowObject: {visit}}) => this._getVisitUrl(visit)
        }

        return tabFields;
      }
    );
  }

  getAnticipatedEventsRules() {
    return this.getVisitsTab().then(
      (visitsTab) => {
        const anticipatedEvents = {rules: [], matchType: 'any'};
        if (visitsTab.anticipatedEvents instanceof Array) {
          anticipatedEvents.rules = visitsTab.anticipatedEvents;
        } else if (typeof visitsTab.anticipatedEvents == 'object') {
          Object.assign(anticipatedEvents, visitsTab.anticipatedEvents);
        }

        return anticipatedEvents;
      }
    );
  }

  async getVisitForms(context) {
    const {visit} = context;
    const promises = [
      visitSvc.getForms(visit),
      visitSvc.getFormDataEntryRules(this.cpId),
      visitSvc.getFormsOrderSpec(this.cpId)
    ];

    return Promise.all(promises).then(
      ([forms, rules, orderSpec]) => this._sortForms(this._getMatchingForms(forms, rules, context), orderSpec)
    );
  }

  async isSaveSprEnabled() {
    const cp = this.getCp();
    if (cp.storeSprEnabled == null || cp.storeSprEnabled == undefined) {
      return settingSvc.getSetting('biospecimen', 'store_spr')
        .then(settings => util.isTrue(settings[0].value));
    }

    return cp.storeSprEnabled;
  }

  getSpecimenDict(dataEntry) {
    if (!this.specimenDictQ) {
      this.specimenDictQ = specimenSvc.getDict(this.cpId);
    }

    return this.specimenDictQ.then(fields => dataEntry ? fields.filter(field => field.name.indexOf('calc') != 0) : fields);
  }

  async getSpecimenAddEditLayout() {
    return this.getSpecimenDict(true).then(dict => specimenSvc.getLayout(this.cpId, dict));
  }

  async getSpecimenEventForms(context) {
    const {specimen} = context;
    return Promise.all([specimenSvc.getEventForms(specimen.id), specimenSvc.getFormDataEntryRules(this.cpId)]).then(
      ([eventForms, rules]) => this._getMatchingForms(eventForms, rules, context)
    );
  }

  async getSpecimenForms(context) {
    const {specimen} = context;
    const promises = [
      specimenSvc.getForms(specimen),
      specimenSvc.getFormDataEntryRules(this.cpId),
      specimenSvc.getFormsOrderSpec(this.cpId)
    ];

    return Promise.all(promises).then(
      ([forms, rules, orderSpec]) => this._sortForms(this._getMatchingForms(forms, rules, context), orderSpec)
    );
  }

  //
  // Access control
  //

  // needs to be invoked only after CP is loaded
  getRole() {
    return cpSvc.getUserRoleOn(this.cp);
  }

  isAccessBasedOnMrnSite() {
    return this.accessBasedOnMrn;
  }

  isCoordinator() {
    return this.getRole() == this.coordinatorRole;
  }

  notCoordinatOrStoreAllowed({storageLocation}) {
    const storeAllowed = this._isAllowed('StorageContainer', ['Read']);
    const {storageSiteBasedAccess} = this.cp;
    storageLocation = storageLocation || {};
    return !this.isCoordinator() || (storageSiteBasedAccess && storeAllowed && storageLocation.id > 0);
  }

  isCreateParticipantAllowed() {
    return this.access && this.access.createParticipant;
  }

  isUpdateParticipantAllowed(cpr) {
    if (!this.access || !this.access.updateParticipant) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'ParticipantPhi', ['Update']);
  }

  isDeleteParticipantAllowed(cpr) {
    if (!this.access || !this.access.deleteParticipant) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'ParticipantPhi', ['Delete']);
  }

  isProceedToConsentAllowed() {
    return this._isAllowed('Consent', ['Update']);
  }

  isImportAllowed() {
    const {specimenCentric} = this.cp || {};
    const {participantExim, visitExim, specimenExim} = this.access || {};
    return (!specimenCentric && !!participantExim) || (!specimenCentric && !!visitExim) || !!specimenExim;
  }

  isExportAllowed() {
    return this.isImportAllowed();
  }

  isCreateVisitAllowed(cpr) {
    if (!this.access || !this.access.createVisit) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Visit', ['Create']);
  }

  isUpdateVisitAllowed(cpr) {
    if (!this.access || !this.access.updateVisit) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Visit', ['Update']);
  }

  isCreateOrUpdateVisitAllowed(cpr) {
    if (!this.access || !this.access.createUpdateVisit) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Visit', ['Create', 'Update']);
  }

  isReadVisitAllowed(cpr) {
    if (!this.access || !this.access.readVisit) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Visit', ['Read']);
  }

  isDeleteVisitAllowed(cpr) {
    if (!this.access || !this.access.deleteVisit) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Visit', ['Delete']);
  }

  isReadSprAllowed(cpr) {
    if (!this.isReadVisitAllowed(cpr) || !this.access || !this.access.readSpr) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'SurgicalPathologyReport', ['Read']);
  }

  isUpdateSprAllowed(cpr) {
    if (!this.isUpdateVisitAllowed(cpr) || !this.access || !this.access.updateSpr) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'SurgicalPathologyReport', ['Update']);
  }

  isDeleteSprAllowed(cpr) {
    if (!this.isDeleteVisitAllowed(cpr) || !this.access || !this.access.deleteSpr) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'SurgicalPathologyReport', ['Delete']);
  }

  isLockSprAllowed(cpr) {
    if (!this.isUpdateVisitAllowed(cpr) || !this.access || !this.access.lockSpr) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'SurgicalPathologyReport', ['Lock']);
  }

  isUnlockSprAllowed(cpr) {
    if (!this.isUpdateVisitAllowed(cpr) || !this.access || !this.access.unlockSpr) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'SurgicalPathologyReport', ['Unlock']);
  }

  isCreateSpecimenAllowed(cpr) {
    if (!this.access || !this.access.createPrimarySpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, ['PrimarySpecimen', 'Specimen'], ['Create']);
  }

  isCreateAllSpecimenAllowed(cpr) {
    if (!this.access || !this.access.createAllSpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Specimen', ['Create']);
  }

  isReadSpecimenAllowed(cpr) {
    if (!this.access || !this.access.readSpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, ['PrimarySpecimen', 'Specimen'], ['Read']);
  }

  isUpdateSpecimenAllowed(cpr) {
    if (!this.access || !this.access.updatePrimarySpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, ['PrimarySpecimen', 'Specimen'], ['Update']);
  }

  isUpdateAllSpecimenAllowed(cpr) {
    if (!this.access || !this.access.updateAllSpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Specimen', ['Update']);
  }

  isDeleteSpecimenAllowed(cpr) {
    if (!this.access || !this.access.deleteSpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, ['PrimarySpecimen', 'Specimen'], ['Delete']);
  }

  isDeleteAllSpecimenAllowed(cpr) {
    if (!this.access || !this.access.deleteAllSpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Specimen', ['Delete']);
  }

  isPrintSpecimenAllowed(cpr) {
    if (!this.access || !this.access.readAllSpecimen) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Specimen', ['Read']);
  }

  isCreatePrimarySpecimenAllowed() {
    return this.access && this.access.createPrimarySpecimen;
  }

  isReadConsentAllowed(cpr) {
    if (!this.access || !this.access.readConsent) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Consent', ['Read']);
  }

  isUpdateConsentAllowed(cpr) {
    if (!this.access || !this.access.updateConsent) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Consent', ['Update']);
  }

  isDeleteConsentAllowed(cpr) {
    if (!this.access || !this.access.deleteConsent) {
      return false;
    }

    return this._isAccessBasedOnMrnAllowed(cpr, 'Consent', ['Delete']);
  }

  async getReceiveSpecimensWf() {
    return this._getWorkflow('receiveSpecimensWorkflow');
  }

  async getRapidCollectionWf() {
    return this._getWorkflow('rapidCollectionWorkflow');
  }

  _getMatchingForms(forms, rules, context) {
    let matchingRule = null;
    for (let rule of rules) {
      if (!rule.when) {
        continue;
      }

      try {
        if (exprUtil.eval(context, rule.when)) {
          matchingRule = rule;
          break;
        }
      } catch (err) {
        alert('Invalid form data entry rule expression: ' + rule.when + ': ' + err);
      }
    }

    if (matchingRule && matchingRule.forms) {
      forms = forms.filter(({formName}) => matchingRule.forms.indexOf(formName) > -1);
    }

    return forms;
  }

  _sortForms(forms, orderSpec) {
    if (!orderSpec || orderSpec.length == 0) {
      return forms;
    }

    const formsByType = {};
    for (let form of forms) {
      formsByType[form.entityType] = formsByType[form.entityType] || [];
      formsByType[form.entityType].push(form);
    }

    const result = [];
    for (let {type, forms: typeForms} of orderSpec) {
      Array.prototype.push.apply(result, this._sortForms0(formsByType[type] || [], typeForms));
      delete formsByType[type];
    }

    for (let form of forms) {
      if (formsByType[form.entityType]) {
        result.push(form);
      }
    }

    return result;
  }

  _sortForms0(forms, orderSpec) {
    const formsById = {};
    for (let form of forms) {
      formsById[form.formId] = form;
    }

    const result = [];
    for (let {id} of orderSpec) {
      let form = formsById[id];
      if (form) {
        result.push(form);
        forms.splice(forms.indexOf(form), 1);
      }
    }

    Array.prototype.push.apply(result, forms);
    return result;
  }

  _getVisitUrl(visit) {
    const {cpId, eventId, cprId, id} = visit;
    const route = routerSvc.getCurrentRoute();
    const params = { cpId, cprId, visitId: id || -1};

    if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
      return routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', params, {eventId});
    } else {
      return routerSvc.getUrl('VisitDetail.Overview', params, {eventId});
    }
  }

  _loadAccessRights() {
    this.access = {
      createParticipant: this._isAllowed('ParticipantPhi', ['Create']),

      updateParticipant: this._isAllowed('ParticipantPhi', ['Update']),

      deleteParticipant: this._isAllowed('ParticipantPhi', ['Delete']),

      participantExim: this._isAllowed('ParticipantPhi', ['Export Import']),

      createVisit: this._isAllowed('Visit', ['Create']),

      createUpdateVisit: this._isAllowed('Visit', ['Create', 'Update']),

      updateVisit: this._isAllowed('Visit', ['Update']),

      readVisit: this._isAllowed('Visit', ['Read']),

      deleteVisit: this._isAllowed('Visit', ['Delete']),

      visitExim: this._isAllowed('Visit', ['Export Import']),

      readSpr: this._isAllowed('SurgicalPathologyReport', ['Read']),

      updateSpr: this._isAllowed('SurgicalPathologyReport', ['Update']),

      deleteSpr: this._isAllowed('SurgicalPathologyReport', ['Delete']),

      lockSpr: this._isAllowed('SurgicalPathologyReport', ['Lock']),

      unlockSpr: this._isAllowed('SurgicalPathologyReport', ['Unlock']),

      createPrimarySpecimen: this._isAllowed(['PrimarySpecimen', 'Specimen'], ['Create']),

      updatePrimarySpecimen: this._isAllowed(['PrimarySpecimen', 'Specimen'], ['Update']),

      readSpecimen: this._isAllowed(['PrimarySpecimen', 'Specimen'], ['Read']),

      deleteSpecimen: this._isAllowed(['PrimarySpecimen', 'Specimen'], ['Delete']),

      createAllSpecimen: this._isAllowed('Specimen', ['Create']),

      updateAllSpecimen: this._isAllowed('Specimen', ['Update']),

      readAllSpecimen: this._isAllowed('Specimen', ['Read']),

      deleteAllSpecimen: this._isAllowed('Specimen', ['Delete']),

      specimenExim: this._isAllowed('Specimen', ['Export Import']) || this._isAllowed('PrimarySpecimen', ['Export Import']),

      readConsent: this._isAllowed('Consent', ['Read']),

      updateConsent: this._isAllowed('Consent', ['Update']),

      deleteConsent: this._isAllowed('Consent', ['Delete']),
    }
  }

  _isAllowed(resources, operations, sites) {
    if (typeof resources == 'string') {
      resources = [resources];
    }

    return authSvc.isAllowed({resources, operations, cp: this.cp.shortTitle, sites: sites || this.cpSites});
  }

  _isAccessBasedOnMrnAllowed(cpr, resources, operations) {
    if (!this.accessBasedOnMrn || !cpr || !cpr.participant || (cpr.participant.pmis || []).length == 0) {
      return true;
    }

    const mrnSites = cpr.participant.pmis.map(({siteName}) => siteName);
    return this._isAllowed(resources, operations, mrnSites);
  }

  async _getWorkflow(propName) {
    if (!this.wfIds[propName]) {
      const {tmWfSvc} = window.osSvc;
      if (!tmWfSvc) {
        this.wfIds[propName] = -1;
      } else {
        const wfName = await cpSvc.getWorkflowProperty(this.cpId, 'common', propName);
        if (wfName) {
          const {id: wfId} = await tmWfSvc.getWorkflowByName(wfName);
          this.wfIds[propName] = wfId;
        } else {
          this.wfIds[propName] = -1;
        }
      }
    }

    return this.wfIds[propName] == -1 ? null : this.wfIds[propName];
  }
}
