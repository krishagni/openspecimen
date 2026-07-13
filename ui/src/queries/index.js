import QueryServices from './services';

import ColumnText from './views/ColumnText.vue';
import ColumnUrl  from './views/ColumnUrl.vue';

export default {
  install(app) {
    app.use(QueryServices);

    app.component('os-query-column-text', ColumnText);
    app.component('os-query-column-url',  ColumnUrl);
  }
}
