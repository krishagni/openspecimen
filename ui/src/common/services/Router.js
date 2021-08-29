
import router from '@/router/index.js';

class Router {
  ngGoto(state, params, opts) {
    let payload = { state: state, params: params, opts: opts };
    window.parent.postMessage({
      op: 'changeRoute',
      payload: payload,
      requestor: 'vueapp'
    }, '*');
  }

  goto(name, params, query) {
    router.push({name: name, params: params || {}, query: query || {}});
  }

  back() {
    router.go(-1);
  }
}

export default new Router();
