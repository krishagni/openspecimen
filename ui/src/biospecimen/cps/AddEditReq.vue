<template>
  <os-grid>
    <os-grid-column :width="12" style="overflow-y: auto;">
      <os-form ref="reqForm" :schema="ctx.addEditFs" :data="ctx">
        <os-button primary :label="$t('common.buttons.add')" @click="saveReq" v-if="!reqId || reqId < 0" />
        <os-button primary :label="$t('common.buttons.update')" @click="saveReq" v-else />
        <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
      </os-form>
    </os-grid-column>
  </os-grid>
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

export default {
  props: ['cp', 'eventId', 'reqId', 'parentReqId', 'lineage'],

  data() {
    return {
      ctx: {
        sr: {
          cpShortTitle: this.cp.shortTitle,
          eventId: this.eventId,
          lineage: this.lineage || 'New',
          anatomicSite: 'Not Specified',
          laterality: 'Not Specified',
          pathology: 'Not Specified',
          collectionProcedure: 'Not Specified',
          collectionContainer: 'Not Specified',
          storageType: this.lineage == 'Aliquot' ? 'Manual' : 'Virtual'
        },

        addEditFs: {rows: []}
      }
    }
  },

  created() {
    this.ctx.addEditFs = cpSvc.getReqAddEditFormSchema();
    if (+this.reqId > 0) {
      cpSvc.getSpecimenRequirement(this.reqId).then(
        sr => {
          this.ctx.sr = sr;
          if (sr.defaultCustomFieldValues) {
            sr.defaultCustomFieldValuesJson = JSON.stringify(sr.defaultCustomFieldValues, null, 2);
          }
        }
      );
    } else if (+this.parentReqId > 0) {
      cpSvc.getSpecimenRequirement(this.parentReqId).then(
        parentSr => {
          this.ctx.parentSr = parentSr;

          const toCopy = ['anatomicSite', 'laterality', 'pathology'];
          for (let attr of toCopy) {
            this.ctx.sr[attr] = parentSr[attr];
          }
        }
      );
    }
  },

  methods: {
    saveReq: function() {
      if (!this.$refs.reqForm.validate()) {
        return;
      }

      const toSave = util.clone(this.ctx.sr);
      toSave.children = null;
      if (toSave.defaultCustomFieldValuesJson) {
        try {
          toSave.defaultCustomFieldValues = JSON.parse(toSave.defaultCustomFieldValuesJson);
          delete toSave.defaultCustomFieldValuesJson;
        } catch (e) {
          alertsSvc.error({code: 'cps.invalid_req_custom_fields_json', args: e});
          return;
        }
      }

      if (this.lineage == 'Derived') {
        toSave.quantity = toSave.initialQty;
        cpSvc.createDerivedRequirement(this.parentReqId, toSave).then(
          savedReq => {
            alertsSvc.success({code: 'cps.derived_req_created', args: savedReq});
            this.cancel(savedReq);
          }
        );
      } else if (this.lineage == 'Aliquot') {
        cpSvc.createAliquots(this.parentReqId, toSave).then(
          () => {
            alertsSvc.success({code: 'cps.aliquots_created', args: this.ctx.parentSr});
            this.cancel(this.ctx.parentSr);
          }
        );
      } else {
        cpSvc.saveOrUpdateSpecimenRequirement(this.ctx.sr).then(
          savedReq => {
            alertsSvc.success({code: 'cps.req_saved', args: savedReq});
            this.cancel(savedReq);
          }
        );
      }
    },

    cancel: function(req) {
      const query = {eventId: this.eventId, reqId: (req && req.id) || this.reqId};
      routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, query);
    }
  }
}
</script>
