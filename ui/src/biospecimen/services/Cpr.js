
import cpSvc    from '@/biospecimen/services/CollectionProtocol.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import formSvc  from '@/forms/services/Form.js';
import http     from '@/common/services/HttpClient.js';

import cprSchema from '@/biospecimen/schemas/participants/cpr.js';
import addEditLayout from '@/biospecimen/schemas/participants/addedit.js';

class CollectionProtocolRegistration {

  workflows = {};

  async getCpr(cprId) {
    return http.get('collection-protocol-registrations/' + cprId);
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
}

export default new CollectionProtocolRegistration();
