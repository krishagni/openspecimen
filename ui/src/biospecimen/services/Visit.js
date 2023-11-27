
import cpSvc from './CollectionProtocol.js';
import http from '@/common/services/HttpClient.js';
import formSvc  from '@/forms/services/Form.js';
import formUtil from '@/common/services/FormUtil.js';
import util     from '@/common/services/Util.js';

class Visit {
  async getVisits(cpr, includeStats = true, sortByDates) {
    if (!cpr.$visitsQ) {
      cpr.$visitsQ = http.get('visits', {cprId: cpr.id, includeStats, sortByDates});
    }

    return cpr.$visitsQ.then(
      (visits) => {
        for (let visit of visits) {
          visit.totalPrimarySpmns = visit.pendingPrimarySpmns + visit.plannedPrimarySpmnsColl +
            visit.uncollectedPrimarySpmns + visit.unplannedPrimarySpmnsColl;
          visit.reqStorage = visit.storedSpecimens + visit.notStoredSpecimens +
            visit.distributedSpecimens + visit.closedSpecimens;
        }

        return visits;
      }
    );
  }

  async getVisit(visitId) {
    return http.get('visits/' + visitId);
  }

  async getDict(cpId) {
    return cpSvc.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        let visitFields = (dict.fields || []).filter(field => field.name.indexOf('visit.') == 0);
        if (visitFields.length > 0) {
          visitFields = formUtil.sdeFieldsToDict(visitFields);
        } else {
          visitFields = []; // util.clone(cprSchema.fields);
        }

        if (visitFields.some(field => field.name.indexOf('visit.extensionDetail') == 0)) {
          return visitFields;
        }

        return this.getCustomFieldsForm(cpId).then(
          (formDef) => {
            let customFields = formUtil.deFormToDict(formDef, 'visit.extensionDetail.attrsMap.');
            return visitFields.concat(customFields);
          }
        );
      }
    );
  }

  async getCustomFieldsForm(cpId) {
    return http.get('visits/extension-form', { cpId }).then(
      (resp) => {
        if (!resp || !resp.formId) {
          return null;
        }

        return formSvc.getDefinition(resp.formId);
      }
    );
  }

  async getVisitsTab(cpId) {
    const cpCfgQ    = cpSvc.getWorkflow(cpId, 'visitsTab');
    const allCpCfgQ = cpSvc.getWorkflow(-1, 'visitsTab');
    const dictQ     = this.getDict(cpId);
    return Promise.all([allCpCfgQ, cpCfgQ, dictQ]).then(
      ([sysVisitsTab, cpVisitsTab, dict]) => {
        const result = util.clone(sysVisitsTab || {});
        Object.assign(result, util.clone(cpVisitsTab || {}));

        if (result.occurred) {
          result.occurred = formUtil.sdeFieldsToDict(result.occurred, dict);
        }

        if (result.missed) {
          result.missed = formUtil.sdeFieldsToDict(result.missed, dict);
        }

        if (result.pending) {
          result.pending = formUtil.sdeFieldsToDict(result.pending, dict);
        }

        return result;
      }
    );
  }

  getDefaultOccurredVisitsTabFields() {
    return [
      {
        name: 'visit.description',
        captionCode: 'visits.event',
        type: 'component',
        component: 'os-visit-event-desc',
        data: ({visit}) => ({visit, showLink: true})
      },
      {
        name: 'visit.name',
        captionCode: 'visits.name'
      },
      {
        name: 'visit.visitDate',
        captionCode: 'visits.date',
        type: 'date'
      }
    ];
  }

  getDefaultMissedVisitsTabFields() {
    return [
      {
        name: 'visit.description',
        captionCode: 'visits.event',
        type: 'component',
        component: 'os-visit-event-desc',
        data: ({visit}) => ({visit, showLink: true})
      },
      {
        name: 'visit.visitDate',
        captionCode: 'visits.date',
        type: 'date'
      },
      {
        name: 'visit.missedReason',
        captionCode: 'visits.reason'
      }
    ];
  }

  getDefaultPendingVisitsTabFields() {
    return [
      {
        name: 'visit.description',
        captionCode: 'visits.event',
        type: 'component',
        component: 'os-visit-event-desc',
        data: ({visit}) => ({visit, showLink: true})
      },
      {
        name: 'visit.anticipatedVisitDate',
        captionCode: 'visits.date',
        type: 'date'
      },
      {
        name: 'visit.pendingPrimarySpmns',
        captionCode: 'visits.pending_specimens'
      }
    ];
  }
}

export default new Visit();
