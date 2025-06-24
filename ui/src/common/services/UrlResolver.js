import routerSvc from './Router.js';

class UrlResolver {
  urlsMap = {};

  registerUrl(name, routeName, paramName, queryName) {
    this.urlsMap[name] = {routeName, paramName, queryName};
  }

  getUrl(name, entityId) {
    const {routeName, paramName, queryName} = this.urlsMap[name] || {};
    if (!routeName) {
      return null;
    }

    const params = {};
    if (paramName) {
      params[paramName] = entityId;
    }

    const query = {};
    if (queryName) {
      query[queryName] = entityId;
    }

    return routerSvc.getUrl(routeName, params, query);
  }
}

export default new UrlResolver();
