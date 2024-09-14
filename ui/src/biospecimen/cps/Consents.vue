<template>
  <div class="os-consents">
    <os-page-toolbar v-if="!cp.consentsSource && ctx.tiers && ctx.tiers.length > 0">
      <template #default>
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="showAddEditConsentTierDialog({})" />
      </template>
    </os-page-toolbar>

    <div v-if="cp.consentsSource">
      <os-card>
        <template #body>
          <div>
            <span>
              <span v-t="'cps.consents_sourced_from'"></span>
              <a :href="consentsSourceUrl" target="_blank">{{cp.consentsSource.shortTitle}}</a>
            </span>

            <os-divider />

            <div>
              <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="showSelectCpDialog" />

              <os-button left-icon="times" :label="$t('common.buttons.remove')" @click="unsetConsentsCp"
                style="margin-left: 1rem;" />
            </div>
          </div>
        </template>
      </os-card>

      <os-message type="info" v-if="!ctx.tiers || ctx.tiers.length == 0">
        <span v-t="'cps.no_consents_in_source_cp'">No consents to display</span>
      </os-message>
    </div>

    <os-grid v-if="ctx.tiers && ctx.tiers.length > 0">
      <os-grid-column :width="12">
        <os-card v-for="tier of ctx.tiers" :key="tier.id">
          <template #body>
            <div class="os-consent-tier">
              <div class="statement">
                <span>{{tier.statement}} ({{tier.statementCode}})</span>
              </div>
              <div class="actions" v-if="!cp.consentsSource">
                <os-button-group>
                  <os-button size="small" left-icon="edit"  @click="showAddEditConsentTierDialog(tier)" />
                  <os-button size="small" left-icon="trash" @click="deleteTier(tier)" />
                </os-button-group>
              </div>
            </div>
          </template>
        </os-card>
      </os-grid-column>
    </os-grid>

    <div v-else>
      <os-card v-if="!cp.consentsSource">
        <template #body>
          <div>
            <span v-t="'cps.waive_consents_q'" v-if="!cp.consentsWaived">Do you want to waive the consents?</span>
            <span v-t="'cps.collect_consents_q'" v-else>Consents are waived. Do you want to start collecting consents?</span>
          </div>

          <os-divider />

          <div>
            <os-button :label="$t('common.buttons.yes')" @click="waiveConsents" v-if="!cp.consentsWaived" />
            <os-button :label="$t('common.buttons.yes')" @click="undoWaiveConsents" v-else />
          </div>
        </template>
      </os-card>

      <div v-if="!cp.consentsWaived && !cp.consentsSource">
        <os-card>
          <template #body>
            <div>
              <span v-t="'cps.source_from_another_cp'">
               Do you want to source consents from another collection protocol?
              </span>
            </div>

            <os-divider />

            <div>
              <os-button :label="$t('common.buttons.yes')" @click="showSelectCpDialog" />
            </div>
          </template>
        </os-card>

        <div>
          <os-button left-icon="plus" :label="$t('cps.add_consent_tier')" @click="showAddEditConsentTierDialog({})" />
        </div>
      </div>
    </div>

    <os-dialog ref="addEditConsentTierDialog">
      <template #header>
        <span v-if="ctx.tier.id > 0" v-t="'cps.edit_consent_tier'">Edit Consent Tier</span>
        <span v-else v-t="'cps.add_consent_tier'">Add Consent Tier</span>
      </template>
      <template #content>
        <os-form ref="addEditConsentTier" :schema="addEditFs" :data="ctx" />
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="hideAddEditConsentTierDialog" />
        <os-button primary :label="$t('common.buttons.add')"    @click="saveOrUpdate" v-if="!ctx.tier.id" />
        <os-button primary :label="$t('common.buttons.update')" @click="saveOrUpdate" v-else />
      </template>
    </os-dialog>

    <os-delete-object ref="deleteConsentTierDialog" :input="ctx.deleteOpts" v-if="ctx.tier" />

    <os-dialog ref="selectCpDialog">
      <template #header>
        <span v-t="'cps.select_cp'">Select Collection Protocol</span>
      </template>
      <template #content>
        <os-form ref="selectCpForm" :schema="consentCpFs" :data="ctx" />
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="hideSelectCpDialog" />
        <os-button primary :label="$t('common.buttons.update')" @click="setConsentsCp" />
      </template>
    </os-dialog>
  </div>
