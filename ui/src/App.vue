<template>
  <div class="os-root" v-if="loading">
    <span>Loading...</span>
  </div>

  <div class="os-root" v-else>
    <os-navbar :hide-buttons="navCtrls.hideButtons" />

    <div class="os-user-impersonate-warn" v-if="(ui.currentUser && (ui.currentUser.impersonated || (ui.currentUser.daysBeforePasswordExpiry >= 0 && ui.currentUser.daysBeforePasswordExpiry <= 5))) || ui.global.appProps.auditEnabled == 'false' || ui.global.appProps.auditEnabled == false">
      <div class="text" v-if="ui.currentUser && ui.currentUser.impersonated">
        <span>You are viewing {{$filters.username(ui.currentUser)}}'s account.
        <a @click="returnToMyAccount">Return back to your account.</a></span>
      </div>
      <div class="text" v-if="ui.currentUser && ui.currentUser.daysBeforePasswordExpiry >= 0 && ui.currentUser.daysBeforePasswordExpiry <= 5">
        <span>Your password will expire in {{ui.currentUser.daysBeforePasswordExpiry}} {{ui.currentUser.daysBeforePasswordExpiry != 1 ? 'days' : 'day'}}. </span>
        <router-link :to="{name: 'UserChangePassword', params: {userId: ui.currentUser.id}}">
          <span>Reset Password</span>
        </router-link>
      </div>
      <div class="text" v-if="ui.global.appProps.auditEnabled == 'false' || ui.global.appProps.auditEnabled == false">
        <div class="text">
          <span>Auditing is disabled. Ensure auditing is enabled in the live environment.</span>
        </div>
      </div>
    </div>

    <os-app-body />
  </div>

  <teleport to="body">
    <os-mask ref="mask" />
  </teleport>
</template>

<script>
import AppBody from '@/common/components/AppBody.vue';
import Navbar from '@/common/components/Navbar.vue';

import authSvc from '@/common/services/Authorization.js';
import homePageSvc from '@/common/services/HomePageService.js';
import routerSvc from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';
import urlResolver from '@/common/services/UrlResolver.js';
import userSvc   from '@/administrative/services/User.js';
import util      from '@/common/services/Util.js';

