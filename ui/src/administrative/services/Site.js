
import http from '@/common/services/HttpClient.js';
import formUtil from '@/common/services/FormUtil.js';
import formSvc from '@/forms/services/Form.js';

import siteSchema    from '@/administrative/schemas/sites/site.js';
import addEditLayout from '@/administrative/schemas/sites/addedit.js';

class Site {
  async getSites(filterOpts) {
    return http.get('sites', filterOpts || {});
  }

  async getSitesCount(filterOpts) {
    return http.get('sites/count', filterOpts || {});
  }

  async getSite(id) {
    return http.get('sites/' + id);
  }

  async saveOrUpdate(site) {
    if (!site.id) {
      return http.post('sites', site);
    } else {
      return http.put('sites/' + site.id, site);
    }
  }

  async bulkUpdate({detail, ids}) {
    if (!ids || ids.length == 0) {
      return [];
    }

    return http.put('sites/bulk-update', {detail: detail, ids: ids});
  }

  async delete(site) {
    return http.delete('sites/' + site.id);
  }

  async getDependents(site) {
    return http.get('sites/' + site.id + '/dependent-entities');
  }

  async getCustomFieldsForm() {
    return http.get('sites/extension-form').then(
      function(resp) {
        if (!resp || !resp.formId) {
          return null;
        }

        return formSvc.getDefinition(resp.formId);
      }
    );
  }

  async getDict() {
    let result = JSON.parse(JSON.stringify(siteSchema.fields)); // copy
    return this.getCustomFieldsForm().then(
      function(formDef) {
        let customFields = formUtil.deFormToDict(formDef, 'site.extensionDetail.attrsMap.');
        return result.concat(customFields);
      }
    );
  }

  async getAddEditFormSchema() {
    return this.getCustomFieldsForm().then(
      function(formDef) {
        const addEditFs = formUtil.getFormSchema(siteSchema.fields, addEditLayout.layout);
        const { schema, defaultValues }   = formUtil.fromDeToStdSchema(formDef, 'site.extensionDetail.attrsMap.');
        addEditFs.rows = addEditFs.rows.concat(schema.rows);
        return { schema: addEditFs, defaultValues };
      }
    );
  }
}

export default new Site();
