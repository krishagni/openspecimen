
import cpSchema      from '@/biospecimen/schemas/cps/cp.js';
import addEditLayout from '@/biospecimen/schemas/cps/addedit.js';
import publishForm   from '@/biospecimen/schemas/cps/publish.js';

import eventAddEditLayout from '@/biospecimen/schemas/cps/event-addedit.js';

import reqAddEditLayout   from '@/biospecimen/schemas/cps/req-addedit.js';
import reqSchema          from '@/biospecimen/schemas/cps/req.js';

import authSvc from '@/common/services/Authorization.js';
import exportSvc from '@/common/services/ExportService.js';
import formSvc  from '@/forms/services/Form.js';
import formUtil from '@/common/services/FormUtil.js';
import http from '@/common/services/HttpClient.js';
import i18n from '@/common/services/I18n.js';
import util from '@/common/services/Util.js';

import ui from '@/global.js';

class CollectionProtocol {

  workflows = {};

  async getCpById(cpId) {
    return http.get('collection-protocols/' + cpId);
  }

  async getCps(filterOpts) {
    return http.get('collection-protocols', filterOpts || {});
  }

  async getCpsForRegistrations(sites, query) {
    const filterOpts = {
      siteName: sites,
      resource: 'ParticipantPhi',
      op: 'Create',
      title: query
    };

    return http.get('collection-protocols/byop', filterOpts || {});
  }

  async saveOrUpdate(cp) {
    if (cp.id > 0) {
      return http.put('collection-protocols/' + cp.id, cp);
    } else {
      return http.post('collection-protocols', cp);
    }
  }

  async getDependents(cp) {
    return http.get('collection-protocols/' + cp.id + '/dependent-entities');
  }

  async deleteCp(cpId, forceDelete, reason) {
    return http.delete('collection-protocols/' + cpId, {}, {forceDelete, reason});
  }

  async deleteCps(cpIds, reason) {
    return http.delete('collection-protocols', {}, {id: cpIds, forceDelete: true, reason});
  }

  async copy(cpId, cp) {
    return http.post('collection-protocols/' + cpId + '/copy', cp);
  }

  async publish(cpId, publishDetail) {
    return http.post('collection-protocols/' + cpId + '/published-events', publishDetail);
  }

  async starCp(cpId) {
    return http.post('collection-protocols/' + cpId + '/labels', {});
  }

  async unstarCp(cpId) {
    return http.delete('collection-protocols/' + cpId + '/labels');
  }

  async getCpes(cpId) {
    return http.get('collection-protocol-events', {cpId});
  }

  async getCpesByCpShortTitle(cpShortTitle) {
    return http.get('collection-protocol-events', {cpShortTitle});
  }


  async getCpe(eventId) {
    return http.get('collection-protocol-events/' + eventId);
  }

  getEventDescription(event) {
    let result = '';
    if (event.eventLabel) {
      if (event.eventPoint != null && event.eventPoint != undefined) {
        result += ((event.eventPoint < 0) ? '-' : '') + 'T' + Math.abs(event.eventPoint);
        result += event.eventPointUnit.charAt(0) + ': ';
      }

      result += event.eventLabel;
      if (event.eventCode || event.code) {
        result += ' (' + (event.eventCode || event.code) + ')';
      }
    } else {
      result = i18n.msg('visits.unplanned_visit');
    }

    return result;
  }

  saveOrUpdateCpe(cpe, copyOf) {
    if (copyOf > 0) {
      return http.post('collection-protocol-events/' + copyOf + '/copy', cpe);
    } else if (cpe.id > 0) {
      return http.put('collection-protocol-events/' + cpe.id, cpe);
    } else {
      return http.post('collection-protocol-events', cpe);
    }
  }

  deleteCpe(cpe) {
    return http.delete('collection-protocol-events/' + cpe.id);
  }

  async getSpecimenRequirements(cpId, eventId, includeChildrenReqs) {
    return http.get('specimen-requirements', {cpId, eventId, includeChildReqs: includeChildrenReqs});
  }

  async getSpecimenRequirement(reqId) {
    return http.get('specimen-requirements/' + reqId);
  }

  async saveOrUpdateSpecimenRequirement(sr) {
    if (sr.id > 0) {
      return http.put('specimen-requirements/' + sr.id, sr);
    } else {
      return http.post('specimen-requirements', sr);
    }
  }

  async createDerivedRequirement(parentReqId, sr) {
    return http.post('specimen-requirements/' + parentReqId + '/derived', sr);
  }

  async createAliquots(parentReqId, sr) {
    return http.post('specimen-requirements/' + parentReqId + '/aliquots', sr);
  }

  async copyRequirement(copyOf) {
    return http.post('specimen-requirements/' + copyOf + '/copy');
  }

  async deleteRequirement(req) {
    return http.delete('specimen-requirements/' + req.id);
  }

  async getConsentTiers(cpId) {
    return http.get('collection-protocols/' + cpId + '/consent-tiers');
  }

