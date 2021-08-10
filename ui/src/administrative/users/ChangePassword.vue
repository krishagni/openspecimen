<template>
  <Page>
    <PageHeader>
      <template #breadcrumb>
        <Breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <span>
          <h3>{{ctx.user.firstName}} {{ctx.user.lastName}}</h3>
        </span>
      </span>
    </PageHeader>
    <PageBody>
      <div>
        <Form ref="userForm" :schema="ctx.chgPasswdFs" :data="ctx.passwdDetail">
          <div>
            <Button label="Update" @click="updatePassword"/>
            <Button label="Cancel" @click="cancel"/>
          </div>
        </Form>
      </div>
    </PageBody>
  </Page>
</template>

<script>
import { reactive, inject } from 'vue';

import Page       from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody   from '@/common/components/PageBody.vue';

import Button     from '@/common/components/Button.vue';
import Form       from '@/common/components/Form.vue';

import chgPasswdFs from '@/administrative/users/change-password-schema.json';

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import settingsSvc from '@/common/services/Settings.js';
import userSvc from '@/administrative/services/User.js';

export default {
  name: 'UserChangePassword',

  props: ['userId'],

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
      user: {},

      bcrumb: [
        {url: ui.ngServer + '#/users', label: 'Users', target: '_parent'}
      ],

      chgPasswdFs: {rows: []},

      passwdDetail: {
        userId: +props.userId,

        currentUser: ui.currentUser
      }
    });

    userSvc.getUserById(+props.userId).then(user => ctx.user = user);
    
    settingsSvc.getPasswordRules().then(
      function(rules) {
        if (rules.pattern) {
          let schema = JSON.parse(JSON.stringify(chgPasswdFs));
          for (let row of schema.rows) {
            for (let field of row.fields) {
              if (field.name == 'newPassword') {
                field.validations = field.validations || {};
                let vr = field.validations['pattern'] = field.validations['pattern'] || {};
                Object.assign(vr, {expr: rules.pattern, message: rules.desc});
                break;
              }
            }
          }

          ctx.chgPasswdFs = schema;
        } else {
          ctx.chgPasswdFs = chgPasswdFs;
        }
      }
    );

    return { ctx };
  },

  methods: {
    updatePassword: function() {
      if(!this.$refs.userForm.validate()) {
        return;
      }

      let form = this.ctx.passwdDetail;
      let payload = {
        userId: form.userId,
        oldPassword: form.oldPassword,
        newPassword: form.newPassword
      }

      userSvc.updatePassword(payload).then(
        function() {
          alertsSvc.success('Password updated!');
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
