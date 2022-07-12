<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!ctx.group.id">
          <span v-t="'user_groups.create'">Create User Group</span>
        </h3>
        <h3 v-else>
          <span v-t="{path: 'common.update', args: ctx.group}">Update {{ctx.group.name}}</span>
        </h3>
      </span>
    </os-page-head>
    <os-page-body>
      <os-form ref="groupForm" :schema="ctx.addEditFs" :data="ctx.group" @input="handleInput($event)">
        <div>
          <os-button primary :label="$t(!ctx.group.id ? 'common.buttons.create' : 'common.buttons.update')"
            @click="saveOrUpdate" />
          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import groupSchema from '@/administrative/user-groups/schemas/group.js';
import addEditSchema from '@/administrative/user-groups/schemas/addedit.js';

import alertSvc   from '@/common/services/Alerts.js';
import i18n       from '@/common/services/I18n.js';
import routerSvc  from '@/common/services/Router.js';
import itemsSvc   from '@/common/services/ItemsHolder.js';
import userGrpSvc from '@/administrative/services/UserGroup.js';
import formUtil   from '@/common/services/FormUtil.js';

export default {
  props: ['groupId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      group: {},

      bcrumb: [
        {url: routerSvc.getUrl('UserGroupsList'), label: i18n.msg('user_groups.list')}
      ],

      addEditFs: {rows: []}
    });

    if (props.groupId && +props.groupId > 0) {
      userGrpSvc.getGroup(+props.groupId).then(group => ctx.group = group);
    } else {
      let users = itemsSvc.getItems('users');
      itemsSvc.clearItems();
      if (users && users.length > 0) {
        let instituteId = users[0].instituteId;
        for (let user of users) {
          if (user.instituteId != instituteId) {
            alertSvc.error({code: 'user_groups.multi_institute_users_na'});
            routerSvc.back();
            return;
          }
        }

        ctx.group.institute = users[0].instituteName;
        ctx.group.users = users;
      }
    }

    ctx.group.showInstitute = false;
    if (!ctx.group.id && !ctx.group.institute) {
      if (!ui.currentUser.admin) {
        ctx.group.institute = ui.currentUser.instituteName;
      } else {
        ctx.group.showInstitute = true;
      }
    }

    ctx.addEditFs = formUtil.getFormSchema(groupSchema.fields, addEditSchema.layout);
    return { ctx };
  },

  methods: {
    handleInput: function(event) {
      Object.assign(this.ctx.group, event.data);
    },

    saveOrUpdate: function() {
      if(!this.$refs.groupForm.validate()) {
        return;
      }

      let toSave = this.ctx.group;
      userGrpSvc.saveOrUpdate(toSave).then(
        (savedGroup) => {
          alertSvc.success({code: !toSave.id ? 'user_groups.created' : 'user_groups.updated', args: savedGroup});
          routerSvc.back();
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
