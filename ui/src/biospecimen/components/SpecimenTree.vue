
<template>
  <os-panel ref="specimenTree">
    <template #header>
      <span v-t="'specimens.list'">Specimens</span>
    </template>

    <template #actions>
      <os-button primary :left-icon="'plus'" :label="$t('participants.collect')" @click="collectSpecimens"
        v-if="cpr.hasConsented && (visit && visit.id > 0) && (!specimen || specimen.status == 'Collected') && selectedPendingSpecimens.length > 0"
      />

      <os-add-to-cart :specimens="selectedExistingSpecimens"  v-if="selectedExistingSpecimens.length > 0" />

      <os-specimen-actions :cp="cp" :specimens="selectedExistingSpecimens" @reloadSpecimens="reloadSpecimens"
        v-if="selectedExistingSpecimens.length > 0" />

      <os-button :label="$t('specimens.show_pending')" @click="showPending" v-if="haveOldSpecimens && pendingHidden" />

      <os-button :label="$t('specimens.hide_pending')" @click="hidePending" v-if="haveOldSpecimens && !pendingHidden" />
    </template>

    <os-table-form ref="spmnsTable" :tree-layout="true" :read-only="true" selection-mode="checkbox"
      :data="{}" :items="items" :schema="{columns: fields}" @selected-items="onItemsSelection($event)"
      v-if="items.length > 0">
    </os-table-form>

    <os-message :type="info" v-else>
      <span v-t="'specimens.no_specimens'"> </span>
    </os-message>

    <os-button class="scroll-top" left-icon="arrow-circle-up" @click="scrollToTop" v-if="panelFixed" />
  </os-panel>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import formUtil from '@/common/services/FormUtil.js';
import settingSvc from '@/common/services/Setting.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import util from '@/common/services/Util.js';

