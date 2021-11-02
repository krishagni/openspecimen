import alertsSvc      from './Alerts.js';
import auditLogsSvc   from './AuditLogs.js';
import authSvc        from './Authorization.js';
import commonSvc      from './Common.js';
import exportSvc      from './ExportService.js';
import exprUtil       from './ExpressionUtil.js';
import fieldFactory   from './FieldFactory.js';
import formUtil       from './FormUtil.js';
import homePageSvc    from './HomePageService.js';
import http           from './HttpClient.js';
import itemsSvc       from './ItemsHolder.js';
import notifSvc       from './Notif.js';
import pluginLoader   from './PluginLoader.js';
import pluginViews    from './PluginViewsRegistry.js';
import routerSvc      from './Router.js';
import settingsSvc    from './Setting.js';
import util           from './Util.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, {
      alertsSvc:    alertsSvc,
      auditLogsSvc: auditLogsSvc,
      authSvc:      authSvc,
      commonSvc:    commonSvc,
      exportSvc:    exportSvc,
      exprUtil:     exprUtil,
      fieldFactory: fieldFactory,
      formUtil:     formUtil,
      homePageSvc:  homePageSvc,
      http:         http,
      itemsSvc:     itemsSvc,
      notifSvc:     notifSvc,
      pluginLoader: pluginLoader,
      pluginViews:  pluginViews,
      routerSvc:    routerSvc,
      settingsSvc:  settingsSvc,
      util:         util
    });
  }
}
