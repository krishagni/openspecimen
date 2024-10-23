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

import ConfirmationService from 'primevue/confirmationservice';
import ToastService from 'primevue/toastservice';
import Tooltip from 'primevue/tooltip';
import BadgeDirective from 'primevue/badgedirective';

import router from './router'
import ui from './global.js';
import Root from './Root.vue'

import alerts from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import pluginLoader from '@/common/services/PluginLoader.js';
import settingSvc from '@/common/services/Setting.js';
import routerSvc from '@/common/services/Router.js';

// import itemsSvc from '@/common/services/ItemsHolder.js';

import showIfAllowed from '@/common/directives/ShowIfAllowed.js';

import CommonComponents  from '@/common/components';
import CommonServices    from '@/common/services';
import CommonFilters     from '@/common/filters';
import AdminServices     from '@/administrative/services';
import BioSpmnComponents from '@/biospecimen/components';
import BioSpmnServices   from '@/biospecimen/services';
import DeFormComponents  from '@/forms/components';
import DeFormServices    from '@/forms/services';
import HomeComponents    from '@/home/components';
import ImportComponents  from '@/importer/components';

import queryRoutes from '@/queries/routes.js';

import Layout from '@/administrative/containers/Layout.vue';

window['Vue'] = Vue;
const app = createApp(Root)
  .use(ConfirmationService)
  .use(ToastService)
  .use(CommonComponents)
  .use(CommonServices)
  .use(CommonFilters)
  .use(AdminServices)
  .use(BioSpmnComponents)
  .use(BioSpmnServices)
  .use(DeFormComponents)
  .use(DeFormServices)
  .use(HomeComponents)
  .use(ImportComponents);

app.directive('show-if-allowed', showIfAllowed);
app.directive('os-tooltip', Tooltip);
app.directive('os-badge', BadgeDirective);

app.component('os-container-layout', Layout);

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

if (!http.headers['X-OS-API-TOKEN']) {
  delete http.headers['X-OS-API-TOKEN'];
}

// register query routes
app.use(queryRoutes, {router, osSvc: app.config.globalProperties.$osSvc});

let appPropsQ  = settingSvc.getAppProps();
let localeQ    = settingSvc.getLocale();
let messagesQ  = settingSvc.getI18nMessages();
let siteAssetsQ = settingSvc.getSiteAssets();

Promise.all([appPropsQ, localeQ, messagesQ, siteAssetsQ]).then(
  (resp) => {
    let appProps = resp[0];
    let locale   = resp[1];
    let messages = resp[2];

    let fallback = locale.locale;
    let parts = (fallback || '').split('_');
    if (parts.length != 0) {
      fallback = parts[0] || 'en';
    }

    const i18n = window.osI18n = createI18n({
      locale: locale.locale,
      fallbackLocale: [fallback, 'en'],
      messages: messages
    });
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
      siteAssets: resp[3]
    };

    let osSvc = window.osSvc = app.config.globalProperties.$osSvc;
    app.provide('ui', ui);
    app.provide('osSvc', osSvc);

    let count = appProps.plugins.length;
    if (count <= 0) {
      app.use(router).use(PrimeVue).mount('#app');
    }

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
