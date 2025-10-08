
import http from '@/common/services/HttpClient.js';

import addEditSchema from '../schemas/rate-lists/services-addedit.js';
import listSchema    from '../schemas/rate-lists/services-list.js';

class LabService {
  getServices(filters) {
    return http.get('lab-services', filters || {});
  }

  getServicesCount(filters) {
    return http.get('lab-services/count', filters || {});
  }

  saveOrUpdate(service) {
    if (service.id > 0) {
      return http.put('lab-services/' + service.id, service);
    } else {
      return http.post('lab-services', service);
    }
  }

  getListSchema() {
    return listSchema;
  }

  getAddEditSchema() {
    return addEditSchema.layout;
  }
}

export default new LabService();
