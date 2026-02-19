<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span class="os-title">
        <h3>
          <span v-if="!dataCtx.visit.id" v-t="'visits.add_visit'">Add Visit</span>
          <span v-else-if="!ctx.header.leftTitle" v-t="{path: 'common.update', args: {name: description}}"></span>
          <span class="custom-title" v-else>
            <span v-t="{path: 'common.buttons.update'}">Update</span>
            <os-dynamic-template v-if="ctx.header.leftTitle" :template="ctx.header.leftTitle"
              :cp="dataCtx.cp" :cpr="cpr" :visit="visit" :hasPhiAccess="hasPhiAccess" />
          </span>
        </h3>
      </span>

      <template #right v-if="ctx.header.rightTitle">
        <h3>
          <os-dynamic-template :template="ctx.header.rightTitle" :cp="dataCtx.cp" :cpr="cpr"
            :visit="visit" :hasPhiAccess="hasPhiAccess" />
        </h3>
      </template>
    </os-page-head>

    <os-page-body>
      <os-form ref="visitForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="$t(!dataCtx.visit.id ? 'common.buttons.add' : 'common.buttons.update')"
            @click="saveOrUpdate()" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import visitSvc   from '@/biospecimen/services/Visit.js';

import authSvc    from '@/common/services/Authorization.js';
import formUtil   from '@/common/services/FormUtil.js';
import routerSvc  from '@/common/services/Router.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['cpr', 'visit'],

  inject: ['cpViewCtx'],

  data() {
    const visit = this.visit ? util.clone(this.visit) : {};
    if (!visit.id) {
      Object.assign(visit, {visitDate: new Date(), cpId: this.cpr.cpId, cprId: this.cpr.id, status: 'Complete'});
    }

    if (visit.extensionDetail) {
      //
      // attrs map created might be suitable for display mode
      // therefore clean it up. create the map suitable for data entry
      //
      delete visit.extensionDetail.attrsMap;
    }

    formUtil.createCustomFieldsMap(visit);

    const cp = this.cpViewCtx.getCp();
    return {
      dataCtx: {
        visit,

        objName: 'visit',

        objCustomFields: 'visit.extensionDetail.attrsMap',

        cp,

        userRole: this.cpViewCtx.getRole()
      },

      ctx: {
        addEditFs: {rows: []},

        header: {}
      }
    };
  },

  created() {
    const cpCtx = this.cpViewCtx;
    const promises = [ cpCtx.getVisitDict(true), cpCtx.getVisitAddEditLayout() ];
    Promise.all(promises).then(
      ([fields, layout]) => {
        const formSchema = this.ctx.addEditFs = formUtil.getFormSchema(fields, layout)
        if (!this.visit.id || this.visit.id <= 0) {
          formUtil.setDefaultValues(formSchema, this.dataCtx);
        }
      }
    );

    cpSvc.getWorkflowProperty(this.dataCtx.cp.id, 'common', 'visitHeader').then(
      header => {
        if (header) {
          this.ctx.header = header;
        }
      }
    );
  },

  computed: {
    description: function() {
      return cpSvc.getEventDescription(this.dataCtx.visit);
    },

    bcrumb: function() {
      const cp = this.dataCtx.cp;
      if (!cp) {
        return [];
      }

      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId: cp.id, cprId: -1}),
          label: cp.shortTitle
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId: cp.id, cprId: this.cpr.id}),
          label: this.cpr.ppid
        }
      ];
    },

    hasPhiAccess: function() {
      return authSvc.isAllowed({resources: ['ParticipantPhi'], cp: this.dataCtx.cp.shortTitle, operations: ['Read']});
    }
  },

  methods: {
    handleInput: function() {
    },

    saveOrUpdate: function() {
      if (!this.$refs.visitForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.visit);
      visitSvc.saveOrUpdate(toSave).then(saved => this._navToOverview(saved));
    },

    cancel: function() {
      this._navToOverview(this.dataCtx.visit);
    },

    _navToOverview: function({cpId, cprId, id}) {
      if (id > 0) {
        routerSvc.goto('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId: id});
      } else {
        routerSvc.back();
      }
    }
  }
}
</script>