  async saveOrUpdateConsentTier(tier) {
    if (tier.id > 0) {
      return http.put('collection-protocols/' + tier.cpId + '/consent-tiers/' + tier.id, tier);
    } else {
      return http.post('collection-protocols/' + tier.cpId + '/consent-tiers', tier);
    }
  }

  async deleteConsentTier(tier) {
    return http.delete('collection-protocols/' + tier.cpId + '/consent-tiers/' + tier.id);
  }

  async getConsentTierDependents(tier) {
    return http.get('collection-protocols/' + tier.cpId + '/consent-tiers/' + tier.id + '/dependent-entities');
  }

  async waiveConsents(cpId) {
    return http.put('collection-protocols/' + cpId + '/consents-waived', {consentsWaived: true});
  }

  async undoWaiveConsents(cpId) {
    return http.put('collection-protocols/' + cpId + '/consents-waived', {consentsWaived: false});
  }

  async setConsentsCp(cpId, source) {
    return http.put('collection-protocols/' + cpId + '/consents-source', source);
  }

  async getForms(cpId, entityTypes) {
    return http.get('collection-protocols/' + cpId + '/forms', {entityType: entityTypes || []});
  }

  async saveWorkflow(cpId, wfName, wfData) {
    return http.patch('collection-protocols/' + cpId + '/workflows', [{name: wfName, data: wfData}]).then(
      ({workflows}) => {
        this.workflows[cpId] = workflows;
        return (workflows[wfName] && workflows[wfName].data) || {}
      }
    );
  }

  async getWorkflow(cpId, wfName) {
    let workflow = await this.loadWorkflows(cpId, wfName);
    if (workflow && workflow.data) {
      if (workflow.data instanceof Array) {
        return workflow.data.length > 0 ? workflow.data : null;
      } else if (Object.keys(workflow.data).length > 0) {
        return workflow.data;
      }
    }

    if (cpId == -1) {
      return null;
    }

    workflow = await this.loadWorkflows(-1, wfName);
    return workflow && workflow.data;
  }

  async getWorkflowProperty(cpId, wfName, propName) {
    const wf = await this.getWorkflow(cpId, wfName);
    if (wf && (wf[propName] || wf[propName] == 'false' || wf[propName] == false)) {
      return wf[propName];
    } else if (cpId == -1 || !cpId) {
      return null;
    }

    const sysWf = await this.getWorkflow(-1, wfName);
    return sysWf && sysWf[propName];
  }

