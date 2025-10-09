
import formUtil from '@/common/services/FormUtil.js';
import http from '@/common/services/HttpClient.js';


import addEditSchema from '../schemas/rate-lists/rate-list-addedit.js';
import dict        from '../schemas/rate-lists/rate-list.js';
import listSchema  from '../schemas/rate-lists/rate-lists.js';

class RateList {
  getRateLists(filters) {
    return http.get('lab-services-rate-lists', filters || {});
  }

  getRateListsCount(filters) {
    return http.get('lab-services-rate-lists/count', filters || {});
  }

  getRateList(rateListId) {
    return http.get('lab-services-rate-lists/' + rateListId);
  }

  saveOrUpdate(rateList) {
    if (rateList.id > 0) {
      return http.put('lab-services-rate-lists/' + rateList.id, rateList);
    } else {
      return http.post('lab-services-rate-lists', rateList);
    }
  }

  deleteRateList(rateListId) {
    return http.delete('lab-services-rate-lists/' + rateListId);
  }

  getListSchema() {
    return listSchema;
  }

  getDict() {
    return dict.fields;
  }

  getAddEditSchema() {
    return formUtil.getFormSchema(dict.fields, addEditSchema.layout);
  }
}

export default new RateList();
