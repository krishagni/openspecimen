
import formUtil from '@/common/services/FormUtil.js';
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';


import addEditSchema from '../schemas/rate-lists/rate-list-addedit.js';
import addRateListCpsSchema from '../schemas/rate-lists/add-rate-list-cps.js';
import addEditRateListServicesSchema from '../schemas/rate-lists/rl-services-addedit.js';
import cpsListSchema from '../schemas/rate-lists/rl-cp-list.js';
import dict        from '../schemas/rate-lists/rate-list.js';
import listSchema  from '../schemas/rate-lists/rate-lists.js';
import servicesListSchema  from '../schemas/rate-lists/rl-services-list.js';

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

  addCollectionProtocols(rateListId, cps) {
    const payload = {op: 'ADD', rateListId, cps};
    return http.put('lab-services-rate-lists/' + rateListId + '/collection-protocols', payload);
  }

  removeCollectionProtocols(rateListId, cps) {
    const payload = {op: 'RM', rateListId, cps};
    return http.put('lab-services-rate-lists/' + rateListId + '/collection-protocols', payload);
  }

  getServices(rateListId) {
    return http.get('lab-services-rate-lists/' + rateListId + '/service-rates');
  }

  upsertServices(rateListId, serviceRates) {
    const payload = {op: 'UPSERT', rateListId, serviceRates};
    return http.put('lab-services-rate-lists/' + rateListId + '/service-rates', payload);
  }

  removeServices(rateListId, serviceRates) {
    const payload = {op: 'DELETE', rateListId, serviceRates};
    return http.put('lab-services-rate-lists/' + rateListId + '/service-rates', payload);
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

  getCpsListSchema() {
    return cpsListSchema;
  }

  getAddCpsSchema() {
    return addRateListCpsSchema.layout;
  }

  getServicesListSchema() {
    return servicesListSchema;
  }

  getAddEditServicesSchema() {
    return addEditRateListServicesSchema;
  }
}

export default new RateList();
