<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'forms.list'">Forms</h3>
          </span>

          <template #right>
            <os-list-size
              :list="ctx.forms"
              :page-size="ctx.pageSize"
              :list-size="ctx.formsCount"
              @updateListSize="getFormsCount"
              v-if="!ctx.detailView"
            />

            <os-button size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable" v-else
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span v-if="ctx.selectedForms && ctx.selectedForms.length > 0">
                <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteForms" />
              </span>
              <span v-else>
                <os-button left-icon="plus" :label="$t('common.buttons.create')"
                  @click="showCreateFormDialog" />

                <os-button left-icon="upload" :label="$t('common.buttons.import')"
                  @click="showImportFormDialog" />

                <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                  url="https://openspecimen.atlassian.net/wiki/x/DYAHaQ" new-tab="true" />
              </span>
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :context="ctx.ui"
            :data="ctx.forms"
            :selected="ctx.selectedForm"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            :showRowActions="true"
            @filtersUpdated="loadForms"
            @rowClicked="onFormRowClick"
            @selectedRows="onFormsSelection"
            ref="listView">
            <!-- template #rowActions="slotProps">
              <os-button-group
              </os-button-group>
            </template -->
          </os-list-view>
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="ctx.selectedForm && ctx.selectedForm.form && ctx.selectedForm.form.formId > 0">
      <router-view :form="ctx.selectedForm.form" :key="$route.params.formId" />
    </os-screen-panel>

    <os-dialog ref="createFormDialog">
      <template #header>
        <span v-t="'forms.create_form'">Create Form</span>
      </template>
      <template #content>
        <os-form ref="addForm" :schema="addSchema.layout" :data="ctx" />
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="closeCreateFormDialog" />
        <os-button primary :label="$t('common.buttons.create')" @click="createForm" />
      </template>
    </os-dialog>

    <os-dialog ref="importFormDialog">
      <template #header>
        <span v-t="'forms.import_form'">Create Form</span>
      </template>
      <template #content>
        <os-form ref="importForm" :schema="{rows: []}" :data="{}">
          <template v-slot:[`static-fields`]>
            <div class="row">
              <div class="field">
                <os-label>
                  <span v-t="'forms.select_form_xml'">Select form XML file</span>
                </os-label>
                <os-file-upload ref="importFormUploader" :url="importFormUrl" :headers="reqHeaders" :auto="false" />
              </div>
            </div>
          </template>
        </os-form>
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="closeImportFormDialog" />
        <os-button primary :label="$t('common.buttons.import')" @click="importForm" />
      </template>
    </os-dialog>

    <os-confirm-delete ref="deleteFormsDialog" :captcha="true">
      <template #message>
        <span v-t="'forms.delete_selected'">You will lose data if you delete the selected forms. Do you want to continue?</span>
      </template>
    </os-confirm-delete>
  </os-screen>
</template>

<script>

import addSchema  from '@/administrative/schemas/forms/add-form.js';
import listSchema from '@/administrative/schemas/forms/list.js';

import http       from '@/common/services/HttpClient.js';
import formsSvc   from '@/forms/services/Form.js';
import routerSvc  from '@/common/services/Router.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['formId', 'filters'],

  data() {
    return {
      ctx: {
        ui: this.$ui,

        forms: [],

        formsCount: -1,

        loading: true,

        query: this.filters,

        detailView: false,

        selectedForm: null,

        selectedForms: [],

        form: {}
      },

      addSchema,

      listSchema
    };
  },

  watch: {
    formId: function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      if (newVal < 0) {
        this.showTable(newVal);
      } else {
        this._showDetails(newVal);
      }
    },

    'ctx.form.caption': function(caption) {
      this.ctx.form.name = util.toSnakeCase(caption).substring(0, 64);
    }

  },

  computed: {
    importFormUrl: function() {
      return http.getUrl('forms/definition-zip');
    },

    reqHeaders: function() {
      return http.headers;
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    showCreateFormDialog: function() {
      this.ctx.form = {};
      this.$refs.createFormDialog.open();
    },

    closeCreateFormDialog: function() {
      this.$refs.createFormDialog.close();
    },

    createForm: function() {
      if (!this.$refs.addForm.validate()) {
        return;
      }

      formsSvc.createForm(this.ctx.form).then(
        ({id: formId}) => {
          this.closeCreateFormDialog();
          this._reloadForms().then(
            () => routerSvc.goto('FormsListItemDetail.Overview', {formId})
          );
        }
      );
    },

    showImportFormDialog: function() {
      this.$refs.importFormDialog.open();
    },

    closeImportFormDialog: function() {
      this.$refs.importFormDialog.close();
    },

    importForm: function() {
      this.$refs.importFormUploader.upload().then(
        () => {
          this.$refs.listView.reload();
          this.closeImportFormDialog();
        }
      );
    },

    deleteForms: function() {
      this.$refs.deleteFormsDialog.open().then(
        resp => {
          if (resp == 'proceed') {
            formsSvc.deleteForms(this.ctx.selectedForms.map(form => form.formId)).then(
              () => {
                this.$refs.listView.reload();
              }
            );
          }
        }
      );
    },

    loadForms: async function({filters, uriEncoding, pageSize}) {
      this.ctx.query = uriEncoding;
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;
      if (this.formId < 0) {
        this._reloadForms().then(
          () => routerSvc.goto('FormsList', {formId: -1}, {filters: uriEncoding})
        );
      } else {
        this._showDetails(this.formId);
      }
    },

    getFormsCount: function() {
      this.ctx.formsCount = -1;
      let opts = Object.assign({excludeSysForms: true}, this.ctx.filterValues);
      formsSvc.getFormsCount(opts).then(({count}) => this.ctx.formsCount = count);
    },

    onFormRowClick: function({form: {formId}}) {
      routerSvc.goto('FormsListItemDetail.Overview', {formId}, {filters: this.ctx.query});
    },

    onFormsSelection: function(selection) {
      this.ctx.selectedForms = (selection || []).map(({rowObject: {form}}) => form);
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('FormsList', {formId: -1}, {filters: this.ctx.query});
      if (reload) {
        this.$refs.listView.reload();
      } 
    },

    _showDetails: async function(formId) {
      let selected = this.ctx.forms.find(({form}) => form.formId == formId);
      if (!selected) {
        const forms = await this._reloadForms(formId);
        selected = forms.find(({form}) => form.formId == formId);
      }

      this.ctx.selectedForm = selected;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    _reloadForms: async function(formId) {
      let opts = Object.assign(
        {formId, excludeSysForms: true, includeStats: true, maxResults: this.ctx.pageSize},
        this.ctx.filterValues || {}
      );
      opts.cpId = opts.allCps ? -1 : null;

      this.ctx.loading = true;
      return formsSvc.getForms(opts).then(
        forms => {
          this.ctx.loading = false;
          this.ctx.forms = forms.map(form => ({form}));
          return this.ctx.forms;
        }
      );
    }
  }
}
</script>
