<template>
  <os-panel class="os-full-height-panel">
    <template #header>
      <span class="title">
        <os-visit-event-desc :event="event" />
        <span>&nbsp;&gt;&gt;&nbsp;</span>

        <os-specimen-description :specimen="ctx.sr" :no-link="true" :detailed="true" v-if="reqId > 0" />
        <os-specimen-description :specimen="ctx.parentSr" :no-link="true" :detailed="true"
          v-else-if="ctx.parentSr && ctx.parentSr.id > 0" />
        <span v-t="'cps.add_req'" v-else>Add Requirement</span>

        <span v-if="ctx.parentSr && ctx.parentSr.id > 0">
          <span>&nbsp;&gt;&gt;&nbsp;</span>
          <span v-if="lineage == 'Derived'" v-t="'cps.create_derivative'">Create Derived Specimen</span>
          <span v-else-if="lineage == 'Aliquot'" v-t="'cps.create_aliquots'">Create Aliquots</span>
        </span>
      </span>
    </template>

    <template #default>
      <os-form ref="reqForm" :schema="ctx.addEditFs" :data="ctx">
        <os-button primary :label="$t('common.buttons.add')" @click="saveReq" v-if="!reqId || reqId < 0" />
        <os-button primary :label="$t('common.buttons.update')" @click="saveReq" v-else />
        <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
      </os-form>
    </template>
  </os-panel>
</template>

<script>
import alertsSvc  from '@/common/services/Alerts.js';
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js'
import util       from '@/common/services/Util.js';

export default {
  props: ['cp', 'event', 'reqId', 'parentReqId', 'lineage'],

  data() {
    return {
      ctx: {
        cp: this.cp,

        barcodingEnabled: false,

        sr: {
          cpShortTitle: this.cp.shortTitle,
          eventId: this.event.id,
          lineage: this.lineage || 'New',
          anatomicSite: 'Not Specified',
          laterality: 'Not Specified',
          pathology: 'Not Specified',
          collectionProcedure: 'Not Specified',
          collectionContainer: 'Not Specified',
          storageType: this.lineage == 'Aliquot' ? 'Manual' : 'Virtual'
        },

        addEditFs: {rows: []},

        mandatoryAliquotQty: true
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

    this.ctx.barcodingEnabled = this.cp.barcodingEnabled;
    if (!this.ctx.barcodingEnabled) {
      settingSvc.getSetting('biospecimen', 'enable_spmn_barcoding')
        .then(settings => this.ctx.barcodingEnabled = util.isTrue(settings[0].value));
    }

    settingSvc.getSetting('biospecimen', 'mandatory_aliquot_qty').then(
      settings => {
        this.ctx.mandatoryAliquotQty = !util.isFalse(settings[0].value);
      }
    );
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
        const {initialQty} = this.ctx.parentSr;
        if (initialQty > 0) {
          if (!toSave.qtyPerAliquot) {
            toSave.qtyPerAliquot = initialQty / toSave.noOfAliquots;
          } else if (!toSave.noOfAliquots) {
            toSave.noOfAliquots = initialQty / toSave.qtyPerAliquot;
          }
        }

        cpSvc.createAliquots(this.parentReqId, toSave).then(
          () => {
            alertsSvc.success({code: 'cps.aliquots_created', args: this.ctx.parentSr});
            this.cancel(this.ctx.parentSr);
          }
        );
      } else {
        cpSvc.saveOrUpdateSpecimenRequirement(toSave).then(
          savedReq => {
            alertsSvc.success({code: 'cps.req_saved', args: savedReq});
            this.cancel(savedReq);
          }
        );
      }
    },

    cancel: function(req) {
      const query = {eventId: this.event.id, reqId: (req && req.id) || this.reqId};
      routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, query);
    }
  }
}
</script>

<style scoped>
.title {
  display: flex;
  align-items: center;
}

.title :deep(a) {
  color: inherit;
  text-decoration: none;
}
</style>
