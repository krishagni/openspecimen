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
  props: ['cpId'],

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

        cp: { }
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
    }
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
      const toSave = util.clone(this.dataCtx.cp);
      if (toSave.storeSprEnabled == 'use_system_setting') {
        toSave.storeSprEnabled = null;
      }

      cpSvc.saveOrUpdate(toSave).then(() => routerSvc.goto('CpsList', {cpId: -1}));
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

      if (!this.cpId || +this.cpId < 0) {
        const cp = this.ctx.cp = {cpSites: [], sites: [], storeSprEnabled: 'use_system_setting'};
        if (Object.keys(this.ctx.defaultValues).length > 0) {
          cp.extensionDetail = {attrsMap: this.ctx.defaultValues};
        }

        this.dataCtx.cp = cp;
        return;
      }

      const cp = this.ctx.cp = await cpSvc.getCpById(this.cpId);
      cp.sites = cp.cpSites.map(({siteName}) => siteName);
      formUtil.createCustomFieldsMap(cp);
      if (cp.storeSprEnabled != true && cp.storeSprEnabled != false) {
        cp.storeSprEnabled = 'use_system_setting';
      }

      this.dataCtx.cp = util.clone(cp);
    }
  }
}
</script>
