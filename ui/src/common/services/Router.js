
import router from '@/router/index.js';

class Router {
  // baseUrl = '#/';
  baseUrl = 'http://localhost:9000/#/';

  ngGoto(state, params) {
    let url   = this.baseUrl + state;
    let query = '';
    Object.keys(params || {}).forEach(
      (key) => {
        let value = params[key];
        if (value) {
          if (query) {
            query += '&';
          }

          query += key + '=' + value;
        }
      }
    );

    if (query) {
      url += '?' + query;
    }

    window.location.href = url;
  }

  goto(name, params, query) {
    router.push({name: name, params: params || {}, query: query || {}});
  }

  back() {
    router.go(-1);
  }

  getUrl(name, params, query) {
    let route = router.resolve({name: name, params: params, query: query});
    return route && route.href;
  }
}

export default new Router();
