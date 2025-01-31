<template>
  <os-panel class="os-full-height-panel">
    <template #header>
      <span v-t="'cps.update_event'" v-if="event.id > 0">Update Event</span>
      <span v-t="'cps.add_cpe'" v-else>Add Event</span>
    </template>

    <template #default>
      <os-form ref="eventForm" :schema="ctx.addEditFs" :data="ctx">
        <os-button primary :label="$t('common.buttons.update')" @click="saveEvent" v-if="event.id > 0" />
        <os-button primary :label="$t('common.buttons.add')" @click="saveEvent" v-else />
        <os-button text :label="$t('common.buttons.cancel')" @click="cancel(event.id > 0 ? event.id : copyOf)" />
      </os-form>
    </template>
  </os-panel>
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

export default {
  props: ['cp', 'event', 'copyOf'],

  data() {
    return {
      ctx: {
        cpe: {
          cpShortTitle: this.cp.shortTitle,
          clinicalDiagnosis: 'Not Specified',
          clinicalStatus: 'Not Specified'
        },

        addEditFs: {rows: []},

        getIntervalUnits: this._getIntervalUnits
      }
    }
  },

  created() {
    this.ctx.addEditFs = cpSvc.getEventAddEditFormSchema();
    if (this.event.id > 0) {
      this.ctx.cpe = util.clone(this.event);
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
          this.$emit('cpe-saved', savedCpe);
          this.cancel(savedCpe.id);
        }
      );
    },

    cancel: function(eventId) {
      eventId = (eventId > 0 ? eventId : null);
      routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId});
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
