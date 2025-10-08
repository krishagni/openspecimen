
import http from '@/common/services/HttpClient.js';

import listSchema from '../schemas/rate-lists/services-list.js';

class LabService {
  getServices(filters) {
    return http.get('lab-services', filters || {});
  }

  getServicesCount(filters) {
    return http.get('lab-services/count', filters || {});
  }

  getListSchema() {
    return listSchema;
  }
}

export default new LabService();
