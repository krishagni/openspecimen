
<template>
  <div class="os-form-associations-container">
    <os-page-toolbar>
      <template #default>
        <os-button left-icon="plus"    :label="$t('common.buttons.add')"   @click="showAddAssociationDialog" />
        <os-button left-icon="history" :label="$t('forms.view_revisions')" @click="showAssociationRevisions" />
      </template>
    </os-page-toolbar>

    <os-list-view
      :data="ctx.associations"
      :schema="listSchema"
      :loading="ctx.loading"
      :showRowActions="true"
      :noRecordsMsg="'forms.no_associations'"
      ref="listView">
      <template #rowActions="slotProps">
        <os-button-group>
          <os-button size="small" left-icon="edit" @click="showEditAssociationDialog(slotProps.rowObject)"
            v-if="slotProps.rowObject.formContext.allowEdits" />
          <os-button size="small" left-icon="trash" @click="deleteAssociation(slotProps.rowObject)"
            v-if="slotProps.rowObject.formContext.allowDelete" />
        </os-button-group>
      </template>
    </os-list-view>

    <os-dialog ref="addAssociationDialog">
      <template #header>
        <span v-t="'forms.add_association'">Add Association</span>
      </template>
      <template #content>
        <os-form class="association-form" ref="addAssociationForm" :schema="addSchema.layout" :data="ctx" />
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="closeAddAssociationDialog" />
        <os-button primary :label="$t('common.buttons.add')"    @click="addAssociation" />
      </template>
    </os-dialog>

    <os-dialog ref="editAssociationDialog">
      <template #header>
        <span v-t="'forms.edit_association'">Edit Association</span>
      </template>
      <template #content>
        <os-form class="association-form" ref="editAssociationForm" :schema="editSchema.layout" :data="ctx" />
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="closeEditAssociationDialog" />
        <os-button primary :label="$t('common.buttons.update')" @click="editAssociation" />
      </template>
    </os-dialog>

    <os-confirm-delete ref="confirmDeleteAssociationDialog" :captcha="false">
      <template #message>
        <span v-t="{path: 'forms.confirm_delete_association', args: ctx.association}"></span>
      </template>
    </os-confirm-delete>

    <os-dialog ref="revisionsDialog">
      <template #header>
        <span v-t="{path: 'forms.association_revs', args: form}"></span> 
      </template>
      <template #content>
        <os-list-view :data="ctx.revisions" :schema="revsListSchema" />
      </template>
      <template #footer>
        <os-button primary :label="$t('common.buttons.done')" @click="closeAssociationRevisionsDialog" />
      </template>
    </os-dialog>
  </div>
</template>

<script>
import i18n    from '@/common/services/I18n.js';
import formSvc from '@/forms/services/Form.js';
import util    from '@/common/services/Util.js';

import addSchema      from '@/administrative/schemas/forms/add-association.js';
import editSchema     from '@/administrative/schemas/forms/edit-association.js';
import listSchema     from '@/administrative/schemas/forms/associations-list.js';
import revsListSchema from '@/administrative/schemas/forms/association-revisions.js';

