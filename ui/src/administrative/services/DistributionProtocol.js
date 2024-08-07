
import listSchema    from '@/administrative/schemas/dps/list.js';
import dpSchema      from '@/administrative/schemas/dps/dp.js';
import addEditLayout from '@/administrative/schemas/dps/addedit.js';

import reqListSchema from '@/administrative/schemas/dps/req-list.js';
import reqSchema     from '@/administrative/schemas/dps/req.js';
import reqFormLayout from '@/administrative/schemas/dps/req-addedit.js';

import formUtil from '@/common/services/FormUtil.js';
import http     from '@/common/services/HttpClient.js';
import formSvc  from '@/forms/services/Form.js';

class DistributionProtocol {
  async getDps(filterOpts) {
    return http.get('distribution-protocols', filterOpts || {});
  }

  async getDpsCount(filterOpts) {
    return http.get('distribution-protocols/count', filterOpts || {});
  }

  async getDp(dpId) {
    return http.get('distribution-protocols/' + dpId);
  }

  async saveOrUpdate(dp) {
    if (!dp.id) {
      return http.post('distribution-protocols', dp);
    } else {
      return http.put('distribution-protocols/' + dp.id, dp);
    }
  }

  async updateStatus(dp, status) {
    return http.put('distribution-protocols/' + dp.id + '/activity-status', {activityStatus: status});
  }

  async bulkDelete(dpIds) {
    return http.delete('distribution-protocols', null, {id: dpIds || []});
  }

  async getCustomFieldsForm() {
    const extnInfo = await http.get('distribution-protocols/extension-form');
    if (!extnInfo || !extnInfo.formId) {
      return null;
    }

    return formSvc.getDefinition(extnInfo.formId);
  }

  async delete(dp) {
    return http.delete('distribution-protocols/' + dp.id);
  }

  async getDependents(dp) {
    return http.get('distribution-protocols/' + dp.id + '/dependent-entities');
  }

  async cancelReservation(dp, spmnIds) {
    const req = {dpId: dp.id, specimens: (spmnIds || []).map(spmnId => ({id: spmnId})), cancelOp: true };
    return http.put('distribution-protocols/' + dp.id + '/reserved-specimens', req);
  }

  async getConsentStatements(searchTerm) {
    return http.get('consent-statements', {searchString: searchTerm});
  }

  async getConsents(dp) {
    return http.get('distribution-protocols/' + dp.id + '/consent-tiers');
  }

  async addConsent(dp, statement) {
    const payload = {dpId: dp.id, statementId: statement.id, statementCode: statement.code};
    return http.post('distribution-protocols/' + dp.id + '/consent-tiers', payload);
  }

  async deleteConsent(dp, consent) {
    return http.delete('distribution-protocols/' + dp.id + '/consent-tiers/' + consent.id);
  }

  async getRequirements(dp) {
    return http.get('distribution-protocol-requirements', {dpId: dp.id});
  }

  async getRequirement(reqId) {
    return http.get('distribution-protocol-requirements/' + reqId);
  }

  async saveOrUpdateReq(req) {
    if (!req.id) {
      return http.post('distribution-protocol-requirements', req);
    } else {
      return http.put('distribution-protocol-requirements/' + req.id, req);
    }
  }

  async deleteRequirement(req) {
    return http.delete('distribution-protocol-requirements/' + req.id);
  }

  async getReqCustomFieldsForm() {
    const extnInfo = await http.get('distribution-protocol-requirements/extension-form');
    if (!extnInfo || !extnInfo.formId) {
      return null;
    }

    return formSvc.getDefinition(extnInfo.formId);
  }

  async getListViewSchema() {
    return listSchema;
  }

  async getDict() {
    const dict = JSON.parse(JSON.stringify(dpSchema.fields));
    const formDef = await this.getCustomFieldsForm();

    const customFields = formUtil.deFormToDict(formDef, 'dp.extensionDetail.attrsMap.');
    return dict.concat(customFields);
  }

  async getAddEditFormSchema() {
    return this.getCustomFieldsForm().then(
      function(formDef) {
        const addEditFs = formUtil.getFormSchema(dpSchema.fields, addEditLayout.layout);
        const { schema, defaultValues }   = formUtil.fromDeToStdSchema(formDef, 'dp.extensionDetail.attrsMap.');
        addEditFs.rows = addEditFs.rows.concat(schema.rows);
        return { schema: addEditFs, defaultValues };
      }
    );
  }

  async getRequirementsListViewSchema() {
    return reqListSchema;
  }

  async getRequirementAddEditFormSchema() {
    const formDef = await this.getReqCustomFieldsForm();
    const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef, 'requirement.extensionDetail.attrsMap.');

    const addEditFs = formUtil.getFormSchema(reqSchema.fields, reqFormLayout.layout);
    addEditFs.rows = addEditFs.rows.concat(schema.rows);
    return { schema: addEditFs, defaultValues };
  }

  star(dp) {
    return http.post('distribution-protocols/' + dp.id + '/labels', {});
  }

  unstar(dp) {
    return http.delete('distribution-protocols/' + dp.id + '/labels');
  }
}

export default new DistributionProtocol();
