import http     from '@/common/services/HttpClient.js';
import formUtil from '@/common/services/FormUtil.js';
import util     from '@/common/services/Util.js';
import ui       from '@/global.js';

import cartSchema from '@/biospecimen/schemas/carts/cart.js';
import addEditLayout from '@/biospecimen/schemas/carts/addedit.js';

class SpecimenCart {
  async getCarts(filterOpts) {
    return http.get('specimen-lists', filterOpts || {});
  }

  async getCartsCount(filterOpts) {
    return http.get('specimen-lists/count', filterOpts || {});
  }

  async getCart(cartId) {
    return http.get('specimen-lists/' + cartId);
  }

  async saveOrUpdate(cart) {
    if (cart.id) {
      return http.put('specimen-lists/' + cart.id, cart);
    } else {
      return http.post('specimen-lists', cart);
    }
  }

  async delete(cart) {
    return http.delete('specimen-lists/' + cart.id);
  }

  async addToCart(cart, specimens) {
    const ids = (specimens || []).map(spmn => spmn.id);
    if (ids.length == 0) {
      return {count: 0};
    }

    return http.put('specimen-lists/' + cart.id + '/specimens', ids, {operation: 'ADD'});
  }

  async removeFromCart(cart, specimens) {
    const ids = (specimens || []).map(spmn => spmn.id);
    if (ids.length == 0) {
      return {count: 0};
    }

    return http.put('specimen-lists/' + cart.id + '/specimens', ids, {operation: 'REMOVE'});
  }

  async addChildSpecimens(cart) {
    return http.post('specimen-lists/' + cart.id + '/add-child-specimens', {});
  }

  async generateReport(cart, params = {}) {
    return http.get('specimen-lists/' + cart.id + '/report', params || {});
  }

  async star(cart) {
    return http.post('specimen-lists/' + cart.id + '/labels');
  }

  async unstar(cart) {
    return http.delete('specimen-lists/' + cart.id + '/labels');
  }

  isDefaultCart(cart) {
    return cart.name && cart.name.indexOf('$$$$user_') == 0;
  }

  getDisplayName(cart) {
    if (!cart.name || cart.name.indexOf('$$$$user_') != 0) {
      return cart.name;
    }

    const {owner} = cart;
    if (owner.id == ui.currentUser.id) {
      return 'My Default Cart';
    } else {
      let displayName = owner.firstName || '';
      if (owner.lastName) {
        if (displayName) {
          displayName += ' ';
        }

        displayName += owner.lastName;
      }

      displayName += (displayName && '\'s ') || '';
      return displayName + 'Default Cart';
    }
  }

  getDict() {
    return util.clone(cartSchema.fields);
  }

  getAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(cartSchema.fields, addEditLayout.layout);
    return { schema: addEditFs };
  }
}

export default new SpecimenCart();
