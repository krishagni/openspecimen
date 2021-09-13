
import http from '@/common/services/HttpClient.js';
import listSchema from '@/administrative/schemas/institutes/list.js';

class Institute {
  async getInstitutes(filterOpts) {
    return http.get('institutes', filterOpts || {});
  }

  async getInstitutesCount(filterOpts) {
    return http.get('institutes/count', filterOpts || {});
  }

  async getListViewSchema() {
    return listSchema;
  }
}

export default new Institute();
