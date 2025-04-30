<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span>
        <h3>
          <span v-if="!ctx.cpg.id" v-t="'cpgs.create_cpg'">Create Collection Protocol Group</span>
          <span v-else v-t="{path: 'common.update', args: {name: description}}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="cpgForm" :schema="addEditFs" :data="ctx">
        <div>
          <os-button primary :label="$t(ctx.cpg.id > 0 ? 'common.buttons.update' : 'common.buttons.create')"
            @click="saveOrUpdate()" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>

import cpgSvc     from '@/biospecimen/services/CollectionProtocolGroup.js';
import i18n       from '@/common/services/I18n.js';
import routerSvc  from '@/common/services/Router.js';

import addEditSchema from '@/biospecimen/schemas/cp-groups/addedit.js';

export default {
  props: ['cpgId'],

  data() {
    return {
      ctx: {
        cpg: { },

        name: null
      },

      addEditFs: addEditSchema.layout
    }
  },

  created() {
    this._setupCpg();
  },

  watch: {
    cpgId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupCpg();
      }
    },
  },

  computed: {
    bcrumb: function() {
      return [
        {url: routerSvc.getUrl('CpgsList', {cpgId: -1}), label: i18n.msg('cpgs.list')}
      ];
    },

    description: function() {
      return this.ctx.name;
    }
  },

  methods: {
    saveOrUpdate: function() {
      if (!this.$refs.cpgForm.validate()) {
        return;
      }

      cpgSvc.saveOrUpdate(this.ctx.cpg).then(
        savedCpg => {
          routerSvc.goto('CpgDetail.Overview', {cpgId: savedCpg.id});
        }
      );
    },

    cancel: function() {
      if (this.cpgId > 0) {
        routerSvc.goto('CpgDetail.Overview', {cpgId: this.cpgId});
      } else {
        routerSvc.goto('CpgsList', {cpgId: -1});
      }
    },

    _setupCpg: async function() {
      if (!this.cpgId || +this.cpgId < 0) {
        this.ctx.cpg = {};
      } else {
        this.ctx.cpg = await this._getCpg(this.cpgId);
        this.ctx.name = this.ctx.cpg.name;
      }
    },

    _getCpg: async function(cpgId) {
      return cpgSvc.getGroupById(cpgId);
    }
  }
}
</script>
