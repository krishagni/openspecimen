
import instituteSvc from './Institute.js';
import roleSvc      from './Role.js';
import siteSvc      from './Site.js';
import userSvc      from './User.js';
import userGroupSvc from './UserGroup.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, {
      instituteSvc: instituteSvc,
      roleSvc:      roleSvc,
      siteSvc:      siteSvc,
      userSvc:      userSvc,
      userGroupSvc: userGroupSvc
    });
  }
}