<template>
  <Page>
    <PageHeader>
      <template #breadcrumb>
        <Breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!ctx.group.id">Create User Group</h3>
        <h3 v-else>Update {{ctx.group.name}}</h3>
      </span>
    </PageHeader>
    <PageBody>
      <Form ref="groupForm" :schema="ctx.addEditFs" :data="ctx.group" @input="handleUserChange($event)">
        <div>
          <Button label="Create" v-if="!ctx.group.id" @click="saveOrUpdate"/>
          <Button label="Update" v-else @click="saveOrUpdate"/>
          <Button label="Cancel" @click="cancel"/>
        </div>
      </Form>
    </PageBody>
  </Page>
</template>

<script>
import { reactive, inject } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody from '@/common/components/PageBody.vue';
import Button from '@/common/components/Button.vue';
import Form from '@/common/components/Form.vue';

import groupSchema from '@/administrative/user-groups/schemas/group.js';
import addEditSchema from '@/administrative/user-groups/schemas/addedit.js';

import alertSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import itemsSvc from '@/common/services/ItemsHolder.js';
import userGrpSvc from '@/administrative/services/UserGroup.js';
import formUtil from '@/common/services/FormUtil.js';

export default {
  props: ['groupId'],

  inject: ['ui'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    Form,
    Button
  },

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      group: {},

      bcrumb: [
        {url: routerSvc.getUrl('UserGroupsList'), label: 'User Groups'}
      ],

      addEditFs: {rows: []},

      bulkEditFs: {rows: []}
    });

    if (props.groupId && +props.groupId > 0) {
      userGrpSvc.getUserGroup(+props.groupId).then(group => ctx.group = group);
    } else {
      let users = itemsSvc.getItems('users');
      itemsSvc.clearItems();
      if (users && users.length > 0) {
        let instituteId = users[0].instituteId;
        for (let user of users) {
          if (user.instituteId != instituteId) {
            alertSvc.error('Users of multiple institutes cannot be added to the group.');
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
    handleUserChange: function(event) {
      Object.assign(this.ctx.group, event.data);
    },

    saveOrUpdate: function() {
      if(!this.$refs.groupForm.validate()) {
        return;
      }

      let toSave = this.ctx.group;
      userGrpSvc.saveOrUpdate(toSave).then(
        (savedGroup) => {
          let action  = !toSave.id || toSave.id == -1 ? ' created!' : ' updated!'
          alertSvc.success('User group ' + savedGroup.name + action);
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
