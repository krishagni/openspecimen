import cpSvc from './CollectionProtocol.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, {
      cpSvc: cpSvc,
    });
  }
}
