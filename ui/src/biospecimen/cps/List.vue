<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'cps.list'">Collection Protocols</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable(false)"
            />

            <os-list-size v-else
              :list="ctx.listInfo.list"
              :page-size="ctx.listInfo.pageSize"
              :list-size="ctx.listInfo.size"
              @updateListSize="getListItemsCount"
            />
          </template>
        </os-page-head>
        <os-page-body v-if="ctx.inited">
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span>Action buttons</span>
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-query-list-view
            name="cp-list-view"
            :object-id="-1"
            :query="ctx.query"
            :auto-search-open="true"
            :allow-selection="true"
            :selected="ctx.selectedItem"
            :include-count="includeCount"
            :url="itemUrl"
            :newUiUrl="true"
            :newTab="false"
            @selectedRows="onItemSelection"
            @rowClicked="onItemRowClick"
            @listLoaded="onListLoad"
            ref="list"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.cpId > 0">
      <span>CP detail</span>
    </os-screen-panel>
  </os-screen>
</template>

<script>
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

export default {
  name: 'CpsList',

  inject: ['ui'],

  props: ['filters', 'cpId'],

  data() {
    const ui = this.ui;
    let ctx = {
      ui,

      inited: false,

      detailView: false,

      selectedItems: [],

      cpId: this.cpId,

      query: this.filters,

      listInfo: {
        list: [],
        pageSize: 0,
        size: 0
      }
    };

    return { ctx };
  },

  created() {
    let filters = {};
    const {query} = this.ctx;
    if (query) {
      filters = JSON.parse(decodeURIComponent(atob(query)));
    }

    const {params} = routerSvc.getCurrentRoute();
    if (+params.cpId > 0) {
      filters['CollectionProtocol.id'] = [+params.cpId, +params.cpId];
      this.ctx.reinit = true;
    }

    if (Object.keys(filters).length > 0) {
      this.ctx.query = util.uriEncode(filters);
    }

    this.ctx.inited = true;
  },

  watch: {
    '$route.params.cpId': function(newValue, oldValue) {
      const cpId = this.ctx.cpId = newValue;
      if (newValue == undefined || newValue == oldValue) {
        // new value is undefined when the route changes
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.items.find(rowObject => rowObject.id == cpId);
        if (!selectedRow) {
          selectedRow = {id: cpId};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    },

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
      const params = event.hidden || {};
      alert(JSON.stringify(params));
    },

    showDetails: function(rowObject) {
      this.ctx.selectedItem = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.list.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      if (this.ctx.reinit) {
        this.ctx.reinit = null;
        this.$refs.list.clearFilters();
      }

      this.ctx.detailView = false;
      this.$refs.list.switchToTableView();
      routerSvc.goto('CpsList', {cpId: -1}, {filters: this.ctx.query});
      if (reload) {
        this.reloadList();
      }
    },

    getListItemsCount: function() {
      this.$refs.list.loadListSize().then(
        () => {
          this.ctx.listInfo.size = this.$refs.list.size;
        }
      );
    },

    onListLoad: function({widget, filters}) {
      const numFormatter = Intl.NumberFormat().format;
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

      if (this.ctx.cpId <= 0) {
        routerSvc.replace('CpsList', {cpId: -1}, { filters });
      } else {
        let selectedRow = this.items.find(row => row.id == this.ctx.cpId);
        if (!selectedRow) {
          selectedRow = {id: this.ctx.cpId};
        }

        this.showDetails(selectedRow);
      }

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
    }
  }
}
</script>
