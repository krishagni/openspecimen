
import formUtil from '@/common/services/FormUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';
import wfSvc    from '@/common/services/Workflow.js';
import formSvc  from '@/forms/services/Form.js';

import addEditLayout  from '@/administrative/schemas/orders/addedit.js';
import orderSchema    from '@/administrative/schemas/orders/order.js';

class Order {
  async getOrders(filterOpts) {
    return http.get('distribution-orders', filterOpts || {});
  }

  async getOrdersCount(filterOpts) {
    return http.get('distribution-orders/count', filterOpts || {});
  }

  async getOrder(id) {
    return http.get('distribution-orders/' + id);
  }

  async getOrderItems(id, filterOpts) {
    return http.get('distribution-orders/' + id + '/items', filterOpts);
  }

  async getOrderItemsBySpecimenIds(specimenIds) {
    return http.post('distribution-orders/specimens', {ids: specimenIds});
  }

  async createOrderItemsFromSpecimens(spmnIds, printLabel) {
    if (!spmnIds || spmnIds.length == 0) {
      return [];
    }

    if (typeof spmnIds == 'string') {
      spmnIds = JSON.parse(spmnIds);
    }

    if (!(spmnIds instanceof Array) || spmnIds.length == 0) {
      return [];
    }

    const specimens = await http.post('specimens/search', {ids: spmnIds, maxResults: spmnIds.length});
    return this.createOrderItems(specimens, printLabel);
  }

  async getCartSize(cartId) {
    const resp = await http.get('specimen-lists/' + cartId + '/specimens-count', {available: true});
    return resp.count;
  }

  async createOrderItemsFromCart(cartId, printLabel, maxResults) {
    const specimens = await http.get('specimen-lists/' + cartId + '/specimens', {available: true, maxResults});
    return this.createOrderItems(specimens, printLabel);
  }

  async getReservedSpecimensListSize(dpId) {
    const resp = await http.get('distribution-protocols/' + dpId + '/reserved-specimens-count');
    return resp.count;
  }

  async createOrderItemsFromReservedSpecimens(dpId, printLabel) {
    const specimens = await http.get('distribution-protocols/' + dpId + '/reserved-specimens');
    return this.createOrderItems(specimens, printLabel);
  }

  async createOrderItemsFromRequest(catalogId, requestId, printLabel) {
    const request = await http.get('specimen-catalogs/' + catalogId + '/specimen-requests/' + requestId);
    const specimens = (request.items || [])
      .filter(item => !item.status || item.status == 'PENDING')
      .map(({specimen}) => specimen);
    return this.createOrderItems(specimens, printLabel);
  }

  createOrderItems(specimens, printLabel) {
    return specimens.filter(specimen => specimen.activityStatus == 'Active')
      .map(specimen => ({
        specimen: specimen,
        quantity: specimen.availableQty,
        status: 'DISTRIBUTED_AND_CLOSED',
        printLabel: printLabel
      }));
  }

  async saveOrUpdate(order) {
    if (!order.id) {
      return http.post('distribution-orders', order);
    } else {
      return http.put('distribution-orders/' + order.id, order);
    }
  }

  async delete(order) {
    return http.delete('distribution-orders/' + order.id);
  }

  async hasDistributionContainers(dp) {
    const containers = await http.get('storage-containers', {dpShortTitle: [dp.shortTitle], maxResults: 1});
    return containers.length > 0;
  }

  async getCosts(dpId, specimenIds) {
    return http.post('distribution-costs/' + dpId + '/costs', {specimenIds: specimenIds || []});
  }

  async getDict(dp) {
    const result  = util.clone(orderSchema.fields);
    const formDef = await this.getCustomFieldsForm(dp);
    const customFields = formUtil.deFormToDict(formDef, 'order.extensionDetail.attrsMap.');
    return result.concat(customFields);
  }

  async getCustomFieldsForm(dp) {
    let formId = dp.orderExtnForm && dp.orderExtnForm.formId;
    if (!formId) {
      const extnForm = await http.get('distribution-protocols/' + dp.id + '/order-extension-form');
      if (!extnForm || !extnForm.formId) {
        return null;
      }

      formId = extnForm.formId;
    }

    return formSvc.getDefinition(formId);
  }

  async getAddEditFormSchema(dp) {
    return this.getCustomFieldsForm(dp || {id: -1}).then(
      function(formDef) {
        const addEditFs = formUtil.getFormSchema(orderSchema.fields, addEditLayout.layout);
        const { schema, defaultValues }   = formUtil.fromDeToStdSchema(formDef, 'order.extensionDetail.attrsMap.');
        addEditFs.rows = addEditFs.rows.concat(schema.rows);
        return { schema: addEditFs, defaultValues };
      }
    );
  }

  async generateReport(orderId) {
    return http.get('distribution-orders/' + orderId + '/report');
  }

  async retrieveSpecimens(orderId, reason) {
    return http.post('distribution-orders/' + orderId + '/retrieve', {comments: reason});
  }

  async returnSpecimens(items) {
    return http.post('distribution-orders/return-specimens', items);
  }

  async printLabels(orderId, itemIds) {
    return http.post('distribution-label-printer', {orderId, itemIds});
  }

  async deleteOrderItems(orderId, itemIds) {
    return http.delete('distribution-orders/' + orderId + '/items', null, {itemId: itemIds});
  }

  async downloadLabelsFile(jobId, outputFilename) {
    let url = 'distribution-label-printer/output-file?jobId=' + jobId;
    if (outputFilename) {
      outputFilename = outputFilename.replace(/[^\w.]+/g, '_').replace(/__+/g, '_');
      url += '&filename=' + outputFilename;
    }

    http.downloadFile(http.getUrl(url));
  }

  async getCustomFields() {
    const ui = window.osUi || {};
    const plugins = ui.global.appProps.plugins || [];
    if (plugins.indexOf('sde') == -1) {
      return [];
    }

    const dictQ   = wfSvc.getDictionary(-1)
    const fieldsQ = wfSvc.getWorkflow(-1, 'order-addedit-specimens');
    return Promise.all([dictQ, fieldsQ]).then(
      (resps) => {
        const dict = resps[0];
        const columns = (resps[1] && resps[1].columns) || [];
        return wfSvc.overrideFields(dict, columns);
      }
    );
  }
}

export default new Order();
