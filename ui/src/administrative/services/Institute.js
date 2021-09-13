
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

  async getListViewSchema() {
    return listSchema;
  }

  async getAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(baseSchema.fields, addEditLayout.layout);
    return { schema: addEditFs, defaultValues: {} };
  }
}

export default new Institute();
