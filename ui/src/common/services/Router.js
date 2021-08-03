
class Router {
  ngGoto(state, params, opts) {
    let payload = { state: state, params: params, opts: opts };
    window.parent.postMessage({
      op: 'changeRoute',
      payload: payload,
      requestor: 'vueapp'
    }, '*');
  }

  back() {
    window.parent.postMessage({op: 'back', requestor: 'vueapp'}, '*');
  }
}

export default new Router();
