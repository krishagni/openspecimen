
class ItemsHolder {
  itemsMap = {};

  getItems(type) {
    return this.itemsMap[type] || [];
  }

  setItems(type, items) {
    this.itemsMap[type] = items;
  }

  ngGetItems(type) {
    let payload = { type: type };
    window.parent.postMessage({
      op: 'getItems',
      payload: payload,
      requestor: 'vueapp'
    }, '*');


    let ctx = {};
    let listener = function(event) {
      if (event.data.op != 'getItems') {
        return;
      }

      window.removeEventListener('message', listener);
      ctx.resolve(event.data.resp.items);
    }

    window.addEventListener('message', listener);
    return new Promise((resolve) => { ctx.resolve = resolve; });
  }
      
  ngSetItems(type, items) {
    let payload = { type: type, items: items };
    window.parent.postMessage({
      op: 'addItems',
      payload: payload,
      requestor: 'vueapp'
    }, '*');
  }
}

export default new ItemsHolder();
