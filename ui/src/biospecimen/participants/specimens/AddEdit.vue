<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span>
        <h3>
          <span v-if="!dataCtx.specimen.id" v-t="'specimens.add_specimen'">Add Specimen</span>
          <span v-else v-t="{path: 'common.update', args: {name: specimenLabel}}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="spmnForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="$t(!dataCtx.specimen.id ? 'common.buttons.add' : 'common.buttons.update')"
            @click="saveOrUpdate()" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>

import cpSvc        from '@/biospecimen/services/CollectionProtocol.js';
import specimenSvc  from '@/biospecimen/services/Specimen.js';

import formUtil   from '@/common/services/FormUtil.js';
import routerSvc  from '@/common/services/Router.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['cpr', 'visit', 'specimen'],

  inject: ['cpViewCtx'],

  data() {
    const specimen = this.specimen ? util.clone(this.specimen) : {};
    if (!specimen.id) {
      Object.assign(specimen, {lineage: 'New', status: 'Collected'});
    }

    if (specimen.extensionDetail) {
      //
      // attrs map created might be suitable for display mode
      // therefore clean it up. create the map suitable for data entry
      //
      delete specimen.extensionDetail.attrsMap;
    }

    formUtil.createCustomFieldsMap(specimen);

    const cp = this.cpViewCtx.getCp();
    const userRole = this.cpViewCtx.getRole();
    return {
      dataCtx: {
        specimen,

        cp,

        item: {cpr: this.cpr, visit: this.visit, specimen, userRole},

        userRole
      },

      ctx: {
        addEditFs: {rows: []}
      }
    };
  },

  created() {
    const cpCtx = this.cpViewCtx;
    const promises = [ cpCtx.getSpecimenDict(true), cpCtx.getSpecimenAddEditLayout() ];
    Promise.all(promises).then(
      ([fields, layout]) => {
        const formSchema = this.ctx.addEditFs = formUtil.getFormSchema(fields, layout);
        if (!this.specimen.id || this.specimen.id <= 0) {
          formUtil.setDefaultValues(formSchema, this.dataCtx);
        }
      }
    );
  },

  computed: {
    specimenLabel: function() {
      const {label, barcode, specimenClass, type} = this.specimen;
      let result = label || '';
      if (barcode) {
        result += result ? ' (' + barcode + ') ' : barcode;
      }

      if (!result && specimenClass) {
        result = type + ' (' + specimenClass + ')';
      }

      return result || 'Specimen';
    },

    bcrumb: function() {
      const cp = this.dataCtx.cp;
      if (!cp) {
        return [];
      }

      const {cpId, cprId, visitId, eventId} = this.specimen;
      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: cp.shortTitle
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}),
          label: this.cpr.ppid
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId, eventId}),
          label: cpSvc.getEventDescription(this.visit)
        }
      ];
    }
  },

  methods: {
    handleInput: function() {
    },

    saveOrUpdate: function() {
      if (!this.$refs.spmnForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.specimen);
      specimenSvc.saveOrUpdate(toSave).then(saved => this._navToOverview(saved));
    },

    cancel: function() {
      this._navToOverview(this.dataCtx.specimen);
    },

    _navToOverview: function({cpId, cprId, visitId, id}) {
      if (id > 0) {
        routerSvc.goto('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, specimenId: id});
      } else {
        routerSvc.back();
      }
    }
  }
}
</script>
