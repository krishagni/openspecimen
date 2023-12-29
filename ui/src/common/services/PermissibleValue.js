
import http from '@/common/services/HttpClient.js';

class PermissibleValue {
  async getPvs(attribute, searchTerm, opts) {
    opts = opts || {};

    const queryParams = {...opts, attribute, searchString: searchTerm};
    return http.get('permissible-values/v', queryParams);
  }
}

export default new PermissibleValue();
