
import AddEditFormRecord  from './AddEditFormRecord.vue';
import FormRecordOverview from './FormRecordOverview.vue';

export default {
  install(app) {
    app.component('os-addedit-form-record',  AddEditFormRecord);
    app.component('os-form-record-overview', FormRecordOverview);
  }
}
