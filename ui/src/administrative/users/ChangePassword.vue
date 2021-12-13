<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{ctx.user.firstName}} {{ctx.user.lastName}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-form ref="userForm" :schema="ctx.chgPasswdFs" :data="ctx.passwdDetail">
          <div>
            <os-button label="Update" @click="updatePassword" />
            <os-button label="Cancel" @click="cancel" />
          </div>
        </os-form>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import chgPasswdFs from '@/administrative/schemas/users/change-password-schema.json';

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import settingsSvc from '@/common/services/Setting.js';
import userSvc from '@/administrative/services/User.js';

export default {
  name: 'UserChangePassword',

  props: ['userId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      user: {},

      bcrumb: [
        {url: routerSvc.getUrl('UsersList'), label: 'Users'}
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
