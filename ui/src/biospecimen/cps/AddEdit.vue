<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span>
        <h3>
          <span v-if="!dataCtx.cp.id" v-t="'cps.create_cp'">Create Collection Protocol</span>
          <span v-else v-t="{path: 'common.update', args: {name: description}}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="cpForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <template v-slot:[`cp.sites`]>
          <span v-if="dataCtx.codingEnabled">
            <a v-if="!dataCtx.showSiteCodeInfo" @click="toggleSiteCodeInfo">
              <span v-t="'cps.show_site_code_info'">Show Site Code Information</span>
            </a>
            <a v-else @click="toggleSiteCodeInfo">
              <span v-t="'cps.hide_site_code_info'">Hide Site Code Information</span>
            </a>
          </span>
        </template>
        <div>
          <os-button primary :label="$t(!dataCtx.cp.id ? 'common.buttons.create' : 'common.buttons.update')"
            @click="saveOrUpdate()" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import formUtil from '@/common/services/FormUtil.js';
import i18n  from '@/common/services/I18n.js';
import routerSvc  from '@/common/services/Router.js';
import util  from '@/common/services/Util.js';

export default {
  props: ['cpId', 'copyOf'],

  inject: ['ui'],

  data() {
    const {global: {appProps}} = this.ui;
    return {
      ctx: {
        addEditFs: {rows: []}
      },

      dataCtx: {
        codingEnabled: appProps.cp_coding_enabled,

        showSiteCodeInfo: false,

        cp: { },

        objName: 'cp',

        objCustomFields: 'cp.extensionDetail.attrsMap'
      }
    }
  },

  async created() {
    this._setupCp();
  },

  watch: {
    cpId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupCp();
      }
    },

    copyOf: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupCp();
      }
    },
  },

  computed: {
    bcrumb: function() {
      return [
        {url: routerSvc.getUrl('CpsList', {cpId: -1}), label: i18n.msg('cps.list')}
      ];
    },

    description: function() {
      return this.ctx.cp && this.ctx.cp.shortTitle;
    }
  },

  methods: {
    handleInput: function({field, value}) {
      if (field.name == 'cp.sites') {
        const siteCodes = this.dataCtx.cp.cpSites.reduce(
          (acc, {siteName, code}) => {
            acc[siteName] = code;
            return acc;
          },
          {}
        );

        const result = this.dataCtx.cp.cpSites = [];
        for (let site of value || []) {
          if (Object.prototype.hasOwnProperty.call(siteCodes, site)) {
            result.push({siteName: site, code: siteCodes[site]});
          } else {
            result.push({siteName: site});
          }
        }
      }
    },

    toggleSiteCodeInfo: function() {
      this.dataCtx.showSiteCodeInfo = !this.dataCtx.showSiteCodeInfo;
    },

    saveOrUpdate: function() {
      if (!this.$refs.cpForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.cp);
      if (toSave.storeSprEnabled == 'use_system_setting') {
        toSave.storeSprEnabled = null;
      }

      let promise = null;
      if (this.copyOf > 0) {
        promise = cpSvc.copy(this.copyOf, toSave);
      } else {
        promise = cpSvc.saveOrUpdate(toSave);
      }

      promise.then((savedCp) => routerSvc.goto('CpDetail.Overview', {cpId: savedCp.id}));
    },

    cancel: function() {
      routerSvc.goto('CpsList', {cpId: -1});
    },

    _setupCp: async function() {
      const {addEditFs} = this.ctx;
      if (!addEditFs || !addEditFs.rows || addEditFs.rows.length == 0) {
        const { schema, defaultValues } = await cpSvc.getAddEditFormSchema();
        this.ctx.addEditFs = schema;
        this.ctx.defaultValues = defaultValues;
      }

      let cp;
      if (!this.cpId || +this.cpId < 0) {
        if (this.copyOf > 0) {
          cp = this.ctx.cp = await this._getCp(this.copyOf);
          cp.id = cp.shortTitle = cp.title = cp.code = null;
          cp.cpSites.forEach(cpSite => cpSite.id = null);
        } else {
          cp = this.ctx.cp = {cpSites: [], sites: [], storeSprEnabled: 'use_system_setting'};
          if (Object.keys(this.ctx.defaultValues).length > 0) {
            cp.extensionDetail = {attrsMap: this.ctx.defaultValues};
          }
        }
      } else {
        cp = this.ctx.cp = await this._getCp(this.cpId);
      }

      this.dataCtx.cp = util.clone(cp);
    },

    _getCp: async function(cpId) {
      const cp = await cpSvc.getCpById(cpId);
      cp.sites = cp.cpSites.map(({siteName}) => siteName);
      formUtil.createCustomFieldsMap(cp);
      if (cp.storeSprEnabled != true && cp.storeSprEnabled != false) {
        cp.storeSprEnabled = 'use_system_setting';
      }

      return cp;
    }
  }
}
</script>
