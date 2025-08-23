import queryFormsCache from './FormsCache.js';
import queryFolderSvc  from './QueryFolder.js';
import querySvc        from './Query.js';
import queryUtil       from './Util.js';
import savedQuerySvc   from './SavedQuery.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, {
      queryFormsCache,
      queryFolderSvc,
      querySvc,
      queryUtil,
      savedQuerySvc
    });
  }
}
