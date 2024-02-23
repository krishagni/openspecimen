
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc from '@/biospecimen/services/Cpr.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import visitSvc from '@/biospecimen/services/Visit.js';
import settingSvc from '@/common/services/Setting.js';
import util from '@/common/services/Util.js';

import matchingTab from '@/biospecimen/schemas/participants/matching-participants.js';

export default class CpViewContext {
  cpId = null;

  cpQ = null;

  cprDictQ = null;

  visitDictQ = null;

  visitsTabQ = null;

  specimenDictQ = null;

  constructor(cpId) {
    this.cpId = cpId;
  }

  getCp() {
    if (!this.cpQ) {
      this.cpQ = cpSvc.getCpById(this.cpId);
    }

    return this.cpQ;
  }

  getCpEvents() {
    if (!this.cpEventsQ) {
      this.cpEventsQ = cpSvc.getCpes(this.cpId)
        .then(events => events.filter(event => event.activityStatus == 'Active'));
    }

    return this.cpEventsQ;
  }

  getCprDict() {
    if (!this.cprDictQ) {
      this.cprDictQ = cprSvc.getDict(this.cpId);
    }

    return this.cprDictQ;
  }

  getCprAddEditLayout() {
    return this.getCprDict().then(dict => cprSvc.getLayout(this.cpId, dict));
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

  getVisitDict() {
    if (!this.visitDictQ) {
      this.visitDictQ = visitSvc.getDict(this.cpId);
    }

    return this.visitDictQ;
  }

  async getVisitAddEditLayout() {
    return this.getVisitDict().then(dict => visitSvc.getLayout(this.cpId, dict));
  }

  getVisitsTab() {
    if (!this.visitsTabQ) {
      this.visitsTabQ = visitSvc.getVisitsTab(this.cpId);
    }

    return this.visitsTabQ;
  }

  getOccurredVisitsTabFields() {
    return this.getVisitsTab().then(
      (visitsTab) => {
        const tabFields = visitsTab.occurred || [];
        if (tabFields.length == 0) {
          Array.prototype.push.apply(tabFields, visitSvc.getDefaultOccurredVisitsTabFields());
        }

        tabFields.push({
          type: 'component',
          captionCode: 'visits.collection_stats',
          component: 'os-visit-specimen-collection-stats',
          data: (rowObject) => rowObject,
          uiStyle: {
            width: '20%'
          }
        });

        tabFields.push({
          type: 'component',
          captionCode: 'visits.utilisation_stats',
          component: 'os-visit-specimen-utilisation-stats',
          data: (rowObject) => rowObject,
          uiStyle: {
            width: '20%'
          }
        });

        return tabFields;
      }
    );
  }

  getMissedVisitsTabFields() {
    return this.getVisitsTab().then(
      (visitsTab) => {
        const tabFields = visitsTab.missed || [];
        if (tabFields.length == 0) {
          Array.prototype.push.apply(tabFields, visitSvc.getDefaultMissedVisitsTabFields());
        }

        return tabFields;
      }
    );
  }

  getPendingVisitsTabFields() {
    return this.getVisitsTab().then(
      (visitsTab) => {
        const tabFields = visitsTab.pending || [];
        if (tabFields.length == 0) {
          Array.prototype.push.apply(tabFields, visitSvc.getDefaultPendingVisitsTabFields());
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
    const cp = await this.getCp();
    if (cp.storeSprEnabled == null || cp.storeSprEnabled == undefined) {
      return settingSvc.getSetting('biospecimen', 'store_spr')
        .then(settings => util.isTrue(settings[0].value));
    }

    return cp.storeSprEnabled;
  }

  getSpecimenDict() {
    if (!this.specimenDictQ) {
      this.specimenDictQ = specimenSvc.getDict(this.cpId);
    }

    return this.specimenDictQ;
  }

  async getSpecimenAddEditLayout() {
    return this.getSpecimenDict().then(dict => specimenSvc.getLayout(this.cpId, dict));
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
}
