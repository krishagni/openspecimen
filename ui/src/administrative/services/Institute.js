
import listSchema    from '@/administrative/schemas/institutes/list.js';
import baseSchema    from '@/administrative/schemas/institutes/institute.js';
import addEditLayout from '@/administrative/schemas/institutes/addedit.js';

import http     from '@/common/services/HttpClient.js';
import formUtil from '@/common/services/FormUtil.js';

class Institute {
  async getInstitutes(filterOpts) {
    return http.get('institutes', filterOpts || {});
  }

  async getInstitutesCount(filterOpts) {
    return http.get('institutes/count', filterOpts || {});
  }

  async getInstitute(instituteId) {
    return http.get('institutes/' + instituteId);
  }

  async saveOrUpdate(institute) {
    if (!institute.id) {
      return http.post('institutes', institute);
    } else {
      return http.put('institutes/' + institute.id, institute);
    }
  }

  async delete(institute) {
    return http.delete('institutes/' + institute.id);
  }

  async deleteInstitutes(instituteIds) {
    return http.delete('institutes', {}, {id: instituteIds});
  }

  async getDependents(institute) {
    return http.get('institutes/' + institute.id + '/dependent-entities');
  }

  async getListViewSchema() {
    return listSchema;
  }

  async getDict() {
    return JSON.parse(JSON.stringify(baseSchema.fields));
  }

  async getAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(baseSchema.fields, addEditLayout.layout);
    return { schema: addEditFs, defaultValues: {} };
  }
}

export default new Institute();
