<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span>
        <h3 v-t="'participants.list'" v-if="ctx.view == 'participants_list'">Participants</h3>
        <h3 v-t="'specimens.list'" v-else>Specimens</h3>
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
        <template #default v-if="ctx.view == 'participants_list'">
          <span v-if="!ctx.selectedItems || ctx.selectedItems.length == 0">
            <os-button left-icon="plus" :label="$t('participants.add_participant')"
              @click="addParticipant" v-if="ctx.cp.activityStatus != 'Closed' && ctx.access.regAllowed" />

            <os-button left-icon="flask" :label="$t('participants.view_specimens')"
              @click="viewSpecimens" v-if="ctx.access.viewSpecimensAllowed" />

            <os-button left-icon="calendar-alt" :label="$t('participants.view_cp_details')"
              @click="$goto('CpDetail.Overview', {cpId: ctx.cp.id})" />

            <os-button-link left-icon="procedures" :label="$t('participants.view_sop')"
              :url="sopUrl" :new-tab="true" v-if="sopUrl" />

            <os-menu :label="$t('common.buttons.more')" :options="ctx.moreOptions" />
          </span>
          <span v-else>
            <os-button left-icon="edit" :label="$t('common.buttons.edit')"
              @click="editParticipants" v-if="ctx.access.updateParticipantAllowed" />

            <os-button left-icon="trash" :label="$t('common.buttons.delete')"
              @click="deleteParticipants" v-if="ctx.access.deleteParticipantAllowed" />

            <os-plugin-views page="participants-list" view="bulk-ops" :view-props="ctx" />
          </span>
        </template>

        <template #default v-else-if="ctx.view == 'specimens_list'">
          <span v-if="!ctx.selectedItems || ctx.selectedItems.length == 0">
            <os-button left-icon="user-friends" :label="$t('participants.view_participants')"
              @click="viewParticipants" v-if="!ctx.cp.specimenCentric" />

            <os-button left-icon="plus" :label="$t('participants.add_specimen')" @click="addSpecimen"
              v-else-if="ctx.cp.activityStatus != 'Closed' && ctx.access.createPrimarySpecimensAllowed" />

            <os-button left-icon="calendar-alt" :label="$t('participants.view_cp_details')"
              @click="$goto('CpDetail.Overview', {cpId: ctx.cp.id})" />

            <os-button-link left-icon="procedures" :label="$t('participants.view_sop')"
              :url="sopUrl" :new-tab="true" v-if="sopUrl" />

            <os-menu :label="$t('common.buttons.more')" :options="ctx.moreOptions" />
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
        :id-filter="'Participant.id'"
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
        :id-filter="'Specimen.id'"
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

    <os-plugin-views ref="moreMenuPluginViews" page="participants-list" view="menu" :viewProps="ctx" />

    <os-confirm-delete ref="deleteParticipantsDialog" :captcha="false" :collectReason="true">
      <template #message>
        <span v-t="'participants.delete_selected'">Are you sure you want to delete the selected participants along with their associated visits and specimens data?</span>
      </template>
    </os-confirm-delete>
  </os-page>
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc    from '@/biospecimen/services/Cpr.js';
import http      from '@/common/services/HttpClient.js';
import itemsSvc  from '@/common/services/ItemsHolder.js';
import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';
import wfSvc     from '@/biospecimen/services/Workflow.js';

