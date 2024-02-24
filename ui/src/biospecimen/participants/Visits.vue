<template>
  <div class="os-visit-card" v-for="(visit, index) of visits" :key="'v_' + index" @click="showVisitDetail(visit)">
    <div class="summary">
      <div :class="dateClass(visit)"
        v-os-tooltip.bottom="$t(!visit.status || visit.status == 'Pending' ?
          'participants.anticipated_visit_date' : 'participants.visit_date')">
        <span class="month-year">
          {{$filters.formatDate(visit.visitDate || visit.anticipatedVisitDate, 'MMM yyyy', '?')}}
        </span>
        <span class="day">{{$filters.formatDate(visit.visitDate || visit.anticipatedVisitDate, 'dd', '?')}}</span>
      </div>
      <div class="content">
        <div class="headline">
          <h4 class="title">
            <span> <os-visit-event-desc :visit="visit" /> </span>
            <span v-show="!!visit.name"> | {{visit.name}}</span>
          </h4>
          <div class="action-buttons" @click="$event.stopPropagation()">
            <os-button left-icon="eye"  size="small" v-os-tooltip.bottom="$t('participants.view_visit')"
              @click="gotoVisit(visit)" />
            <os-button left-icon="redo" size="small" v-os-tooltip.bottom="$t('participants.new_visit')"
              @click="repeatVisit(visit)" v-if="visit.status && visit.status != 'Pending'" />
            <os-button left-icon="flask" size="small"
              v-os-tooltip.bottom="$t('participants.collect_pending_specimens')" @click="collectPending(visit)"
              v-if="!visit.status || visit.status == 'Pending' || visit.status == 'Complete' "/>
            <os-button left-icon="plus" size="small"
              v-os-tooltip.bottom="$t('participants.collect_unplanned_specimens')" @click="addSpecimen(visit)"
              v-if="visit.status == 'Complete'" />
            <os-button left-icon="print" size="small" v-os-tooltip.bottom="$t('participants.print_specimen_labels')"
              @click="printLabels(visit)" v-if="visit.status == 'Complete'" />
          </div>
        </div>
        <div class="stats">
          <div class="collection">
            <i v-show="visit.status == 'Complete'" v-t="'participants.collection'">Collection</i>
            <i v-show="!visit.status || visit.status == 'Pending'" v-t="'participants.anticipated'">Anticipated</i>
            <i v-show="visit.status == 'Missed Collection'" v-t="'participants.missed_collection'">Missed Collection</i>
            <os-visit-specimen-collection-stats :visit="visit" />
          </div>
          <div class="utilisation" v-show="visit.status == 'Complete'">
            <i v-t="'participants.utilisation'">Utilisation</i>
            <os-visit-specimen-utilisation-stats :visit="visit" />
          </div>
        </div>
      </div>
    </div>
    <div :class="detailClass(visit)">
      <os-overview :schema="dict" :object="{cp, cpr, visit}" v-if="dict && dict.length > 0" />
    </div>
  </div>
</template>

<script>

import routerSvc   from '@/common/services/Router.js';
import util        from '@/common/services/Util.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import wfSvc       from '@/biospecimen/services/Workflow.js';

export default {
  props: ['cp', 'cpr', 'visits', 'dict'],

  data() {
    return {
      ctx: {}
    };
  },

  methods: {
    dateClass: function(visit) {
      const result = ['date'];
      switch (visit.status) {
        case 'Complete':
          result.push('completed');
          break;

        case 'Missed Collection':
        case 'Not Collected':
          result.push('missed-collection');
          break;

        default:
          result.push('pending');
          break;
      }

      return result;
    },

    detailClass: function(visit) {
      const result = ['details'];
      if (visit == this.ctx.visit) {
        result.push('show');
      }

      return result;
    },

    showVisitDetail: function(visit) {
      this.ctx.visit = visit;
    },

    gotoVisit: function(visit) {
      const route = routerSvc.getCurrentRoute();
      const {cpId, cprId, id, eventId} = visit;
      if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
        routerSvc.goto('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId: id || -1}, {eventId});
      } else {
        routerSvc.goto('VisitDetail.Overview', {cpId, cprId, visitId: id || -1}, {eventId});
      }
    },  

    repeatVisit: async function(visit) {
      wfSvc.collectVisitSpecimens(visit, true);
    },

    collectPending: async function(visit) {
      if (!visit.status || visit.status == 'Pending') {
        wfSvc.collectVisitSpecimens(visit);
      } else {
        wfSvc.collectPending(visit);
      }
    },

    addSpecimen: async function(visit) {
      wfSvc.addSpecimen(this.cp, visit);
    },

    printLabels: function(visit) {
      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [visit.cpShortTitle, visit.ppid, visit.name, ts].join('_') + '.csv';
      specimenSvc.printLabels({visitId: visit.id}, outputFilename);
    }
  }
}
</script>

<style scoped>
.os-visit-card {
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 2px 6px 2px rgba(60,64,67,.15);
  padding: 1rem;
  border-radius: 0.5rem;
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  cursor: pointer;
}

.os-visit-card .summary {
  display: flex;
}

.os-visit-card .summary .date {
  flex: 0 0 5rem;
  height: 5rem;
  margin-right: 0.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
  border-radius: 0.5rem;
}

.os-visit-card .summary .date.completed {
  background: #5cb85c;
}

.os-visit-card .summary .date.missed-collection {
  background: #888;
}

.os-visit-card .summary .date.pending {
  background: #f0ad4e;
}

.os-visit-card .summary .date .month-year {
  flex: 0 0 1rem;
  margin-top: 0.5rem;
}

.os-visit-card .summary .date .day {
  font-size: 2rem;
}

.os-visit-card .content {
  flex: 1;
}

.os-visit-card .headline {
  display: flex;
}

.os-visit-card .headline .title {
  color: #666;
  margin-top: 0rem;
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
  flex: 1;
}

.os-visit-card .headline .action-buttons {
  display: flex;
}

.os-visit-card .headline .action-buttons :deep(.btn) {
  margin-right: 0.5rem;
}

.os-visit-card .content .stats {
  display: flex;
}

.os-visit-card .content .stats .collection {
  flex: 1;
  margin-right: 0.5rem;
  max-width: calc(50% - 0.5rem);
}
.os-visit-card .content .stats .utilisation {
  flex: 1;
  margin-left: 0.5rem;
  max-width: calc(50% - 0.5rem);
}

.os-visit-card .details {
  overflow: hidden;
  transition: all 1s ease-in-out;
  border-top: 0;
  padding: 0;
  margin-top: 0;
  max-height: 0;
}

.os-visit-card .details.show {
  border-top: 1px solid #ddd;
  padding: 1rem 0rem;
  margin-top: 1rem;
  max-height: 1000rem;
}
</style>