export default {
  props: ['form'],

  data() {
    return {
      ctx: {
        currentUser: this.$ui.currentUser,

        associations: [],

        association: {},

        revisions: []
      },

      addSchema,

      editSchema,

      listSchema,

      revsListSchema
    };
  },

  created() {
    this._loadAssociations();
    this.ctx.getEntityTypes = this.getEntityTypes;
  },

  methods: {
    showAddAssociationDialog: function() {
      this.ctx.association = {formId: this.form.formId};
      this.$refs.addAssociationDialog.open();
    },

    closeAddAssociationDialog: function() {
      this.$refs.addAssociationDialog.close();
    },

    addAssociation: function() {
      if (!this.$refs.addAssociationForm.validate()) {
        return;
      }

      this._saveAssociation(this.ctx.association, this.closeAddAssociationDialog);
    },

    showEditAssociationDialog: function({formContext}) {
      const association = this.ctx.association = util.clone(formContext);
      if (formSvc.isCpBasedAssociationType(association.level.entityType)) {
        association.allCps = !association.collectionProtocol.id || association.collectionProtocol.id < 0;
      } else if (formSvc.isInstituteBasedAssociationType(association.level.entityType)) {
        association.allInstitutes = !association.instituteName;
      }

      this.$refs.editAssociationDialog.open();
    },

    closeEditAssociationDialog: function() {
      this.$refs.editAssociationDialog.close();
    },

    editAssociation: function() {
      if (!this.$refs.editAssociationForm.validate()) {
        return;
      }

      this._saveAssociation(this.ctx.association, this.closeEditAssociationDialog);
    },

    deleteAssociation: function({formContext}) {
      this.ctx.association = formContext;
      this.$refs.confirmDeleteAssociationDialog.open().then(
        (resp) => {
          if (resp == 'proceed') {
            formSvc.deleteAssociation(formContext.formCtxtId).then(() => this._loadAssociations()); 
          }
        }
      );
    },

    showAssociationRevisions: function() {
      formSvc.getAssociationRevisions(this.form.formId).then(
        revisions => {
          this.ctx.revisions = revisions.map(
            revision => {
              const {entityTypeLabel} = formSvc.getEntityType(revision.entityType);
              revision.entityTypeLabel = entityTypeLabel;
              return {revision};
            }
          );
          this.$refs.revisionsDialog.open();
        }
      );
    },

    closeAssociationRevisionsDialog: function() {
      this.$refs.revisionsDialog.close();
    },

    getEntityTypes: async function() {
      return formSvc.getEntityTypes().filter(entityType => !entityType.forbidAdd)
    },

    _loadAssociations: async function() {
      const associations = await formSvc.getFormAssociations(this.form.formId);
      this.ctx.associations = associations.map(
        association => {
          association.allowEdits  = formSvc.isAssociationEditAllowed(association.level);
          association.allowDelete = formSvc.isAssociationDeleteAllowed(association.level); 
          association.group = association.instituteName || association.collectionProtocol.shortTitle;
          if (!association.group) {
            if (formSvc.isCpBasedAssociationType(association.level)) {
              association.group = i18n.msg('forms.all_cps');
              association.allCps = true;
              association.allowEdits = association.allowDelete = this.$ui.currentUser.admin;
            } else if (formSvc.isInstituteBasedAssociationType(association.level)) {
              association.group = i18n.msg('forms.all_institutes');
              association.allInstitutes = true;
              association.allowEdits = association.allowDelete = this.$ui.currentUser.admin;
            } else {
              association.group = i18n.msg('forms.not_applicable');
            }
          }

          association.level = formSvc.getEntityType(association.level);
          return {formContext: association};
        }
      )
    },

    _getAssociationModel: function(association) {
      const {
        formCtxtId, formId, level, collectionProtocol: cp, allCps,
        notifEnabled, dataInNotif, notifUserGroups,
        entityId, allInstitutes, multiRecord
      } = association;

      const payload = {
        formCtxtId, formId, level: level.entityType,
        collectionProtocol: allCps ? {id: -1} : (cp && cp.id > 0 ? cp : {id: -1}),
        entityId: level.cpBased ? null : (allInstitutes ? -1 : entityId),
        multiRecord, notifEnabled, dataInNotif, notifUserGroups
      };

      if (!formCtxtId || formCtxtId < 0) {
        if (formSvc.isInstituteBasedAssociationType(level.entityType) && !this.$ui.currentUser.admin) {
          payload.entityId = this.$ui.currentUser.instituteId;
        }
      }

      return payload
    },

    _saveAssociation: function(association, closeDialog) {
      const payload = this._getAssociationModel(association);
      formSvc.saveAssociation(payload).then(
        () => {
          this._loadAssociations();
          closeDialog();
        }
      );
    }
  }
}
</script>

<style scoped>
.os-form-associations-container {
  overflow-y: auto;
}

.association-form :deep(.p-field-checkbox) {
  margin-bottom: 0rem;
}
</style>
