import alertsSvc      from './Alerts.js';
import auditLogsSvc   from './AuditLogs.js';
import authSvc        from './Authorization.js';
import boxUtil        from './BoxUtil.js';
import commonSvc      from './Common.js';
import exportSvc      from './ExportService.js';
import exprUtil       from './ExpressionUtil.js';
import fieldFactory   from './FieldFactory.js';
import formUtil       from './FormUtil.js';
import homePageSvc    from './HomePageService.js';
import http           from './HttpClient.js';
import itemsSvc       from './ItemsHolder.js';
import i18nSvc        from './I18n.js';
import notifSvc       from './Notif.js';
import numConvUtil    from './NumberConverterUtil.js';
import pvSvc          from './PermissibleValue.js';
import pluginLoader   from './PluginLoader.js';
import pluginViews    from './PluginViewsRegistry.js';
import routerSvc      from './Router.js';
import settingsSvc    from './Setting.js';
import util           from './Util.js';
import workflowSvc    from './Workflow.js';

export default {
  install(app) {
    let osSvc = app.config.globalProperties.$osSvc = app.config.globalProperties.$osSvc || {};
    Object.assign(osSvc, {
      alertsSvc:    alertsSvc,
      auditLogsSvc: auditLogsSvc,
      authSvc:      authSvc,
      boxUtil:      boxUtil,
      commonSvc:    commonSvc,
      exportSvc:    exportSvc,
      exprUtil:     exprUtil,
      fieldFactory: fieldFactory,
      formUtil:     formUtil,
      homePageSvc:  homePageSvc,
      http:         http,
      itemsSvc:     itemsSvc,
      i18nSvc:      i18nSvc,
      notifSvc:     notifSvc,
      numConvUtil:  numConvUtil,
      pvSvc:        pvSvc,
      pluginLoader: pluginLoader,
      pluginViews:  pluginViews,
      routerSvc:    routerSvc,
      settingsSvc:  settingsSvc,
      util:         util,
      workflowSvc:  workflowSvc
    });
  }
}
