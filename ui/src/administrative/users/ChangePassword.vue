<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{$filters.username(ctx.user)}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-form ref="userForm" :schema="ctx.chgPasswdFs" :data="ctx.passwdDetail">
          <div>
            <os-button primary :label="$t('common.buttons.update')" @click="updatePassword" />
            <os-button text    :label="$t('common.buttons.cancel')" @click="cancel" />
          </div>
        </os-form>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import chgPasswdFs from '@/administrative/schemas/users/change-password-schema.json';

import alertsSvc   from '@/common/services/Alerts.js';
import i18n        from '@/common/services/I18n.js';
import routerSvc   from '@/common/services/Router.js';
import settingsSvc from '@/common/services/Setting.js';
import userSvc     from '@/administrative/services/User.js';

export default {
  name: 'UserChangePassword',

  props: ['userId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      user: {},

      bcrumb: [
        {url: routerSvc.getUrl('UsersList', {userId: -1}), label: i18n.msg('users.list')}
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
        () => {
          alertsSvc.success({code: 'users.password_updated'});
          this.$ui.currentUser.daysBeforePasswordExpiry = -1;
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
