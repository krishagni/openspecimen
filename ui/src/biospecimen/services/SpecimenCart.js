import http from '@/common/services/HttpClient.js';
import ui   from '@/global.js';

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

  async addToCart(cart, specimens) {
    const ids = (specimens || []).map(spmn => spmn.id);
    if (ids.length == 0) {
      return {count: 0};
    }

    return http.put('specimen-lists/' + cart.id + '/specimens', ids, {operation: 'ADD'});
  }

  async star(cart) {
    return http.post('specimen-lists/' + cart.id + '/labels');
  }

  async unstar(cart) {
    return http.delete('specimen-lists/' + cart.id + '/labels');
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
}

export default new SpecimenCart();
