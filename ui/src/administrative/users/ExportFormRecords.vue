<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <h3 v-t="'export.export_records'">Export Records</h3>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12">
          <os-form ref="exportForm" :schema="formSchema" :data="ctx">
            <os-button primary :label="$t('common.buttons.submit')" @click="exportRecords" />
            <os-button text    :label="$t('common.buttons.cancel')" @click="cancel" />
          </os-form>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import exportSvc   from '@/common/services/ExportService.js';
import formSvc     from '@/forms/services/Form.js';
import itemsSvc    from '@/common/services/ItemsHolder.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  data() {
    return {
      ctx: {
        recordType: null,

        getRecordTypes: this._getRecordTypes
      }
    };
  },

  created() {
    const users = itemsSvc.getItems('users');
    itemsSvc.clearItems('users');
    if (users && users.length > 0) {
      this.ctx.emailIds = users.map(user => user.emailAddress).join(',');
    } else {
      this.ctx.emailIds = null;
    }
  },

  computed: {
    bcrumb: function() {
      return [ {url: routerSvc.getUrl('UsersList', {userId: -1}), label: this.$t('users.list')} ];
    },

    formSchema: function() {
      return {
        rows: [
          {
            fields: [
              {
                type: 'group-single-select',
                name: 'recordType',
                labelCode: 'export.record_type',
                listSource: {
                  displayProp: 'title',
                  groupNameProp: 'group',
                  groupItemsProp: 'types',
                  loadFn: ({context}) => context.formData.getRecordTypes()
                },
                validations: {
                  required: {
                    messageCode: 'export.record_type_req'
                  }
                }
              }
            ]
          },
          {
            fields: [
              {
                type: 'textarea',
                name: 'emailIds',
                labelCode: 'users.email_ids',
                placeholder: this.$t('users.scan_email_ids')
              }
            ]
          }
        ]
      }
    }
  },

  methods: {
    exportRecords: async function() {
      if (!this.$refs.exportForm.validate()) {
        return;
      }

      const {recordType: {type, params}, emailIds} = this.ctx;
      const jobParams = {emailIds, ...(params || {})};

      const payload = { objectType: type, params: jobParams };
      exportSvc.exportRecords(payload).then(() => routerSvc.goto('UsersList', {userId: -1}));
    },

    cancel: function() {
      routerSvc.back();
    },

    _getRecordTypes: async function() {
      const {currentUser} = this.$ui;
      const entityId = (!currentUser.admin && currentUser.instituteId) || null;

      const userForms    = await formSvc.getForms({formType: 'User', entityId});
      const profileForms = await formSvc.getForms({formType: 'UserProfile', entityId});
      return [
        {
          group: this.$t('users.user_forms'),
          types: userForms.map(form => ({
            id: 'userForm_' + form.formId,
            type: 'userExtensions',
            title: form.caption,
            params: { entityType: 'User', entityId, formName: form.name }
          }))
        },
        {
          group: this.$t('users.profile_forms'),
          types: profileForms.map(form => ({
            id: 'profileForm_' + form.formId,
            type: 'userExtensions',
            title: form.caption,
            params: { entityType: 'UserProfile', entityId, formName: form.name }
          }))
        }
      ];
    }
  }
}
</script>
