
import http from '@/common/services/HttpClient.js';
import formUtil from '@/common/services/FormUtil.js';

import siteSchema from '@/administrative/schemas/sites/site.js';

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

        return http.get('forms/' + resp.formId + '/definition');
      }
    );
  }

  async getDict() {
    let result = JSON.parse(JSON.stringify(siteSchema.fields)); // copy
    return http.get('sites/extension-form').then(
      function(extensionCtxt) {
        if (!extensionCtxt || !extensionCtxt.formId) {
          return result;
        }

        return http.get('forms/' + extensionCtxt.formId + '/definition').then(
          function(formDef) {
            let customFields = formUtil.deFormToDict(formDef, 'site.extensionDetail.attrsMap.');
            return result.concat(customFields);
          }
        );
      }
    );
  }
}

export default new Site();
