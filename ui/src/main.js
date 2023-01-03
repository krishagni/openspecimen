import { createApp } from 'vue'
import { createI18n } from 'vue-i18n'
import * as Vue from 'vue';
import PrimeVue from 'primevue/config';

// import 'bootstrap/dist/css/bootstrap.min.css';
// import 'primevue/resources/themes/saga-blue/theme.css';
import 'primevue/resources/themes/bootstrap4-light-blue/theme.css';
import 'primevue/resources/primevue.css';
import 'primeflex/primeflex.css';
import 'primeicons/primeicons.css';

import { library } from '@fortawesome/fontawesome-svg-core'
import { fas } from '@fortawesome/free-solid-svg-icons'
import { far } from '@fortawesome/free-regular-svg-icons'

import ToastService from 'primevue/toastservice';
import Tooltip from 'primevue/tooltip';
import BadgeDirective from 'primevue/badgedirective';

import router from './router'
import ui from './global.js';
import Root from './Root.vue'

import alerts from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import authSvc from '@/common/services/Authorization.js';
import pluginLoader from '@/common/services/PluginLoader.js';
import settingSvc from '@/common/services/Setting.js';
import util from '@/common/services/Util.js';
import userSvc from '@/administrative/services/User.js';
import routerSvc from '@/common/services/Router.js';

// import itemsSvc from '@/common/services/ItemsHolder.js';

import showIfAllowed from '@/common/directives/ShowIfAllowed.js';

import CommonComponents  from '@/common/components';
import CommonServices    from '@/common/services';
import CommonFilters     from '@/common/filters';
import AdminServices     from '@/administrative/services';
import BioSpmnComponents from '@/biospecimen/components';
import BioSpmnServices   from '@/biospecimen/services';

window['Vue'] = Vue;
const app = createApp(Root)
  .use(ToastService)
  .use(CommonComponents)
  .use(CommonServices)
  .use(CommonFilters)
  .use(AdminServices)
  .use(BioSpmnComponents)
  .use(BioSpmnServices);

app.directive('show-if-allowed', showIfAllowed);
app.directive('os-tooltip', Tooltip);
app.directive('os-badge', BadgeDirective);

function registerHomePageCards(osSvc, i18n) {
  const t = i18n.global.t;
  osSvc.homePageSvc.registerCards([
    {
      showIf: {resource: 'CollectionProtocol', operations: ['Read']},
      href: '#/cps',
      icon: 'fa fa-calendar',
      title: t('common.home.cps'),
      description: t('common.home.cps_desc')
    },

    {
      showIf: 'admin',
      href: '#/institutes',
      icon: 'fa fa-institution',
      title: t('common.home.institutes'),
      description: t('common.home.institutes_desc')
    },

    {
      showIf: {resource: 'User', operations: ['Create', 'Update']},
      href: '#/users',
      icon: 'fa fa-user',
      title: t('common.home.users'),
      description: t('common.home.users_desc')
    },

    {
      showIf: {resource: 'User', operations: ['Create', 'Update']},
      href: '#/roles',
      icon: 'fa fa-lock',
      title: t('common.home.roles'),
      description: t('common.home.roles_desc')
    },

    {
      showIf: 'institute-admin',
      href: '#/sites',
      icon: 'fa fa-hospital-o',
      title: t('common.home.sites'),
      description: t('common.home.sites_desc')
    },

    {
      showIf: {resource: 'StorageContainer', operations: ['Read']},
      href: '#/containers',
      icon: 'fa fa-dropbox',
      title: t('common.home.containers'),
      description: t('common.home.containers_desc')
    },

    {
      showIf: {resource: 'Query', operations: ['Read']},
      href: '#/queries/list',
      icon: 'fa fa-dashboard',
      title: t('common.home.queries'),
      description: t('common.home.queries_desc')
    },

    {
      showIf: {resources: ['Specimen', 'PrimarySpecimen'], operations: ['Read']},
      href: '#/specimen-lists',
      icon: 'fa fa-shopping-cart',
      title: t('common.home.carts'),
      description: t('common.home.carts_desc')
    },

    {
      href: '#/forms',
      icon: 'fa fa-copy',
      title: t('common.home.forms'),
      description: t('common.home.forms_desc'),
      showIf: () => ui.currentUser.admin || ui.currentUser.instituteAdmin || ui.currentUser.manageForms
    },

    {
      showIf: {resource: 'DistributionProtocol', operations: ['Read']},
      href: '#/dps',
      icon: 'fa fa-truck',
      title: t('common.home.dps'),
      description: t('common.home.dps_desc')
    },

    {
      showIf: {resource: 'Order', operations: ['Read']},
      href: '#/orders',
      icon: 'fa fa-share',
      title: t('common.home.orders'),
      description: t('common.home.orders_desc')
    },

    {
      showIf: {resource: 'ShippingAndTracking', operations: ['Read']},
      href: '#/shipments',
      icon: 'fa fa-paper-plane-o',
      title: t('common.home.shipments'),
      description: t('common.home.shipments_desc')
    },

    {
      showIf: {resource: 'ScheduledJob', operations: ['Read']},
      href: '#/jobs',
      icon: 'fa fa-gears',
      title: t('common.home.jobs'),
      description: t('common.home.jobs_desc')
    },

    {
      showIf: 'institute-admin',
      href: '#/consent-statements',
      icon: 'fa fa-file-text-o',
      title: t('common.home.consents'),
      description: t('common.home.consents_desc')
    },

    {
      href: async () => {
        let setting = await settingSvc.getSetting('training', 'training_url');
        return setting[0].value;
      },
      icon: 'fa fa-graduation-cap',
      title: t('common.home.training'),
      description: t('common.home.training_desc'),
      newTab: true
    },

    {
      showIf: 'admin',
      href: '#/settings/settings-list',
      icon: 'fa fa-wrench',
      title: t('common.home.settings'),
      description: t('common.home.settings_desc'),
    }
  ]);
}

