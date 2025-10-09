
import formUtil from '@/common/services/FormUtil.js';
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';


import addEditSchema from '../schemas/rate-lists/rate-list-addedit.js';
import cpListSchema from '../schemas/rate-lists/rl-cp-list.js';
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

  deleteRateList(rateList) {
    const toSave = util.clone(rateList);
    toSave.activityStatus = 'Disabled';
    return http.put('lab-services-rate-lists/' + toSave.id, toSave);
  }

  getCollectionProtocols(rateListId, filters) {
    return http.get('lab-services-rate-lists/' + rateListId + '/collection-protocols', filters);
  }

  getCollectionProtocolsCount(rateListId, filters) {
    return http.get('lab-services-rate-lists/' + rateListId + '/collection-protocols/count', filters);
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

  getCpListSchema() {
    return cpListSchema;
  }
}

export default new RateList();
