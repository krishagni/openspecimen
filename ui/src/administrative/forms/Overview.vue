<template>
  <div class="os-form-designer-container">
    <os-page-toolbar>
      <template #default>
        <os-button left-icon="code" :label="$t('forms.download_xml')" @click="downloadXml" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteForm" />

        <os-button left-icon="history" :label="$t('audit.trail')" @click="viewRevisions" />
      </template>
    </os-page-toolbar>

    <Designer :form-def="ctx.formDef" @form-saved="onSave" v-if="ctx.formDef.id > 0" />

    <os-dialog ref="revisionsDialog">
      <template #header>
        <span v-t="'audit.trail'">Audit Trail</span>
      </template>
      <template #content>
        <os-list-view :data="ctx.revisions" :schema="revsListSchema" :showRowActions="true">
          <template #rowActions="slotProps">
            <os-button-group>
              <os-button size="small" left-icon="eye"  v-os-tooltip.bottom="$t('forms.view_preview')"
                @click="showRevisionPreview(slotProps.rowObject)" />
              <os-button size="small" left-icon="code" v-os-tooltip.bottom="$t('forms.download_xml')"
                @click="downloadRevisionXml(slotProps.rowObject)" />
            </os-button-group>
          </template>
        </os-list-view>
      </template>
      <template #footer>
        <os-button primary :label="$t('common.buttons.done')" @click="closeRevisionsDialog" />
      </template>
    </os-dialog>

    <os-dialog ref="revisionPreviewDialog">
      <template #header>
        <span>{{ctx.revTitle}}</span>
      </template>
      <template #content>
        <os-form :schema="ctx.revSchema" :data="ctx.revRecord" />
      </template>
      <template #footer>
        <os-button primary :label="$t('common.buttons.done')" @click="closeRevPreviewDialog" />
      </template>
    </os-dialog>

    <os-delete-object ref="deleteFormDialog" :input="ctx.deleteOpts" />
  </div>
</template>

<script>

import formSvc from '@/forms/services/Form.js';
import formUtil from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';

import Designer from './designer/Designer.vue';

import revsListSchema from '@/administrative/schemas/forms/revisions.js';

export default {
  props: ['form'],

  components: {
    Designer
  },

  data() {
    return {
      ctx: {
        formDef: {},

        revisions: [],

        revSchema: {rows: []},

        deleteOpts: {}
      },

      revsListSchema
    };
  },

  created() {
    formSvc.getDefinition(this.form.formId).then(formDef => this.ctx.formDef = formDef);
    this.ctx.deleteOpts = {
      dependents: this._getDependents,
      deleteObj: this._deleteForm,
      forceDelete: true,
      title: this.form.caption
    }
  },

  methods: {
    downloadXml: function() {
      formSvc.downloadDefinition(this.form.formId);
    },

    deleteForm: function() {
      this.$refs.deleteFormDialog.execute().then(
        resp => {
          if (resp == 'deleted') {
            routerSvc.goto('FormsList', {formId: -1});
          }
        }
      );
    },

    viewRevisions: function() {
      formSvc.getRevisions(this.form.formId).then(
        revisions => {
          this.ctx.revisions = revisions.map(revision => ({revision}));
          this.$refs.revisionsDialog.open();
        }
      );
    },

    closeRevisionsDialog: function() {
      this.$refs.revisionsDialog.close();
    },

    showRevisionPreview: function({revision}) {
      const {id: formId, rev} = this.ctx.revision = revision;
      formSvc.getRevisionDef(formId, rev).then(
        formDef => {
          const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef);
          this.ctx.revTitle = formDef.caption;
          this.ctx.revSchema = schema;
          this.ctx.revRecord = defaultValues;
          this.$refs.revisionPreviewDialog.open();
        }
      );
    },

    closeRevPreviewDialog: function() {
      this.$refs.revisionPreviewDialog.close();
    },

    downloadRevisionXml: function({revision: {id: formId, rev}}) {
      formSvc.downloadRevision(formId, rev);
    },

    onSave: function({form: {caption}}) {
      Object.assign(this.form, {caption});
    },

    _getDependents: function() {
      return formSvc.getDependents(this.form.formId);
    },

    _deleteForm: function() {
      return formSvc.deleteForm(this.form.formId);
    }
  }
}
</script>

<style scoped>
.os-form-designer-container {
  overflow-y: auto;
}
</style>