  async getDictFor(cpId, objAlias, customFieldsAlias, defSchema, customFieldsFormFn) {
    let objAliases = [];
    if (typeof objAlias == 'string') {
      objAliases.push(objAlias + '.');
    } else if (objAlias instanceof Array) {
      objAliases = objAlias.map(alias => alias + '.');
    }

    if (customFieldsAlias) {
      customFieldsAlias += '.attrsMap.';
    }

    defSchema = defSchema || {};
    defSchema.fields = defSchema.fields || [];
    return this.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        let fields = (dict.fields || []).filter(field => objAliases.some(alias => field.name.indexOf(alias) == 0));
        if (fields.length > 0) {
          fields = formUtil.sdeFieldsToDict(fields);
        } else {
          fields = util.clone(defSchema.fields);
        }

        if (fields.some(field => field.name.indexOf(customFieldsAlias) == 0) || !customFieldsFormFn) {
          return fields;
        }

        return customFieldsFormFn(cpId).then(
          (formDef) => {
            let customFields = formUtil.deFormToDict(formDef, customFieldsAlias);
            return fields.concat(customFields);
          }
        );
      }
    );
  }

  async getLayoutFor(cpId, objAlias, customFieldsAlias, defLayout, objFields) {
    objAlias += '.';
    if (customFieldsAlias) {
      customFieldsAlias += '.attrsMap.';
    }

    return this.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        const layout = util.clone(dict.layout || []);

        const result = {rows: []};
        for (let section of layout) {
          for (let row of section.rows) {
            const outputRow = row.filter(field => field.indexOf(objAlias) == 0)
              .map(field => ({name: field}));
            if (outputRow.length > 0) {
              result.rows.push({fields: outputRow});
            }
          }
        }

        if (result.rows.length == 0) {
          //
          // CP or system level dictionary has no layout
          //
          if (dict.fields && dict.fields.some(field => field.name.indexOf(objAlias) == 0)) {
            //
            // CP or system level dictionary configured
            // use the dictionary to create a default layout
            //
            result.rows = objFields.map(field => ({fields: [ {name: field.name} ]}));
          } else {
            //
            // no dictionary configured, use the default layout shipped with the app
            //
            result.rows = util.clone((defLayout && defLayout.rows) || []);
          }
        }

        const rows = result.rows;
        if (rows.some(row => row.fields.some(field => field.name.indexOf(customFieldsAlias) == 0))) {
          //
          // layout has one or more custom fields. use it
          //
          return result;
        }


        //
        // append the custom fields to the configured or default layout
        //
        const customFieldRows = {};
        let lastRow = 1000, lastCol = 1000;
        for (let field of (objFields || [])) {
          if (field.name.indexOf(customFieldsAlias) != 0) {
            continue;
          }

          let {row, column} = field;
          if (!row && row != 0) {
            row = lastRow++;
          }

          if (!column && column != 0) {
            column = lastCol++;
          }

          if (!customFieldRows[row]) {
            customFieldRows[row] = [];
          }

          customFieldRows[row][column] = {name: field.name};
        }

        for (let rowIdx of Object.keys(customFieldRows).sort((idx1, idx2) => +idx1 - +idx2)) {
          const fields = customFieldRows[rowIdx].filter(f => !!f);
          result.rows.push({fields});
        }

        return result;
      }
    );
  }

  async loadWorkflows(cpId, wfName) {
    let workflow = this.workflows[cpId];
    if (!workflow) {
      workflow = await http.get('collection-protocols/' + cpId + '/workflows');
      workflow = this.workflows[cpId] = workflow.workflows;
    }

    return workflow[wfName];
  }

  async getSpecimenTreeCfg(cpId) {
    return this.getWorkflow(-1, 'specimenTree').then(
      (sysTreeCfg) => {
        const result = Object.assign({}, sysTreeCfg || {});
        if (cpId == -1) {
          return result;
        }

        return this.getWorkflow(cpId, 'specimenTree').then(cpTreeCfg => Object.assign(result, cpTreeCfg || {}));
      }
    );
  }

  async isBarcodingEnabled() {
    return http.get('collection-protocols/barcoding-enabled').then(resp => resp == true || resp == 'true');
  }

  async getLockedFields(cpId, entityType, source) {
    return this.getWorkflowProperty(cpId, 'locked-fields', entityType).then(
      sources => sources && sources[source] instanceof Array ? sources[source] : []
    );
  }

  async generateCpReport(cpId) {
    const {status} = await http.post('collection-protocols/' + cpId + '/report', {});
    return status;
  }

  getUserRoleOn(cp) {
    const {currentUser} = ui;
    if (currentUser.admin) {
      return 'system-admin';
    }

    const roles = authSvc.getUserRoles();
    if (!roles || roles.length == 0) {
      return null;
    }

    for (let sr of roles) {
      if (!sr.site && !sr.collectionProtocol) {
        return sr.role.name;
      } else if (!sr.site && sr.collectionProtocol) {
        if (sr.collectionProtocol.id == cp.id) {
          return sr.role.name;
        }
      } else if (sr.site && !sr.collectionProtocol) {
        if (cp.cpSites.some(cpSite => cpSite.siteId == sr.site.id)) {
          return sr.role.name;
        }
      } else {
        if (sr.collectionProtocol.id == cp.id && cp.cpSites.some(cpSite => cpSite.siteId == sr.site.id)) {
          return sr.role.name;
        }
      }
    }

    return null;
  }

  star(cp) {
    return http.post('collection-protocols/' + cp.id + '/labels', {});
  }

  unstar(cp) {
    return http.delete('collection-protocols/' + cp.id + '/labels');
  }

  exportJson(cp) {
    http.downloadFile(http.getUrl('collection-protocols/' + cp.id + '/definition'));
  }

  exportCsv(cp) {
    exportSvc.exportRecords({objectType: 'cp', recordIds: [cp.id]});
  }

  exportEvents(cp) {
    exportSvc.exportRecords({objectType: 'cpe', params: {cpId: cp.id}});
  }

  exportSpecimenReqs(cp) {
    exportSvc.exportRecords({objectType: 'sr', params: {cpId: cp.id}});
  }

  exportWorkflows(cp) {
    http.downloadFile(http.getUrl('collection-protocols/' + cp.id + '/workflows-file'));
  }

  getRevisions(cp, opts) {
    return http.get('collection-protocols/' + cp.id + '/published-events', opts || {});
  }

  downloadRevisionJson(cp, versionId) {
    return http.downloadFile(http.getUrl('collection-protocols/' + cp.id + '/published-versions/' + versionId));
  }

  async getCustomFieldsForm() {
    const extnInfo = await http.get('collection-protocols/extension-form');
    if (!extnInfo || !extnInfo.formId) {
      return null;
    }

    return formSvc.getDefinition(extnInfo.formId);
  }

  async getDict() {
    const result  = util.clone(cpSchema.fields);
    const formDef = await this.getCustomFieldsForm();
    const customFields = formUtil.deFormToDict(formDef, 'cp.extensionDetail.attrsMap.');
    return result.concat(customFields);
  }

  async getAddEditFormSchema() {
    return this.getCustomFieldsForm().then(
      function(formDef) {
        const addEditFs = formUtil.getFormSchema(cpSchema.fields, addEditLayout.layout);
        const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef, 'cp.extensionDetail.attrsMap.');
        addEditFs.rows = addEditFs.rows.concat(schema.rows);
        return { schema: addEditFs, defaultValues };
      }
    );
  }

  getPublishFormSchema() {
    return util.clone(publishForm.layout);
  }

  getEventAddEditFormSchema() {
    return eventAddEditLayout.layout;
  }

  getReqAddEditFormSchema() {
    return formUtil.getFormSchema(reqSchema.fields, reqAddEditLayout.layout);
  }
}

export default new CollectionProtocol();
