<template>
  <os-grid>
    <os-grid-column :width="12">
      <os-form ref="eventForm" :schema="ctx.addEditFs" :data="ctx">
        <os-button primary :label="$t('common.buttons.add')" @click="saveEvent" v-if="!eventId || eventId < 0" />
        <os-button primary :label="$t('common.buttons.update')" @click="saveEvent" v-else />
        <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
      </os-form>
    </os-grid-column>
  </os-grid>
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cp', 'eventId', 'copyOf'],

  data() {
    return {
      ctx: {
        cpe: {cpShortTitle: this.cp.shortTitle},

        addEditFs: {rows: []},

        getIntervalUnits: this._getIntervalUnits
      }
    }
  },

  created() {
    this.ctx.addEditFs = cpSvc.getEventAddEditFormSchema();
    if (+this.eventId > 0) {
      cpSvc.getCpe(this.eventId).then(cpe => this.ctx.cpe = cpe);
    } else if (+this.copyOf > 0) {
      cpSvc.getCpe(this.copyOf).then(
        cpe => {
          this.ctx.cpe = cpe;
          cpe.id = cpe.code = null;
        }
      );
    }
  },

  methods: {
    saveEvent: function() {
      if (!this.$refs.eventForm.validate()) {
        return;
      }

      cpSvc.saveOrUpdateCpe(this.ctx.cpe, this.copyOf).then(
        savedCpe => {
          alertsSvc.success({code: 'cps.event_saved', args: savedCpe});
          this.cancel();
        }
      );
    },

    cancel: function() {
      routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id});
    },

    _getIntervalUnits: async function() {
      return [
        { value: 'DAYS',   label: this.$t('cps.days') },
        { value: 'WEEKS',  label: this.$t('cps.weeks') },
        { value: 'MONTHS', label: this.$t('cps.months') },
        { value: 'YEARS',  label: this.$t('cps.years') }
      ];
    }
  }
}
</script>
