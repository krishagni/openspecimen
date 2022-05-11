
import containerSvc     from './Container.js';
import containerTypeSvc from './ContainerType.js';
import instituteSvc     from './Institute.js';
import roleSvc          from './Role.js';
import siteSvc          from './Site.js';
import userSvc          from './User.js';
import userGroupSvc     from './UserGroup.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, {
      containerSvc:     containerSvc,
      containerTypeSvc: containerTypeSvc,
      instituteSvc:     instituteSvc,
      roleSvc:          roleSvc,
      siteSvc:          siteSvc,
      userSvc:          userSvc,
      userGroupSvc:     userGroupSvc
    });
  }
}
