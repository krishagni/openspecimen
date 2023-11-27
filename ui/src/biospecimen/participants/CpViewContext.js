
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc from '@/biospecimen/services/Cpr.js';
import visitSvc from '@/biospecimen/services/Visit.js';

export default class CpViewContext {
  cpId = null;

  cpQ = null;

  cprDictQ = null;

  visitsTabQ = null;

  constructor(cpId) {
    this.cpId = cpId;
  }

  getCp() {
    if (!this.cpQ) {
      this.cpQ = cpSvc.getCpById(this.cpId);
    }

    return this.cpQ;
  }

  getCprDict() {
    if (!this.cprDictQ) {
      this.cprDictQ = cprSvc.getDict(this.cpId);
    }

    return this.cprDictQ;
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
}