export default {
  name: 'App',

  inject: ['ui'],

  components: {
    'os-navbar': Navbar,
    'os-app-body': AppBody
  },

  setup() {
  },

  data() {
    return {
      loading: true,

      navCtrls: { }
    }
  },

  provide() {
    return {navCtrls: this.navCtrls};
  },

  mounted() {
    util.setMask(this.$refs.mask);
  },

  created() {
    let currUserQ  = userSvc.getCurrentUser();
    let usrStateQ  = userSvc.getUiState();
    let usrRightsQ = authSvc.loadUserRights();
    let spmnPropsQ = util.loadSpecimenTypeProps();
    let spmnUnitsQ = util.loadSpecimenUnits();
    Promise.all([currUserQ, usrStateQ, usrRightsQ, spmnPropsQ, spmnUnitsQ]).then(
      (resps) => {
        this.$ui.currentUser = resps[0];
        const state = this.$ui.global.state = resps[1] || {};
        if (!state.widgets) {
          state.widgets = [];
        }

        this.registerHomePageCards();
        this.registerUrls();

        this.loading = false;
        const {name} = routerSvc.getCurrentRoute();
        if (name == 'App') {
          setTimeout(() => routerSvc.goto('HomePage'), 0);
        }
      }
    );
  },

  methods: {
    returnToMyAccount: function() {
      userSvc.unpersonate().then(() => routerSvc.reloadView('HomePage'));
    },

    registerHomePageCards: function() {
      const t = this.$t;
      const ui = this.$ui;
      homePageSvc.registerCards([
        {
          showIf: {resource: 'CollectionProtocol', operations: ['Read']},
          route: {name: 'CpsList'},
          icon: 'fa fa-calendar',
          title: t('common.home.cps'),
          description: t('common.home.cps_desc')
        },

        {
          showIf: 'admin',
          route: {name: 'InstitutesList', params: {instituteId: -1}},
          icon: 'fa fa-institution',
          title: t('common.home.institutes'),
          description: t('common.home.institutes_desc')
        },

        {
          showIf: {resource: 'User', operations: ['Create', 'Update']},
          route: {name: 'UsersList', params: {userId: -1}},
          icon: 'fa fa-user',
          title: t('common.home.users'),
          description: t('common.home.users_desc')
        },

        {
          showIf: {resource: 'User', operations: ['Create', 'Update']},
          route: {name: 'UserRolesList', params: {roleId: -1}},
          icon: 'fa fa-lock',
          title: t('common.home.roles'),
          description: t('common.home.roles_desc')
        },

        {
          showIf: 'institute-admin',
          route: {name: 'SitesList', params: {siteId: -1}},
          icon: 'fa fa-hospital-o',
          title: t('common.home.sites'),
          description: t('common.home.sites_desc')
        },

        {
          showIf: {resource: 'StorageContainer', operations: ['Read']},
          route: {name: 'ContainersList'},
          icon: 'fa fa-dropbox',
          title: t('common.home.containers'),
          description: t('common.home.containers_desc')
        },

        {
          showIf: {resource: 'Query', operations: ['Read']},
          route: {name: 'QueriesList'},
          icon: 'fa fa-dashboard',
          title: t('common.home.queries'),
          description: t('common.home.queries_desc')
        },

        {
          showIf: {resources: ['Specimen', 'PrimarySpecimen'], operations: ['Read']},
          route: {name: 'SpecimenCartsList', params: {cartId: -1}},
          icon: 'fa fa-shopping-cart',
          title: t('common.home.carts'),
          description: t('common.home.carts_desc')
        },

        {
          route: {name: 'FormsList', params: {formId: -1}},
          icon: 'fa fa-copy',
          title: t('common.home.forms'),
          description: t('common.home.forms_desc'),
          showIf: () => ui.currentUser.admin || ui.currentUser.instituteAdmin || ui.currentUser.manageForms
        },

        {
          showIf: {resource: 'DistributionProtocol', operations: ['Read']},
          route: {name: 'DpsList', params: {dpId: -1}},
          icon: 'fa fa-truck',
          title: t('common.home.dps'),
          description: t('common.home.dps_desc')
        },

        {
          showIf: {resource: 'Order', operations: ['Read']},
          route: {name: 'OrdersList', params: {orderId: -1}},
          icon: 'fa fa-share',
          title: t('common.home.orders'),
          description: t('common.home.orders_desc')
        },

        {
          showIf: {resource: 'ShippingAndTracking', operations: ['Read']},
          route: {name: 'ShipmentsList', params: {shipmentId: -1}},
          icon: 'fa fa-paper-plane-o',
          title: t('common.home.shipments'),
          description: t('common.home.shipments_desc')
        },

        {
          showIf: {resource: 'ScheduledJob', operations: ['Read']},
          route: {name: 'JobsList'},
          icon: 'fa fa-gears',
          title: t('common.home.jobs'),
          description: t('common.home.jobs_desc')
        },

        {
          showIf: 'institute-admin',
          route: {name: 'ConsentStatementsList'},
          icon: 'fa fa-file-text-o',
          title: t('common.home.consents'),
          description: t('common.home.consents_desc')
        },

        {
          href: async () => {
            let setting = await settingSvc.getSetting('training', 'training_url');
            return setting[0].value;
          },
          icon: 'fa fa-graduation-cap',
          title: t('common.home.training'),
          description: t('common.home.training_desc'),
          newTab: true
        },

        {
          showIf: 'admin',
          route: {name: 'SettingsList'},
          icon: 'fa fa-wrench',
          title: t('common.home.settings'),
          description: t('common.home.settings_desc'),
        }
      ]);
    },

    registerUrls: function() {
      urlResolver.registerUrl('cp-overview',    'CpDetail.Overview',    'cpId');
      urlResolver.registerUrl('dp-overview',    'DpDetail.Overview',    'dpId');
      urlResolver.registerUrl('folder-queries', 'QueriesList',          null, 'folderId');
      urlResolver.registerUrl('job-run-log',    'JobRunsList',          'jobId');
      urlResolver.registerUrl('order-overview', 'OrderDetail.Overview', 'orderId');
      urlResolver.registerUrl('specimen-list',  'CartSpecimensList',    'cartId');
      urlResolver.registerUrl('user-overview',  'UserDetail.Overview',  'userId');
      urlResolver.registerUrl('user-password-change', 'UserDetail.Overview', 'userId');
      urlResolver.registerUrl('user-roles',     'UserDetail.Roles',     'userId');
    }
  }
}
</script>

<style scoped>
.os-user-impersonate-warn {
  position: absolute;
  left: 0;
  right: 0;
  width: 40%;
  margin: auto;
  z-index: 10;
  background: #c33;
  padding: 0.25rem 0.5rem;
  color: #fff;
  border-bottom-left-radius: 0.25rem;
  border-bottom-right-radius: 0.25rem;
  text-align: center;
}

.os-user-impersonate-warn a {
  color: #fff;
  text-decoration-line: underline;
}
</style>