</template>

<script>

import consentSchema from '@/biospecimen/schemas/cps/consent-addedit.js';
import consentCpSchema from '@/biospecimen/schemas/cps/consent-cp.js';

import alertsSvc from '@/common/services/Alerts.js';
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';

export default {
  props: ['cp'],

  data() {
    return {
      ctx: {
        tiers: null,

        tier: null,

        deleteOpts: null,

        cp: null
      },

      addEditFs: consentSchema.layout,

      consentCpFs: consentCpSchema.layout
    }
  },

  created() {
    this._loadConsentTiers();
  },

  computed: {
    consentsSourceUrl: function() {
      return routerSvc.getUrl('CpDetail.Consents', {cpId: this.cp.consentsSource.id});
    }
  },

  methods: {
    showAddEditConsentTierDialog: function(tier) {
      this.ctx.tier = util.clone(tier);
      this.$refs.addEditConsentTierDialog.open();
    },

    hideAddEditConsentTierDialog: function() {
      this.ctx.tier = null;
      this.$refs.addEditConsentTierDialog.close();
    },

    saveOrUpdate: function() {
      if (!this.$refs.addEditConsentTier.validate()) {
        return;
      }

      const {id, statementCode} = this.ctx.tier;
      const toSave = {id, statementCode, cpId: this.cp.id};
      cpSvc.saveOrUpdateConsentTier(toSave).then(
        (saved) => {
          alertsSvc.success({code: 'cps.consent_tier_saved', args: saved});
          this.hideAddEditConsentTierDialog();
          this._loadConsentTiers();
        }
      );
    },

    deleteTier: function(tier) {
      this.ctx.tier = tier;
      tier.cpId = this.cp.id;
      this.ctx.deleteOpts = {
        type: this.$t('cps.consent_tier'),
        title: tier.statementCode,
        dependents: () => cpSvc.getConsentTierDependents(tier),
        forceDelete: false,
          askReason: false,
          deleteObj: () => cpSvc.deleteConsentTier(tier)
      };

      setTimeout(
        async () => {
          const resp = await this.$refs.deleteConsentTierDialog.execute();
          if (resp == 'deleted') {
            this._loadConsentTiers();
          }
        }
      );
    },

    waiveConsents: function() {
      cpSvc.waiveConsents(this.cp.id).then(savedCp => this.$emit('cp-saved', savedCp));
    },

    undoWaiveConsents: function() {
      cpSvc.undoWaiveConsents(this.cp.id).then(savedCp => this.$emit('cp-saved', savedCp));
    },

    showSelectCpDialog: function() {
      this.ctx.cp = null;
      this.$refs.selectCpDialog.open();
    },

    hideSelectCpDialog: function() {
      this.$refs.selectCpDialog.close();
    },

    setConsentsCp: function() {
      if (!this.$refs.selectCpForm.validate()) {
        return;
      }

      this._setConsentsCp(this.cp.id, this.ctx.cp);
    },

    unsetConsentsCp: function() {
      this._setConsentsCp(this.cp.id, {});
    },

    _loadConsentTiers: function() {
      cpSvc.getConsentTiers(this.cp.id).then(tiers => this.ctx.tiers = tiers);
    },

    _setConsentsCp: function(cpId, sourceCp) {
      cpSvc.setConsentsCp(cpId, sourceCp).then(
        savedCp => {
          this.$emit('cp-saved', savedCp);
          this._loadConsentTiers();
          this.hideSelectCpDialog();
        }
      );
    }
  }
}
</script>

<style scoped>
.os-consents :deep(.os-card) {
  margin-bottom: 1rem;
}

.os-consent-tier {
  display: flex;
  align-items: center;
}

.os-consent-tier .statement {
  flex: 1;
}
</style>
