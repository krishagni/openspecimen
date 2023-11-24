
import cpSvc    from '@/biospecimen/services/CollectionProtocol.js';
import formSvc  from '@/forms/services/Form.js';
import formUtil from '@/common/services/FormUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';

import cprSchema from '@/biospecimen/schemas/participants/cpr.js';

class CollectionProtocolRegistration {

  workflows = {};

  async getCpr(cprId) {
    return http.get('collection-protocol-registrations/' + cprId);
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
}

export default new CollectionProtocolRegistration();

