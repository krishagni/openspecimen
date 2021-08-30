import { createApp } from 'vue'
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
import settingSvc from '@/administrative/services/Setting.js';
import userSvc from '@/administrative/services/User.js';

// import itemsSvc from '@/common/services/ItemsHolder.js';
// import routerSvc from '@/common/services/Router.js';

import showIfAllowed from '@/common/directives/ShowIfAllowed.js';

const app = createApp(Root)
  .use(ToastService);

app.directive('show-if-allowed', showIfAllowed);
app.directive('os-tooltip', Tooltip);

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

alerts.toastSvc = app.config.globalProperties.$toast;
library.add(fas);

let url = window.location.href;
let params = new URLSearchParams(url.split('?')[1]);

app.config.globalProperties.$ui = ui;
let server = ui.server || {};
http.protocol = server.secure ? 'https' : 'http';
http.host = server.hostname;
http.port = server.port;
http.path = server.app || '..';
if (http.path) {
  http.path += '/';
}

http.path += 'rest/ng'
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

    console.log(ui);
    app.use(router)
      .use(PrimeVue)
      .provide('ui', ui)
      .mount('#app');
  }
);


// window.parent.postMessage({op: 'getGlobalProps', requestor: 'vueapp'}, '*');
// window.parent.postMessage({op: 'getAuthToken', requestor: 'vueapp'}, '*');
// window.parent.postMessage({op: 'getUserDetails', requestor: 'vueapp'}, '*');
// window.parent.postMessage({op: 'getAppMenuItems', requestor: 'vueapp'}, '*');
// let count = 3;
// window.addEventListener('message', function(event) {
//   if (event.data.op == 'getGlobalProps') {
//     ui.os = event.data.resp.os || {};
// 
//     let server = ui.os.server || {};
//     http.protocol = server.secure ? 'https' : 'http';
//     http.host = server.hostname;
//     http.port = server.port;
//     http.path = server.app || '..';
//     if (http.path) {
//       http.path += '/';
//     }
// 
//     http.path += 'rest/ng'
// 
//     --count;
//   } else if (event.data.op == 'getAuthToken') {
//     let resp = event.data.resp;
//     ui.token = resp.token;
// 
//     http.headers['X-OS-API-TOKEN'] = ui.token; // localStorage.getItem('osAuthToken');
//     if (resp.impUserToken) {
//       http.headers['X-OS-IMPERSONATE-USER'] = resp.impUserToken;
//     } else {
//       delete http.headers['X-OS-IMPERSONATE-USER'];
//     }
// 
//     --count;
//   } else if (event.data.op == 'getUserDetails') {
//     Object.assign(ui, event.data.resp);
//     --count;
//   } else if (event.data.op == 'getAppMenuItems') {
//     ui.menuItems = event.data.resp;
//   } else if (event.data.op == 'getItems') {
//     itemsSvc.setItems(event.data.type, event.data.items);
//   }
//     
// 
//   if (count == 0) {
//     app.mount('#app')
//     app.provide('ui', ui);
//     count = -1;
//   }
// });


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
