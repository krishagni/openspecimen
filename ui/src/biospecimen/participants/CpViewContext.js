
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc from '@/biospecimen/services/Cpr.js';
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

  getVisitDict() {
    if (!this.visitDictQ) {
      this.visitDictQ = visitSvc.getDict(this.cpId);
    }

    return this.visitDictQ;
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
          data: (rowObject) => rowObject
        });

        tabFields.push({
          type: 'component',
          captionCode: 'visits.utilisation_stats',
          component: 'os-visit-specimen-utilisation-stats',
          data: (rowObject) => rowObject
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

  getSpecimenDict() {
    if (!this.specimenDictQ) {
      this.specimenDictQ = specimenSvc.getDict(this.cpId);
    }

    return this.specimenDictQ;
  }
}
