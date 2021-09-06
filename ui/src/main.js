import { createApp } from 'vue'
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

import ToastService from 'primevue/toastservice';
import Tooltip from 'primevue/tooltip';

import router from './router'
import ui from './global.js';
import Root from './Root.vue'

import alerts from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import authSvc from '@/common/services/Authorization.js';
import pluginLoader from '@/common/services/PluginLoader.js';
import settingSvc from '@/administrative/services/Setting.js';
import userSvc from '@/administrative/services/User.js';
import routerSvc from '@/common/services/Router.js';

import svcRegistry from '@/common/services/ServicesRegistry.js';

// import itemsSvc from '@/common/services/ItemsHolder.js';

import showIfAllowed from '@/common/directives/ShowIfAllowed.js';

import Icon from '@/common/components/Icon.vue';
import Message from '@/common/components/Message.vue';


window['Vue'] = Vue;
window['osSvcRegistry'] = svcRegistry;

const app = createApp(Root)
  .use(ToastService);

app.directive('show-if-allowed', showIfAllowed);
app.directive('os-tooltip', Tooltip);

app.component('os-icon', Icon);
app.component('os-message', Message);

let filters = app.config.globalProperties.$filters = app.config.globalProperties.$filters || {};
filters.username = (user) => {
  if (!user) {
    return '-';
  }

  let result = '';
  if (user.firstName) {
    result = user.firstName;
  }

  if (result) {
    result += ' ';
  }

  if (user.lastName) {
    result += user.lastName;
  }

  return result || '-';
}

filters.dateTime = (date) => {
  if (!date) {
    return '-';
  }

  var dt = new Date(date);
  return dt.toLocaleDateString() + ' ' + dt.toLocaleTimeString();
}

filters.date = (date) => {
  if (!date) {
    return '-';
  }

  return new Date(date).toLocaleDateString();
}

filters.boolValue = (value, cfg) => {
  cfg = cfg || {};
  if (value == 1 || value == true || value == 'true') {
    return cfg[true] || 'Yes';
  } else if (value == 0 || value == false || value == 'false') {
    return cfg[false] || 'No';
  } else {
    return '-';
  }
}

filters.arrayJoin = (value) => {
  if (value instanceof Array) {
    return value.join(', ');
  } else if (!value) {
    return '-';
  } else {
    return value;
  }
}

app.config.globalProperties.$goto =
  (name, params, query) => routerSvc.goto(name, params, query);

alerts.toastSvc = app.config.globalProperties.$toast;
library.add(fas);

let url = window.location.href;
let urlParts = url.split('?')
let query = urlParts.length > 1 ? urlParts[1] : '';
let params = new URLSearchParams(query);

app.config.globalProperties.$ui = ui;
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

let settingsQ  = settingSvc.getAppProps();
let localeQ    = settingSvc.getLocale();
let currUserQ  = userSvc.getCurrentUser();
let usrRightsQ = authSvc.loadUserRights();
Promise.all([settingsQ, localeQ, currUserQ, usrRightsQ]).then(
  (resp) => {
    let appProps = resp[0];
    let locale   = resp[1];
    let currUser = resp[2];

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
      }
    };

    ui.currentUser = currUser;
    ui.menuItems = [];
    app.provide('ui', ui);

    let count = appProps.plugins.length;
    // app.use(router).use(PrimeVue)
    //  .mount('#app');
    appProps.plugins.forEach(
      (pluginName) => {
        pluginLoader.load(pluginName).then(
          function(pluginModule) {
            app.use(pluginModule.default, { router });
            --count;
            if (count <= 0) {
              app.use(router).use(PrimeVue)
                .mount('#app');

              console.log(router.getRoutes());
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

//
// listen for route changes
//

/*router.afterEach(
  (to, from, failure) => {
    if (!failure) {
      // alert('Change the window browser URL to: ' + to.fullPath);
      // window.parent.href = to.href;
      // routerSvc.changeUrl(to.href);
    }
  }
);*/
