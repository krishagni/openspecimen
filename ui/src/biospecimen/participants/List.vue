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

            <os-list-size
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
      <span>Display details of {{ctx.selectedCpr}}</span>
    </os-screen-panel>
  </os-screen>
</template>

<script>
import { inject, reactive } from 'vue';

import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

export default {
  name: 'ParticipantsList',

  inject: ['ui', 'cp'],

  props: ['filters', 'cprId'],

  setup(props) {
    const ui = inject('ui');

    const cp = inject('cp').value;
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
      routerSvc.ngGoto('cp-view/' + this.ctx.cp.id + '/participants/' + cprId + '/detail/overview', {}, true);
    },

    getParticipantsCount: function() {
      this.$refs.participantsList.loadListSize().then(
        () => {
          this.ctx.listInfo.size = this.$refs.participantsList.size;
        }
      );
    },

    onListLoad: function({widget, filters}) {
      this.showListSize = true;
      this.ctx.selectedCprs.length = 0;

      widget.data.forEach(
        row => {
          row.hidden = row.hidden || {};
          row.hidden.cpId = this.ctx.cp.id;
        }
      );

      routerSvc.goto('ParticipantsList', {cpId: this.ctx.cp.id}, {filters});
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
