
import cpSvc from './CollectionProtocol.js';
import http from '@/common/services/HttpClient.js';
import formSvc  from '@/forms/services/Form.js';
import formUtil from '@/common/services/FormUtil.js';
import util     from '@/common/services/Util.js';

import visitSchema   from '@/biospecimen/schemas/visits/visit.js';
import addEditLayout from '@/biospecimen/schemas/visits/addedit.js';

class Visit {
  async getVisits(cpr, includeStats = true, sortByDates) {
    if (!cpr.$visitsQ) {
      cpr.$visitsQ = http.get('visits', {cprId: cpr.id, includeStats, sortByDates});
    }

    return cpr.$visitsQ.then(
      (visits) => {
        for (let visit of visits) {
          visit.cprId = cpr.id;
          visit.totalPrimarySpmns = visit.pendingPrimarySpmns + visit.plannedPrimarySpmnsColl +
            visit.uncollectedPrimarySpmns + visit.unplannedPrimarySpmnsColl;
          visit.reqStorage = visit.storedSpecimens + visit.notStoredSpecimens +
            visit.distributedSpecimens + visit.closedSpecimens;
        }

        return visits;
      }
    );
  }

  clearVisits(cpr) {
    cpr.$visitsQ = null;
  }

  async getVisit(visitId) {
    return http.get('visits/' + visitId);
  }

  async saveOrUpdate(visit) {
    if (visit.id > 0) {
      return http.put('visits/' + visit.id, visit);
    } else {
      return http.post('visits/', visit);
    }
  }

  async deleteVisit(visitId, forceDelete, reason) {
    return http.delete('visits/' + visitId, {}, {forceDelete, reason});
  }

  async getSpecimens(cprId, eventId, visitId) {
    return http.get('specimens', {cprId, eventId, visitId});
  }

  async getDependents(visit) {
    return http.get('visits/' + visit.id + '/dependent-entities');
  }

  async getDict(cpId) {
    return cpSvc.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        let visitFields = (dict.fields || []).filter(field => field.name.indexOf('visit.') == 0);
        if (visitFields.length > 0) {
          visitFields = formUtil.sdeFieldsToDict(visitFields);
        } else {
          visitFields = util.clone(visitSchema.fields);
        }

        if (visitFields.some(field => field.name.indexOf('visit.extensionDetail') == 0)) {
          return visitFields;
        }

        return this.getCustomFieldsForm(cpId).then(
          (formDef) => {
            let customFields = formUtil.deFormToDict(formDef, 'visit.extensionDetail.attrsMap.')
              .map(field => {
                field.showWhen = "visit.status != 'Missed Collection' && visit.status != 'Not Collected'";
                return field;
              });
            return visitFields.concat(customFields);
          }
        );
      }
    );
  }

  async getLayout(cpId, visitFields) {
    return cpSvc.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        const layout = util.clone(dict.layout || {});
        layout.rows = (layout.rows || [])
          .map(row => ({fields: row.fields.filter(field => field.name.indexOf('visit.') == 0)}))
          .filter(row => row.fields.length > 0);

        if (layout.rows == 0) {
          //
          // CP or system level dictionary has no layout
          //

          if (dict.fields && dict.fields.some(field => field.name.indexOf('visit.') == 0)) {
            //
            // CP or system level dictionary configured
            // use the dictionary to create a default layout
            //
            layout.rows = visitFields.map(field => ({fields: [ {name: field.name} ]}));
          } else {
            //
            // no dictionary configured, use the default layout shipped with the app
            //
            layout.rows = util.clone(addEditLayout.layout.rows);
          }
        }

        const rows = layout.rows;
        if (rows.some(row => row.fields.some(field => field.name.indexOf('visit.extensionDetail') == 0))) {
          //
          // layout has one or more custom fields. use it
          //
          return layout;
        }


        //
        // append the custom fields to the configured or default layout
        //
        const customFields = (visitFields || [])
          .filter(field => field.name.indexOf('visit.extensionDetail') == 0)
          .map(field => ({fields: [ {name: field.name} ]}));
        Array.prototype.push.apply(layout.rows, customFields);
        return layout;
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
