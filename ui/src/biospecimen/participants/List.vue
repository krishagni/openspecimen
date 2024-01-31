<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <template #breadcrumb v-if="!ctx.detailView">
            <os-breadcrumb :items="ctx.bcrumb" />
          </template>

          <span>
            <h3 v-t="'participants.list'" v-if="ctx.view == 'participants_list'">Participants</h3>
            <h3 v-t="'specimens.list'" v-else>Specimens</h3>
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
            <template #default v-if="ctx.view == 'participants_list'">
              <span v-if="!ctx.selectedItems || ctx.selectedItems.length == 0">
                <os-button left-icon="plus" :label="$t('participants.add_participant')" @click="addParticipant" />

                <os-button left-icon="flask" :label="$t('participants.view_specimens')" @click="viewSpecimens" />
              </span>
            </template>

            <template #default v-else-if="ctx.view == 'specimens_list'">
              <span v-if="!ctx.selectedItems || ctx.selectedItems.length == 0">
                <os-button left-icon="user-friends" :label="$t('participants.view_participants')"
                  @click="viewParticipants" />
              </span>
              <span v-else>
                <os-specimen-actions :cp="ctx.cp" :specimens="selectedSpecimens" @reloadSpecimens="reloadList" />

                <os-add-to-cart :specimens="selectedSpecimens" />
              </span>
            </template>

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
            :selected="ctx.selectedItem"
            :include-count="includeCount"
            :url="itemUrl"
            :newUiUrl="true"
            :newTab="false"
            @selectedRows="onItemSelection"
            @rowClicked="onItemRowClick"
            @listLoaded="onListLoad"
            ref="list"
            v-if="ctx.view == 'participants_list'"
          />

          <os-query-list-view
            name="specimen-list-view"
            :object-id="ctx.cp.id"
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
            v-else
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.cprId > 0">
      <router-view :cprId="$route.params.cprId" :key="$route.params.cprId" />
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

  props: ['filters', 'cprId', 'specimenId', 'view'],

  async setup(props) {
    const ui = inject('ui');

    const cpViewCtx = inject('cpViewCtx').value;
    const cp = await cpViewCtx.getCp();

    console.log('List view initialisation: ' + JSON.stringify(props));
    let ctx = reactive({
      ui: ui,

      cp: cp,

      inited: true,

      detailView: false,

      selectedItems: [],

      cprId: props.cprId,

      specimenId: props.specimenId,

      query: props.filters,

      view: props.view,

      listInfo: {
        list: [],
        pageSize: 0,
        size: 0
      },

      bcrumb: [ 
        // TODO: change the breadcrumbs
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
    '$route.query.view': function(newValue) {
      console.log('Detected change in view. View = ' + newValue + ', params = ' + JSON.stringify(this.$route.params));
      this.ctx.view = newValue || 'participants_list';
    },

    '$route.params.cprId': function(newValue, oldValue) {
      console.log('Detected change in CPR ID. CPR ID = ' + newValue + ', params = ' + JSON.stringify(this.$route.params));
      const cprId = this.ctx.cprId = newValue;
      if (this.ctx.view != 'participants_list' || newValue == undefined || newValue == oldValue) {
        // new value is undefined when the route changes
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.items.find(rowObject => rowObject.id == cprId);
        if (!selectedRow) {
          selectedRow = {id: cprId};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    },

    '$route.params.specimenId': function(newValue, oldValue) {
      console.log('Detected change in specimen ID. Specimen ID = ' + newValue + ', params = ' + JSON.stringify(this.$route.params));
      const specimenId = this.ctx.specimenId = newValue;
      if (this.ctx.view != 'specimens_list' || newValue == oldValue) {
        return;
      }

      if (newValue == undefined && this.$route.params.cprId != -1) {
        // new value is undefined when the route changes
        // cprId != -1 when participant or visit overview link is clicked in specimens list view
        this.ctx.view = 'participants_list';
        this.ctx.cprId = +this.$route.params.cprId;
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.items.find(rowObject => rowObject.id == specimenId);
        if (!selectedRow) {
          selectedRow = {id: specimenId};
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
      if (this.ctx.view == 'participants_list') {
        return "'#/cp-view/' + hidden.cpId + '/participants/' + hidden.cprId + '/detail/overview?view=participants_list'";
      } else {
        return "'#/cp-view/' + hidden.cpId + '/participants/' + hidden.cprId + '/visit/' + hidden.visitId + '/specimen/' + hidden.specimenId + '/detail/overview?view=specimens_list'";
      }
    },

    selectedSpecimens: function() {
      const {view, selectedItems} = this.ctx;
      if (!view || view == 'participants_list') {
        return;
      }

      return selectedItems;
    }
  },

  methods: {
    reloadList: function() {
      this.$refs.list.reload();
    },

    onItemSelection: function(items) {
      const key = this.ctx.query == 'participants_list' ? 'cprId' : 'specimenId';
      this.ctx.selectedItems = items.map(({rowObject}) => ({...rowObject.hidden, id: +rowObject.hidden[key]}));
    },

    onItemRowClick: function(event) {
      const params = event.hidden || {};
      const {view} = this.ctx;
      if (view == 'participants_list') {
        routerSvc.goto('ParticipantsListItemDetail.Overview', { ...params }, {filters: this.ctx.query, view});
      } else {
        routerSvc.goto('ParticipantsListItemSpecimenDetail.Overview', {...params }, {filters: this.ctx.query, view});
      }
    },

    showDetails: function(rowObject) {
      this.ctx.selectedItem = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.list.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.list.switchToTableView();
      routerSvc.goto('ParticipantsList', {cprId: -1}, {filters: this.ctx.query, view: this.ctx.view});
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
      const cp = this.ctx.cp;

      this.showListSize = true;
      this.ctx.selectedItems.length = 0;
      this.items = widget.data;
      this.items.forEach(
        row => {
          const hidden = row.hidden = row.hidden || {};
          hidden.cpId = cp.id;
          row.id = this.ctx.view == 'participants_list' ? hidden.cprId : hidden.specimenId;
        }
      );

      if (this.ctx.cprId <= 0) {
        routerSvc.goto('ParticipantsList', {cpId: cp.id, cprId: -1}, { filters, view: this.ctx.view });
      } else {
        const selectedItemId = this.ctx.view == 'participants_list' ? this.ctx.cprId : this.ctx.specimenId;
        let selectedRow = this.items.find(row => row.id == selectedItemId);
        if (!selectedRow) {
          selectedRow = {id: selectedItemId};
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

    addParticipant: function() {
      routerSvc.goto('ParticipantAddEdit', {cpId: this.ctx.cp.id, cprId: -1});
    },

    viewSpecimens: function() {
      routerSvc.goto('ParticipantsList', {cpId: this.ctx.cp.id, cprId: -1}, {view: 'specimens_list'});
    },

    viewParticipants: function() {
      routerSvc.goto('ParticipantsList', {cpId: this.ctx.cp.id, cprId: -1}, {view: 'participants_list'});
    },

    openSearch: function () {
      this.$refs.list.toggleShowFilters();
    }
  }
}
</script>
