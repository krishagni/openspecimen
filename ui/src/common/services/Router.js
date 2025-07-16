
import router from '@/router/index.js';

class Router {
  uiUrl = 'ui-app/';
  // uiUrl = 'http://localhost:8081/';

  ngGoto(state) {
    alert('AngularJS link (ngGoto): "' + state + '" not supported. Report the problem to OpenSpecimen support staff');
  }

  ngUrl(state) {
    alert('AngularJS link (ngUrl): "' + state + '" not supported. Report the problem to OpenSpecimen support staff');
  }

  goto(name, params, query) {
    return router.push({name: name, params: params || {}, query: query || {}});
  }

  replace(name, params, query) {
    return router.push({name: name, params: params || {}, query: query || {}, replace: true});
  }

  back() {
    router.go(-1);
  }

  reload() {
    router.go(router.currentRoute);
  }

  reloadView(name, params, query) {
    window.location.href = this.getUrl(name, params, query);
    window.location.reload();
  }

  getCurrentRoute() {
    const route = router.currentRoute;
    return (route && route.value) || {};
  }

  getUrl(name, params, query) {
    let route = router.resolve({name, params, query});
    return route && route.href;
  }

  getFullUrl(name, params, query) {
    const url = this.getUrl(name, params, query);
    return this.uiUrl + url;
  }

  getLastRoute() {
    return this.lastRoute;
  }

  setLastRoute(route) {
    this.lastRoute = route;
  }

  eraseLastRoute() {
    this.lastRoute = null;
  }
}

export default new Router();
