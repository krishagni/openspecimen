<template>
  <os-screen>
    <os-screen-panel :width="12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'cps.list'">Collection Protocols</h3>
          </span>

          <template #right>
            <os-list-size
              :list="ctx.listInfo.list"
              :page-size="ctx.listInfo.pageSize"
              :list-size="ctx.listInfo.size"
              @updateListSize="getListItemsCount"
            />
          </template>
        </os-page-head>
        <os-page-body v-if="ctx.inited">
          <os-page-toolbar>
            <template #default>
              <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="createCp"
                v-show-if-allowed="cpResources.createOpts" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-query-list-view
            name="cp-list-view"
            :object-id="-1"
            :query="ctx.query"
            :id-filter="'CollectionProtocol.id'"
            :auto-search-open="true"
            :allow-selection="true"
            :selected="ctx.selectedItem"
            :include-count="includeCount"
            :url="itemUrl"
            :newUiUrl="true"
            :newTab="false"
            :allowStarring="true"
            @selectedRows="onItemSelection"
            @rowClicked="onItemRowClick"
            @rowStarToggled="onItemRowStarToggle"
            @listLoaded="onListLoad"
            ref="list"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>
  </os-screen>
</template>

<script>
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';

import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  name: 'CpsList',

  inject: ['ui'],

  props: ['filters'],

  data() {
    const ui = this.ui;
    const ctx = {
      ui,

      inited: false,

      selectedItems: [],

      query: this.filters,

      listInfo: {
        list: [],
        pageSize: 0,
        size: 0
      }
    };

    return { ctx, cpResources };
  },

  created() {
    let filters = {};
    const {query} = this.ctx;
    if (query) {
      filters = JSON.parse(decodeURIComponent(atob(query)));
    }

    if (Object.keys(filters).length > 0) {
      this.ctx.query = util.uriEncode(filters);
    }

    this.ctx.inited = true;
  },

  watch: {
    '$route.query.filters': function(newValue) {
      this.ctx.query = newValue;
    }
  },

  computed: {
    itemUrl: function() {
      return "'#/cps/' + hidden.cpId + '/detail/overview'";
    }
  },

  methods: {
    reloadList: function() {
      this.$refs.list.reload();
    },

    onItemSelection: function(items) {
      this.ctx.selectedItems = items.map(({rowObject}) => ({...rowObject.hidden, id: +rowObject.hidden.cpId}));
    },

    onItemRowClick: function(event) {
      const {cpId} = event.hidden || {};
      routerSvc.goto('CpDetail.Overview', { cpId }, {filters: this.ctx.query});
    },

    onItemRowStarToggle: function(event) {
      const promise = event.starred ? cpSvc.unstarCp(event.hidden.cpId) : cpSvc.starCp(event.hidden.cpId);
      promise.then(() => event.starred = !event.starred);
    },

    getListItemsCount: function() {
      this.$refs.list.loadListSize().then(
        () => {
          this.ctx.listInfo.size = this.$refs.list.size;
        }
      );
    },

    onListLoad: function({widget, filters}) {
      const numFormatter         = Intl.NumberFormat().format;
      const participantsCountIdx = widget.list.columns.length;
      const specimensCountIdx    = widget.list.columns.length + 1;
      widget.schema.columns[participantsCountIdx].href = ({rowObject: {id}}) => 
        routerSvc.getUrl('ParticipantsList', {cpId: id, cprId: -1});
      widget.schema.columns[specimensCountIdx].href = ({rowObject: {id}}) => 
        routerSvc.getUrl('ParticipantsList', {cpId: id, cprId: -1}, {view: 'specimens_list'});

      this.showListSize = true;
      this.ctx.selectedItems.length = 0;
      this.items = widget.data;
      this.items.forEach(
        row => {
          const hidden = row.hidden = row.hidden || {};
          row.id = hidden.cpId;

          const column = row.column;
          column['a_' + participantsCountIdx] = numFormatter(column['a_' + participantsCountIdx]);
          column['a_' + specimensCountIdx]    = numFormatter(column['a_' + specimensCountIdx]);
          
          if (column['a_' + participantsCountIdx] == -1) {
            column['a_' + participantsCountIdx] = 'N/A';
          }
        }
      );

      routerSvc.replace('CpsList', {}, { filters });
      setTimeout(() => {
        this.ctx.listInfo = {
          list: this.$refs.list.list.rows,
          size: this.$refs.list.size,
          pageSize: this.$refs.list.pageSize
        }
      }, 0);
    },

    openSearch: function () {
      this.$refs.list.toggleShowFilters();
    },

    createCp: function() {
      routerSvc.goto('CpAddEdit', {cpId: -1});
    },

    cpDictFn: function() {
      const dict = [];
      return async () => {
        if (dict.length > 0) {
          return dict;
        }

        Array.prototype.push.apply(dict, await cpSvc.getDict());
        return dict;
      }
    }
  }
}
</script>
