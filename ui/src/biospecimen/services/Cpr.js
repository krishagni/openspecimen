
import cpSvc    from '@/biospecimen/services/CollectionProtocol.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import formSvc  from '@/forms/services/Form.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';

import cprSchema from '@/biospecimen/schemas/participants/cpr.js';
import addEditLayout from '@/biospecimen/schemas/participants/addedit.js';

const PHI_FIELDS = [
  'cpr.participant.firstName', 'cpr.participant.middleName', 'cpr.participant.lastName',
  'cpr.participant.emailAddress', 'cpr.participant.emailOptIn',
  'cpr.participant.phoneNumber', 'cpr.participant.textOptIn',
  'cpr.participant.birthDate', 'cpr.participant.birthDateStr', 'cpr.participant.deathDate',
  'cpr.participant.empi', 'cpr.participant.uid', 'cpr.participant.pmis'
];

class CollectionProtocolRegistration {

  workflows = {};

  async getCpr(cprId) {
    return http.get('collection-protocol-registrations/' + cprId);
  }

  async getCprs(cprIds) {
    return http.get('collection-protocol-registrations', {id: cprIds});
  }

  async getMatchingParticipants(cpr) {
    cpr = cpr || {};
    const participant = cpr.participant || {};
    return http.post('participants/match', participant).then(
      (matches) => {
        if (!participant.id) {
          return matches;
        }

        return matches.filter(match => match.participant.id != participant.id);
      }
    );
  }

  async getParticipant(participantId) {
    return http.get('participants/' + participantId);
  }

  async saveOrUpdate(cpr) {
    if (cpr.id > 0) {
      return http.put('collection-protocol-registrations/' + cpr.id, cpr);
    } else {
      return http.post('collection-protocol-registrations/', cpr);
    }
  }

  async bulkUpdate(cprIds, cpr) {
    const payload = {ids: cprIds, detail: cpr};
    return http.put('collection-protocol-registrations/bulk-update', payload);
  }

  async deleteCpr(cprId, forceDelete, reason) {
    return http.delete('collection-protocol-registrations/' + cprId, {}, {forceDelete, reason});
  }

  async bulkDelete(cprIds, reason) {
    return http.delete('collection-protocol-registrations', {}, {id: cprIds, forceDelete: true, reason: reason})
  }

  async anonymize(cpr) {
    return http.put('collection-protocol-registrations/' + cpr.id + '/anonymize', cpr);
  }

  async getDependents(cpr) {
    return http.get('collection-protocol-registrations/' + cpr.id + '/dependent-entities');
  }

  async getDict(cpId) {
    return cpSvc.getDictFor(cpId, ['cpr', 'calcCpr'], 'cpr.participant.extensionDetail', cprSchema, this.getCustomFieldsForm);
  }

  async getLayout(cpId, cprFields) {
    return cpSvc.getLayoutFor(cpId, 'cpr', 'cpr.participant.extensionDetail', addEditLayout.layout, cprFields);
  }

  async getCustomFieldsForm(cpId) {
    return http.get('collection-protocol-registrations/extension-form', { cpId }).then(
      (resp) => {
        if (!resp || !resp.formId) {
          return null;
        }

        return formSvc.getDefinition(resp.formId);
      }
    );
  }

  getDefaultLookupFields(fields) {
    const defFields = [
      'cpr.participant.empi', 'cpr.participant.uid', 'cpr.participant.pmis',
      'cpr.participant.lastName', 'cpr.participant.birthDate'
    ];
    return fields.filter(field => defFields.indexOf(field.name) >= 0);
  }

  getDeidentifiedFields(fields) {
    return fields.map(
      (field) => {
        if (field.phi || PHI_FIELDS.indexOf(field.name) >= 0) {
          return null;
        }

        field = util.clone(field);
        if (field.fields) {
          field.fields = this.getDeidentifiedFields(field.fields);
        }

        return !field.fields || field.fields.length > 0 ? field : null;
      }
    ).filter(field => !!field);
  }

  getDeidentifiedLayout(layout, fields) {
    const fieldNames = new Set(fields.map(field => field.name));
    layout = util.clone(layout);
    layout.rows = (layout.rows || []).map(
      row => ({...row, fields: row.fields.filter(field => fieldNames.has(field.name))})
    ).filter(row => row.fields.length > 0);

    return layout;
  }

