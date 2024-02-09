
import AddEditFormRecord  from './AddEditFormRecord.vue';
import AddEditFormRecordView  from './AddEditFormRecordView.vue';
import FormsList from './FormsList.vue';
import FormRecordOverview from './FormRecordOverview.vue';

export default {
  install(app) {
    app.component('os-addedit-form-record',  AddEditFormRecord);
    app.component('os-addedit-form-record-view',  AddEditFormRecordView);
    app.component('os-forms-list', FormsList);
    app.component('os-form-record-overview', FormRecordOverview);
  }
}
