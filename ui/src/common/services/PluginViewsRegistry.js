
class PluginViews {

  views = {};

  options = {};

  importTypes = {};

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

  registerOptions(pageName, optsName, options) {
    let page = this.options[pageName];
    if (!page) {
      page = this.options[pageName] = {};
    }

    let pageOpts = page[optsName];
    if (!pageOpts) {
      pageOpts = page[optsName] = [];
    }

    for (let option of options) {
      pageOpts.push(option);
    }
  }

  getOptions(pageName, optsName) {
    let page = this.options[pageName] || {};
    return page[optsName] || [];
  }

  getImportTypes(entityType) {
    const types = this.importTypes[entityType];
    return types || [];
  }

  registerImportTypes(entityType, types) {
    const entityImportTypes = this.importTypes[entityType] = this.importTypes[entityType] || [];
    Array.prototype.push.apply(entityImportTypes, types);
  }
}

export default new PluginViews();