  removePhi(cpr, fields) {
    const context = {cpr};
    PHI_FIELDS.forEach(name => this._deleteValue(context, name));
    this._removePhiFields(context, fields || []);

    return cpr;
  }

  getFormattedTitle(cpr) {
    cpr = cpr || {};

    let name = '';
    const participant = cpr.participant;
    if (participant.firstName && participant.firstName.indexOf('###') != 0) {
      name = participant.firstName;
    }

    if (participant.middleName && participant.middleName.indexOf('###') != 0) {
      if (name) {
        name += ' ';
      }

      name += participant.middleName;
    }

    if (participant.lastName && participant.lastName.indexOf('###') != 0) {
      if (name) {
        name += ' ';
      }

      name += participant.lastName;
    }

    name = cpr.ppid + (name ? ' (' + name + ')' : '');
    return name;
  }

  async getFormDataEntryRules(cpId) {
    return cpSvc.getWorkflow(cpId, 'formDataEntryRules').then(wf => (wf && wf['participant']) || []);
  }

  async getFormsOrderSpec(cpId) {
    return cpSvc.getWorkflow(cpId, 'forms').then(
      wf => {
        if (!wf) {
          wf = {};
        }

        return [
          {type: 'CommonParticipant', forms: wf['CommonParticipant'] || []},
          {type: 'Participant', forms: wf['Participant'] || []}
        ];
      }
    );
  }

  async getForms(cpr) {
    if (!cpr || !cpr.id) {
      return [];
    }

    return http.get('collection-protocol-registrations/' + cpr.id + '/forms');
  }

  getFormRecords(cpr) {
    return http.get('collection-protocol-registrations/' + cpr.id + '/extension-records').then(
      (formRecords) => {
        const result = [];
        for (let {id, caption, records} of formRecords) {
          for (let record of records || []) {
            record.formId = id;
            record.formCaption = caption;
            result.push(record);
          }
        }

        result.sort(({updateTime: t1}, {updateTime: t2}) => +t2 - +t1);
        return result;
      }
    );
  }

  getAllowedEvents(cpr, anticipatedEvents) {
    if (!anticipatedEvents) {
      return null;
    }

    const {rules, matchType} = anticipatedEvents;
    if (!rules || rules.length == 0) {
      return null;
    }

    let result = null;
    for (let spec of anticipatedEvents.rules) {
      if (!spec.rule || spec.rule == 'any' || spec.rule == '*' || exprUtil.eval({cpr}, spec.rule)) {
        result = result || [];
        if (matchType == 'any') {
          result = spec.events;
          break;
        } else if (matchType == 'all') {
          for (let event of spec.events) {
            if (result.indexOf(event) == -1) {
              result.push(event);
            }
          }
        }
      }
    }

    return result;
  }

  getConsents(cpr) {
    if (!cpr.$consentsQ) {
      if (window.osSvc.ecDocRespSvc) {
        cpr.$consentsQ = window.osSvc.ecDocRespSvc.getConsents(cpr.id);
      } else {
        cpr.$consentsQ = http.get('collection-protocol-registrations/' + cpr.id + '/consents');
      }
    }

    return cpr.$consentsQ;
  }

  updateConsents(cpr, consent) {
    return http.put('collection-protocol-registrations/' + cpr.id + '/consents', consent).then(
      savedConsent => {
        cpr.$consentsQ = null;
        return savedConsent;
      }
    );
  }

  deleteConsentDoc(cpr) {
    return http.delete('collection-protocol-registrations/' + cpr.id + '/consent-form').then(
      status => {
        cpr.$consentsQ = null;
        return status;
      }
    );
  }

  getConsentDocUrl({id: cprId}) {
    return http.getUrl('collection-protocol-registrations/' + cprId + '/consent-form');
  }

  getSpecimens(cpId, cprId) {
    return http.get('specimens', {cpId, cprId, includeExtensions: true});
  }

  _removePhiFields(object, fields) {
    fields.forEach(
      field => {
        if (field.phi) {
          this._deleteValue(object, field.name);
        } else if (field.fields) {
          const value = util.getValue(object, field.name);
          (value || []).forEach(row => this._removePhiFields(row, field.fields));
        }
      }
    );
  }

  _deleteValue(object, name) {
    const path = name.split('.');
    const property = path.pop();
    const parent = util.getValue(object, path.join('.'));
    if (parent) {
      delete parent[property];
    }
  }
}

export default new CollectionProtocolRegistration();
