import formSvc from './Form.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, { formSvc: formSvc });
  }
}
