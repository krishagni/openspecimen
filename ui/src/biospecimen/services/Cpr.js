
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

import cprSchema from '@/biospecimen/schemas/participants/cpr.js';

class CollectionProtocolRegistration {

  workflows = {};

  async getCpr(cprId) {
    return http.get('collection-protocol-registrations/' + cprId);
  }

  async getDict() {
    return util.clone(cprSchema.fields);
  }
}

export default new CollectionProtocolRegistration();