app.config.globalProperties.$goto =
  (name, params, query) => routerSvc.goto(name, params, query);

app.config.globalProperties.$goback = () => routerSvc.back();

alerts.toastSvc = app.config.globalProperties.$toast;
library.add(fas);
library.add(far);

let url = window.location.href;
let urlParts = url.split('?')
let query = urlParts.length > 1 ? urlParts[1] : '';
let params = new URLSearchParams(query);

window.osUiApp = app;
window.osUi    = app.config.globalProperties.$ui = ui;
let server = ui.server || {};
http.protocol = server.secure ? 'https' : 'http';
http.host = server.hostname;
http.port = server.port;
http.path = server.app || '..';
if (http.path) {
  http.path += '/';
}

// http.path += 'rest/ng'
if (params.get('token')) {
  localStorage.osAuthToken = http.headers['X-OS-API-TOKEN'] = params.get('token');
} else {
  http.headers['X-OS-API-TOKEN'] = localStorage.osAuthToken;
}

let appPropsQ  = settingSvc.getAppProps();
let localeQ    = settingSvc.getLocale();
let messagesQ  = settingSvc.getI18nMessages();
let currUserQ  = userSvc.getCurrentUser();
let usrStateQ  = userSvc.getUiState();
let usrRightsQ = authSvc.loadUserRights();
let spmnPropsQ = util.loadSpecimenTypeProps();
let siteAssetsQ = settingSvc.getSiteAssets();

Promise.all([appPropsQ, localeQ, messagesQ, currUserQ, usrRightsQ, usrStateQ, spmnPropsQ, siteAssetsQ]).then(
  (resp) => {
    let appProps = resp[0];
    let locale   = resp[1];
    let messages = resp[2];
    let currUser = resp[3];

    const i18n = window.osI18n = createI18n({locale: locale, messages: messages});
    app.use(i18n);

    ui.global = {
      defaultDomain: 'openspecimen',
      filterWaitInterval: appProps.searchDelay,
      appProps: appProps,
      locale: {
        dateFmt: locale.dateFmt,
        shortDateFmt: locale.deBeDateFmt,
        timeFmt: locale.timeFmt,
        queryDateFmt: {format: locale.deFeDateFmt},
        dateTimeFmt: locale.dateFmt + ' ' + locale.timeFmt,
        locale: locale.locale,
        utcOffset: locale.utcOffset
      },
      state: resp[5],
      siteAssets: resp[7]
    };

    let osSvc = window.osSvc = app.config.globalProperties.$osSvc;
    ui.currentUser = currUser;
    registerHomePageCards(osSvc, i18n);

    app.provide('ui', ui);
    app.provide('osSvc', osSvc);

    let count = appProps.plugins.length;
    appProps.plugins.forEach(
      (pluginName) => {
        pluginLoader.load(pluginName).then(
          function(pluginModule) {
            app.use(pluginModule.default, { router, osSvc });
            --count;
            if (count <= 0) {
              app.use(router).use(PrimeVue)
                .mount('#app');
            }
          },

          function(error) {
            console.log('Error loading the plugin: ' + pluginName);
            console.log(error);
            --count;
            if (count <= 0) {
              app.use(router).use(PrimeVue)
                .mount('#app');
            }
          }
        )
      }
    );
  }
);
