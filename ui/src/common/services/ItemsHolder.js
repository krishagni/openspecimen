
class ItemsHolder {
  itemsMap = {};

  getItems(type) {
    return this.itemsMap[type] || [];
  }

  setItems(type, items) {
    this.itemsMap[type] = items;
  }

  clearItems(type) {
    this.itemsMap[type] = undefined;
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
    if (!items) {
      delete this.itemsMap[type];
      localStorage.removeItem('os.' + type);
    } else {
      this.itemsMap[type] = items;
      localStorage.setItem('os.' + type, JSON.stringify(items));
    }
  }
}

export default new ItemsHolder();