export default {
  props: ['cp', 'cpr', 'visit', 'specimen', 'specimens', 'refDate'],

  emits: ['reload'],

  data() {
    return {
      treeCfg: {},

      panelFixed: false,

      hidePendingSpmnsInterval: 0,

      haveOldSpecimens: false,

      pendingHidden: true,

      ctx: {
        selectedSpecimens: []
      }
    }
  },

  created() {
    const treeCfgQ = cpSvc.getSpecimenTreeCfg((this.cp && this.cp.id) || -1);
    const dictQ    = specimenSvc.getDict((this.cp && this.cp.id) || -1);
    Promise.all([dictQ, treeCfgQ]).then(
      ([dict, treeCfg]) => {
        this.treeCfg = util.clone(treeCfg || {});
        this.treeCfg.fields = formUtil.sdeFieldsToDict(this.treeCfg.fields || [], dict);
        for (let field of this.treeCfg.fields) {
          if (field.type == 'specimen-description') {
            field.showStatus = true;
          }

          if (field.name.indexOf('calc') == 0 && field.displayExpr) {
            field.value = (row) => exprUtil.eval({...row, fns: util.fns()}, field.displayExpr)
          }

          field.showCellWhen = field.showWhen;
          field.showValue = true;
          delete field.showWhen;
        }
      }
    );

    settingSvc.getSetting('biospecimen', 'pending_spmns_disp_interval')
      .then(([setting]) => this.hidePendingSpmnsInterval = setting.value ? +setting.value : 0);
    window.addEventListener('wheel', this.onScroll);
  },

  unmounted() {
    window.removeEventListener('wheel', this.onScroll);
  },
   
  computed: {
    startDate: function() {
      if (this.refDate) {
        return new Date(this.refDate);
      }

      return Date.now();
    },

    oldTree: function() {
      if (this.hidePendingSpmnsInterval <= 0) {
        return false;
      }

      return Math.floor((Date.now() - this.startDate) / (1000 * 60 * 60 * 24)) > this.hidePendingSpmnsInterval;
    },

    items: function() {
      return this._flattenSpecimens(this.specimens || [], 0);
    },

    selectedSpecimens: function() {
      return this.ctx.selectedSpecimens.map(({specimen}) => specimen);
    },

    selectedPendingSpecimens: function() {
      return this.ctx.selectedSpecimens.filter(({specimen: {status}}) => !status || status == 'Pending');
    },

    selectedExistingSpecimens: function() {
      return this.selectedSpecimens.filter(({id}) => id > 0);
    },

    fields: function() {
      if (this.treeCfg.fields && this.treeCfg.fields.length > 0) {
        return this.treeCfg.fields;
      }

      return [
        {
          type: 'specimen-description',
          name: 'specimen',
          labelCode: 'specimens.description',
          showStatus: true,
          detailed: false,
        },
        {
          type: 'text',
          name: 'specimen.label',
          labelCode: 'specimens.label',
        },
        {
          type: 'storage-position',
          name: 'specimen.storageLocation',
          labelCode: 'specimens.location',
        }
      ];
    }
  },

  methods: {
    onItemsSelection: function(items) {
      const prevSelectedItems = this.prevSelectedItems || [];

      // new items = items not present in previous selection
      const newItems = items.filter(item => prevSelectedItems.indexOf(item) == -1);

      // removed items = items present in previous selection but not in the current selection
      const rmItems  = prevSelectedItems.filter(item => items.indexOf(item) == -1);

      const added = [];
      for (let item of newItems) {
        if (!item.expanded) {
          // when an item is selected in collapsed mode, select all its descendants as well
          Array.prototype.push.apply(added, this._getDescendants(this.items, item));
        }
      }

      const removed = [];
      for (let item of rmItems) {
        if (!item.expanded) {
          // when an item is de-selected in collapsed mode, de-select all its descendants as well
          Array.prototype.push.apply(removed, this._getDescendants(this.items, item));
        }
      }

      items = items.concat(added.filter(item => items.indexOf(item) == -1)) // add new items
        .filter(item => removed.indexOf(item) == -1); // filter the removed items

      this.ctx.selectedSpecimens = items;
      this.prevSelectedItems = items;
      this.$refs.spmnsTable.setSelection(items);
    },

    collectSpecimens: async function() {
      const specimenIds = this.selectedPendingSpecimens.map(({specimen: {id}}) => id).filter(id => id > 0);
      const reqIds = this.selectedPendingSpecimens
        .filter(({specimen: {id, reqId}}) => !id && reqId > 0)
        .map(({specimen: {reqId}}) => reqId);

      if (specimenIds.length == 0 && reqIds.length == 0) {
        alert('Nothing to collect');
        return;
      }

      const reqOrSpmnIds = {specimens: specimenIds, requirements: reqIds};
      const wfInstanceSvc = this.$osSvc.tmWfInstanceSvc;
      if (wfInstanceSvc) {
        let wfName;
        if (!this.visit.id || !this.visit.status || this.visit.status == 'Pending') {
          wfName = await this._getCollectVisitsWf();
        } else {
          wfName = await this._getCollectPendingSpmnsWf();
        }

        let inputItem = {
          cpr:   {id: this.cpr.id, cpId: this.cp.id, cpShortTitle: this.cp.shortTitle},
          visit: {id: this.visit.id, cpId: this.cp.id, cpShortTitle: this.cp.shortTitle}
        };

        if (this.visit.eventId) {
          inputItem.cpe = {id: this.visit.eventId, cpId: this.cp.id, cpShortTitle: this.cp.shortTitle};
        }

        if (this.specimen) {
          inputItem.specimen = {id: this.specimen.id};
        }

        const opts = {params: this._getBatchParams(this.$t('participants.collect_specimens'), reqOrSpmnIds)};
        if (this.specimen) {
          opts.inputType = 'specimen';
        }

        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        alert('Workflow module not installed!');
      }
    },

    reloadSpecimens: function() {
      this.$emit('reload');
      this.ctx.selectedSpecimens = [];
      this.$refs.spmnsTable.setSelection([]);
    },

    onScroll: function() {
      const treeEl = this.$refs.specimenTree.$el;
      if (treeEl.getBoundingClientRect().top <= 167) {
        const width = treeEl.offsetWidth + 'px';
        const height = 'calc(100% - 187px)';
        Object.assign(treeEl.style, {position: 'fixed', top: '167px', width, height, overflow: 'scroll'});
        this.panelFixed = true;
      }
    },

    scrollToTop: function() {
      const treeEl = this.$refs.specimenTree.$el;
      Object.assign(treeEl.style, {position: '', top: '', width: 'initial', height: '', overflow: 'visible'});
      this.panelFixed = false;
      setTimeout(() => window.scrollTo(0, 0));
    },

    showPending: function() {
      this.pendingHidden = false;
    },

    hidePending: function() {
      this.pendingHidden = true;
    },

    _flattenSpecimens: function(specimens, depth, parentUid) {
      let idx = 0;
      let result = [];
      for (let specimen of specimens) {
        formUtil.createCustomFieldsMap(specimen, true);
        if (this.oldTree && (!specimen.status || specimen.status == 'Pending')) {
          this.haveOldSpecimens = true;
          if (this.pendingHidden) {
            continue;
          }
        }

        const uid = parentUid !== undefined && parentUid !== null ? parentUid + '_' + idx : idx;
        const item = {cpr: this.cpr, visit: this.visit, specimen, depth, expanded: true, show: true, uid, parentUid};
        result.push(item);
        ++idx;
      
        const children = this._flattenSpecimens(specimen.children || [], depth + 1, uid);
        Array.prototype.push.apply(result, children);
        item.hasChildren = (children || []).length > 0;
      }

      return result;
    },

    _getDescendants: function(items, item) {
      const result = [];
      const workingList = [item];
      while (workingList.length > 0) {
        let parentItem = workingList.shift();
        result.push(parentItem);

        for (let child of parentItem.specimen.children || []) {
          let childItem = items.find(({specimen}) => specimen == child);
          workingList.push(childItem);
        }
      }

      return result;
    },

    _getCollectPendingSpmnsWf() {
      return cpSvc.getWorkflowProperty(this.visit.cpId, 'common', 'collectPendingSpecimensWf')
        .then(wfName => wfName || 'sys-collect-pending-specimens');
    },

    _getCollectVisitsWf() {
      return cpSvc.getWorkflowProperty(this.visit.cpId, 'common', 'collectVisitsWf')
        .then(wfName => wfName || 'sys-collect-visits');
    },

    _getBatchParams(title, reqOrSpmnIds) {
      let returnOnExit = 'current_view';
      if (!this.visit.id) {
        returnOnExit = JSON.stringify({
          name: 'ParticipantsListItemDetail.Overview',
          params: {cpId: this.cp.id, cprId: this.cpr.id}
        });
      }

      let breadCrumb2 = null;
      if (!this.cp.specimenCentric) {
        breadCrumb2 = JSON.stringify({
          label: this.cpr.ppid,
          route: {name: 'ParticipantsListItemDetail.Overview', params: {cpId: this.cp.id, cprId: this.cpr.id}}
        });
      }

      return {
        returnOnExit,
        cpId: this.cp.id,
        'breadcrumb-1': JSON.stringify({
          label: this.cp.shortTitle,
          route: {name: 'ParticipantsList', params: {cpId: this.cp.id, cprId: -1}}
        }),
        'breadcrumb-2': breadCrumb2,
        batchTitle: title,
        showOptions: false,
        reqOrSpmnIds: JSON.stringify(reqOrSpmnIds || {})
      };
    }
  }
}
</script>

<style scoped>
.scroll-top {
  position: absolute;
  right: 1rem;
  bottom: 1rem;
  padding: 0.25rem 0.5rem;
  font-size: 1.5rem!important;
  height: 2.625rem!important;
  width: 2.625rem!important;
  z-index: 100;
  background: #fff!important;
}
</style>
