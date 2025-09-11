
import listSchema     from '@/administrative/schemas/shipments/list.js';
import shipmentSchema from '@/administrative/schemas/shipments/shipment.js';
import addEditLayout  from '@/administrative/schemas/shipments/addedit.js';

import http from '@/common/services/HttpClient.js';
import formUtil from '@/common/services/FormUtil.js';

class Shipment {
  async getShipments(filterOpts) {
    return http.get('shipments', filterOpts || {});
  }

  async getShipmentsCount(filterOpts) {
    return http.get('shipments/count', filterOpts || {});
  }

  async getShipment(shipmentId) {
    return http.get('shipments/' + shipmentId);
  }

  async saveOrUpdate(shipment) {
    if (!shipment.id) {
      return http.post('shipments', shipment);
    } else {
      return http.put('shipments/' + shipment.id, shipment);
    }
  }

  async delete(shipment) {
    return http.delete('shipments/' + shipment.id);
  }

  async updateRequestStatus(shipment, status) {
    return http.put('shipments/' + shipment.id + '/request-status', {id: shipment.id, requestStatus: status});
  }

  async getSpecimens(shipmentId, params = {startAt: 0, maxResults: 25}) {
    return http.get('shipments/' + shipmentId + '/specimens', params || {startAt: 0, maxResults: 25});
  }

  getSelectedSpecimens(spmnIds) {
    if (!spmnIds || spmnIds.length == 0) {
      return null;
    }

    spmnIds = JSON.parse(spmnIds);
    if (!(spmnIds instanceof Array) || spmnIds.length == 0) {
      return null;
    }

    return this.getSpecimenByIds(spmnIds).then(
      spmns => spmns
        .filter(spmn => (spmn.availableQty == undefined || spmn.availableQty > 0) && spmn.activityStatus == 'Active')
        .map(spmn => ({specimen: spmn}))
    );
  }

  async getSpecimenByIds(spmnIds) {
    return http.post('specimens/search', {ids: spmnIds, maxResults: spmnIds.length});
  }

  async getContainers(shipmentId, params) {
    return http.get('shipments/' + shipmentId + '/containers', params || {startAt: 0, maxResults: 25});
  }

  async searchContainers({request, sendingSite, receivingSite}, names) {
    const params = {request, sendingSite, receivingSite, name: names};
    return http.get('shipments/containers', params);
  }

  async generateReport(shipmentId) {
    return http.get('shipments/' + shipmentId + '/report');
  }

  async createCartIfAbsent(shipmentId, cart) {
    return http.put('shipments/' + shipmentId + '/cart', cart);
  }

  async getListViewSchema() {
    return listSchema;
  }

  async getDict() {
    return JSON.parse(JSON.stringify(shipmentSchema.fields));
  }

  async getAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(shipmentSchema.fields, addEditLayout.layout);
    return { schema: addEditFs };
  }
}

export default new Shipment();