export default {
  name: 'ParticipantsList',

  inject: ['ui', 'cpViewCtx'],

  props: ['filters', 'view'],

  data() {
    const ui = this.ui;
    const cpViewCtx = this.cpViewCtx;
    const cp = cpViewCtx.getCp();

    console.log('List view initialisation: ' + JSON.stringify(this.$props));
    let ctx = {
      ui,

      cp,

      inited: false,

      selectedItems: [],

      query: this.filters,

      view: cp.specimenCentric ? 'specimens_list' : this.view,

      listInfo: {
        list: [],

        pageSize: 0,

        size: 0
      },

      moreOptions: [],

      access: {
        regAllowed: cpViewCtx.isCreateParticipantAllowed(),

        updateParticipantAllowed: cpViewCtx.isUpdateParticipantAllowed(),

        deleteParticipantAllowed: cpViewCtx.isDeleteParticipantAllowed(),

        createPrimarySpecimensAllowed: cpViewCtx.isCreatePrimarySpecimenAllowed(),

        viewSpecimensAllowed: cpViewCtx.isReadSpecimenAllowed(),
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

    if (Object.keys(filters).length > 0) {
      this.ctx.query = util.uriEncode(filters);
    }

    this.ctx.inited = true;
    this._loadMoreOptions();
  },

  watch: {
    '$route.query.view': function(newValue) {
      console.log('Detected change in view. View = ' + newValue + ', params = ' + JSON.stringify(this.$route.params));
      this.ctx.view = newValue || 'participants_list';

      //
      // when the list view changes, reset the filters
      // for example Specimen.id filter has no meaning in participants list view
      //
      this.ctx.query = util.uriEncode({});
    },

    '$route.query.filters': function(newValue) {
      this.ctx.query = newValue;
    }
  },

  computed: {
    bcrumb: function () {
      const cp = this.cpViewCtx.getCp();
      return [
        {url: routerSvc.getUrl('CpsList'), label: i18n.msg('participants.collection_protocols')},
        {url: routerSvc.getUrl('DefCpListView', {cpId: cp.id}), label: cp.shortTitle}
      ];
    },

    itemUrl: function() {
      const baseUrl = "'#/cp-view/' + hidden.cpId + '/participants/' + hidden.cprId";
      if (this.ctx.view == 'participants_list') {
        return baseUrl + " + '/overview'";
      } else {
        return baseUrl + " + '/visit/' + hidden.visitId + '/specimen/' + hidden.specimenId + '/overview'";
      }
    },

    selectedSpecimens: function() {
      const {view, selectedItems} = this.ctx;
      if (!view || view == 'participants_list') {
        return;
      }

      return selectedItems;
    },

    sopUrl: function() {
      const cp = this.cpViewCtx.getCp();
      if (cp.sopDocumentName) {
        return http.getUrl('collection-protocols/' + cp.id + '/sop-document');
      } else if (cp.sopDocumentUrl) {
        return cp.sopDocumentUrl;
      }

      return null;
    }
  },

  methods: {
    reloadList: function() {
      this.$refs.list.reload();
    },

    onItemSelection: function(items) {
      const key = this.ctx.view == 'participants_list' ? 'cprId' : 'specimenId';
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

      routerSvc.replace('ParticipantsList', {cpId: cp.id}, { filters, view: this.ctx.view });
      setTimeout(() => {
        this.ctx.listInfo = {
          list: this.$refs.list.list.rows,
          size: this.$refs.list.size,
          pageSize: this.$refs.list.pageSize
        }
      }, 0);
    },

    addParticipant: async function() {
      const rapidWfId = await this._getRapidCollectionWf();
      if (rapidWfId && rapidWfId > 0) {
        routerSvc.goto('tmWorkflowCreateInstance', {workflowId: rapidWfId}, {cpId: this.ctx.cp.id});
      } else {
        routerSvc.goto('ParticipantAddEdit', {cpId: this.ctx.cp.id, cprId: -1});
      }
    },

    editParticipants: function() {
      itemsSvc.setItems('participants', this.ctx.selectedItems.map(({cprId}) => ({id: cprId})));
      routerSvc.goto('ParticipantsBulkEdit', {cpId: this.ctx.cp.id});
    },

    deleteParticipants: function() {
      this.$refs.deleteParticipantsDialog.open().then(
        ({reason}) => {
          const ids = this.ctx.selectedItems.map(({cprId}) => cprId);
          cprSvc.bulkDelete(ids, reason).then(
            ({completed}) => {
              if (completed) {
                alertsSvc.success({code: 'participants.many_deleted', args: {count: ids.length}});
              } else {
                alertsSvc.info({code: 'participants.delete_more_time'});
              }

              this.reloadList();
            }
          );
        }
      );
    },

    viewSpecimens: function() {
      routerSvc.goto('ParticipantsList', {cpId: this.ctx.cp.id}, {view: 'specimens_list'});
    },

    viewParticipants: function() {
      routerSvc.goto('ParticipantsList', {cpId: this.ctx.cp.id}, {view: 'participants_list'});
    },

    addSpecimen: function() {
      wfSvc.addSpecimen(this.ctx.cp, {cpId: this.ctx.cp.id});
    },

    openSearch: function () {
      this.$refs.list.toggleShowFilters();
    },

    generateCpReport: function() {
      const status = cpSvc.generateCpReport(this.ctx.cp.id);
      if (status) {
        alertsSvc.success({code: 'participants.preparing_report', args: this.ctx.cp});
      }
    },

    _loadMoreOptions: async function() {
      const options = [];
      if (!this.cpViewCtx.isCoordinator() && this.cpViewCtx.isUpdateAllSpecimenAllowed()) {
        try {
          const recvWfId = await this._getReceiveSpecimensWf();
          if (recvWfId) {
            options.push({
              icon: 'check-square',
              caption: this.$t('participants.receive'),
              url: routerSvc.getUrl('tmWorkflowCreateInstance', {workflowId: recvWfId}, {cpId: this.ctx.cp.id})
            });
          }
        } catch {
          alertsSvc.error({code: 'participants.invalid_receive_wf'});
        }
      }

      if (this.cpViewCtx.isImportAllowed()) {
        if (options.length > 0) {
          options.push({divider: true});
        }

        options.push({
          icon: 'upload',
          caption: this.$t('participants.import_biospecimen_data'),
          url: routerSvc.getUrl('CpImportRecords', {cpId: this.ctx.cp.id})
        });

        options.push({
          icon: 'list',
          caption: this.$t('participants.view_past_imports'),
          url: routerSvc.getUrl('CpImportJobsList', {cpId: this.ctx.cp.id})
        });
      }

      if (this.cpViewCtx.isExportAllowed()) {
        if (options.length > 0) {
          options.push({divider: true});
        }

        options.push({
          icon: 'download',
          caption: this.$t('common.buttons.export'),
          url: routerSvc.getUrl('CpExportRecords', {cpId: this.ctx.cp.id})
        });
      }

      if (options.length > 0) {
        options.push({divider: true});
      }

      options.push({
        icon: 'file-pdf',
        caption: this.$t('participants.generate_cp_report'),
        onSelect: this.generateCpReport
      });

      const ctxt = {...this.ctx, cpViewCtx: this.cpViewCtx};
      util.getPluginMenuOptions(this.$refs.moreMenuPluginViews, 'participants-list', 'menu', ctxt)
        .then(pluginOptions => this.ctx.moreOptions = options.concat(pluginOptions));
    },

    _getReceiveSpecimensWf: async function() {
      return this.cpViewCtx.getReceiveSpecimensWf();
    },

    _getRapidCollectionWf: async function() {
      return this.cpViewCtx.getRapidCollectionWf();
    }
  }
}
</script>
