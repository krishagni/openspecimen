
import cpSvc    from '@/biospecimen/services/CollectionProtocol.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import formSvc  from '@/forms/services/Form.js';
import formUtil from '@/common/services/FormUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';

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
    return cpSvc.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        let cprFields = (dict.fields || []).filter(field => field.name.indexOf('cpr.') == 0);
        if (cprFields.length > 0) {
          cprFields = formUtil.sdeFieldsToDict(cprFields);
        } else {
          cprFields = util.clone(cprSchema.fields);
        }

        if (cprFields.some(field => field.name.indexOf('cpr.participant.extensionDetail') == 0)) {
          return cprFields;
        }

        return this.getCustomFieldsForm(cpId).then(
          (formDef) => {
            let customFields = formUtil.deFormToDict(formDef, 'cpr.participant.extensionDetail.attrsMap.');
            return cprFields.concat(customFields);
          }
        );
      }
    );
  }

  async getLayout(cpId, cprFields) {
    return cpSvc.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        const layout = util.clone(dict.layout || {});
        layout.rows = (layout.rows || [])
          .map(row => ({fields: row.fields.filter(field => field.name.indexOf('cpr.') == 0)}))
          .filter(row => row.fields.length > 0);

        if (layout.rows == 0) {
          //
          // CP or system level dictionary has no layout
          //

          if (dict.fields && dict.fields.some(field => field.name.indexOf('cpr.') == 0)) {
            //
            // CP or system level dictionary configured
            // use the dictionary to create a default layout
            //
            layout.rows = cprFields.map(field => ({fields: [ {name: field.name} ]}));
          } else {
            //
            // no dictionary configured, use the default layout shipped with the app
            //
            layout.rows = util.clone(addEditLayout.layout.rows);
          }
        }

        const rows = layout.rows;
        if (rows.some(row => row.fields.some(field => field.name.indexOf('cpr.participant.extensionDetail') == 0))) {
          //
          // layout has one or more custom fields. use it
          //
          return layout;
        }


        //
        // append the custom fields to the configured or default layout
        //
        const customFields = (cprFields || [])
          .filter(field => field.name.indexOf('cpr.participant.extensionDetail') == 0)
          .map(field => ({fields: [ {name: field.name} ]}));
        Array.prototype.push.apply(layout.rows, customFields);
        return layout;
      }
    );
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

  getForms(cpr) {
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

  getConsents({id: cprId}) {
    return http.get('collection-protocol-registrations/' + cprId + '/consents');
  }

  updateConsents({id: cprId}, consent) {
    return http.put('collection-protocol-registrations/' + cprId + '/consents', consent);
  }

  deleteConsentDoc({id: cprId}) {
    return http.delete('collection-protocol-registrations/' + cprId + '/consent-form');
  }

  getConsentDocUrl({id: cprId}) {
    return http.getUrl('collection-protocol-registrations/' + cprId + '/consent-form');
  }
}

export default new CollectionProtocolRegistration();
