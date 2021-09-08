
class PluginViews {

  views = {};

  registerView(pageName, viewName, componentDef) {
    let page = this.views[pageName];
    if (!page) {
      page = this.views[pageName] = {};
    }

    let pageViews = page[viewName];
    if (!pageViews) {
      pageViews = page[viewName] = [];
    }

    pageViews.push(componentDef);
  }

  getViews(pageName, viewName) {
    let page = this.views[pageName] || {};
    return page[viewName] || [];
  }
}

export default new PluginViews();
