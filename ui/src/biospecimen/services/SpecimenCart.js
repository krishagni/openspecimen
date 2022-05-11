import http from '@/common/services/HttpClient.js';

class SpecimenCart {
  addToCart(cart, specimens) {
    const ids = (specimens || []).map(spmn => spmn.id);
    if (ids.length == 0) {
      return;
    }

    return http.put('specimen-lists/' + cart.id + '/specimens', ids, {operation: 'ADD'});
  }
}

export default new SpecimenCart();
