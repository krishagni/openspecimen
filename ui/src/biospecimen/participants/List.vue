<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <template #breadcrumb v-if="!ctx.detailView">
            <os-breadcrumb :items="ctx.bcrumb" />
          </template>

          <span>
            <h3 v-t="'participants.list'">Participants</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.listInfo.list"
              :page-size="ctx.listInfo.pageSize"
              :list-size="ctx.listInfo.size"
              @updateListSize="getParticipantsCount"
            />
          </template>
        </os-page-head>
        <os-page-body v-if="ctx.inited">
          <os-page-toolbar v-if="!ctx.detailView">
            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-query-list-view
            name="participant-list-view"
            :object-id="ctx.cp.id"
            :query="ctx.query"
            :auto-search-open="true"
            :allow-selection="true"
            :selected="ctx.selectedCpr"
            :include-count="includeCount"
            url="'#/cp-view/' + hidden.cpId + '/participants/' + hidden.cprId + '/detail/overview'"
            :newTab="false"
            @selectedRows="onParticipantSelection"
            @rowClicked="onParticipantRowClick"
            @listLoaded="onListLoad"
            ref="participantsList"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.cprId > 0 && ctx.selectedCpr">
      <router-view :cprId="ctx.selectedCpr.id" :key="'cpr_view_' + $route.params.cprId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>
import { inject, reactive } from 'vue';

import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

export default {
  name: 'ParticipantsList',

  inject: ['ui'],

  props: ['filters', 'cprId'],

  async setup(props) {
    const ui = inject('ui');

    const cpViewCtx = inject('cpViewCtx').value;
    const cp = await cpViewCtx.getCp();

    let ctx = reactive({
      ui: ui,

      cp: cp,

      inited: true,

      detailView: false,

      selectedCprs: [],

      query: props.filters,

      listInfo: {
        list: [],
        pageSize: 0,
        size: 0
      },

      bcrumb: [ 
        {url: routerSvc.ngUrl('cps', {}), label: i18n.msg('participants.collection_protocols')},
        {url: routerSvc.ngUrl('cp-view/' + cp.id + '/list-view', {}), label: cp.shortTitle}
      ]
    });

    return {
      ctx
    };
  },

  created() {
  },

  watch: {
    'cprId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.cprs.find(rowObject => rowObject.id == this.cprId);
        if (!selectedRow) {
          selectedRow = {id: this.cprId};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    }
  },

  methods: {
    reloadList: function() {
      this.$refs.participantsList.reload();
    },

    onParticipantSelection: function(participants) {
      this.ctx.selectedCprs = participants.map(({rowObject}) => ({id: +rowObject.hidden.cprId}));
    },

    onParticipantRowClick: function(event) {
      const cprId = +event.hidden.cprId;
      routerSvc.goto('ParticipantsListItemDetail.Overview', {cprId: cprId}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedCpr = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.participantsList.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.participantsList.switchToTableView();
      routerSvc.goto('ParticipantsList', {cprId: -1}, {filters: this.filters});
      if (reload) {
        this.reloadList();
      }
    },

    getParticipantsCount: function() {
      this.$refs.participantsList.loadListSize().then(
        () => {
          this.ctx.listInfo.size = this.$refs.participantsList.size;
        }
      );
    },

    onListLoad: function({widget, filters}) {
      const cp = this.ctx.cp;

      this.showListSize = true;
      this.ctx.selectedCprs.length = 0;
      this.cprs = widget.data;
      this.cprs.forEach(
        row => {
          row.hidden = row.hidden || {};
          row.hidden.cpId = cp.id;
          row.id = row.hidden.cprId;
        }
      );

      if (this.cprId <= 0) {
        routerSvc.goto('ParticipantsList', {cpId: cp.id, cprId: -1}, { filters });
      } else {
        let selectedRow = this.cprs.find(row => row.id == this.cprId);
        if (!selectedRow) {
          selectedRow = {id: this.cprId};
        }

        this.showDetails(selectedRow);
      }

      setTimeout(() => {
        this.ctx.listInfo = {
          list: this.$refs.participantsList.list.rows,
          size: this.$refs.participantsList.size,
          pageSize: this.$refs.participantsList.pageSize
        }
      }, 0);
    },

    openSearch: function () {
      this.$refs.participantsList.toggleShowFilters();
    }
  }
}
</script>
