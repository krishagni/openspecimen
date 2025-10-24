<template>
  <div>
    <div class="toolbar">
      <span class="left">
        <span v-if="ctx.selected && ctx.selected.length > 0">
          <os-button left-icon="times" :label="$t('common.buttons.remove')"
            v-os-tooltip.bottom="$t('carts.rm_spmns_from_picked_list')" @click="removeSpecimens" />

          <os-specimen-actions :specimens="ctx.selected" @reloadSpecimens="reloadSpecimens" />
        </span>
      </span>
      <span class="right">
        <os-button left-icon="search" :label="$t('common.buttons.search')" @click="toggleSearch" />
      </span>
    </div>

    <div class="content">
      <os-list-view
        :context="listViewCtx"
        :data="ctx.list || []"
        :schema="listSchema"
        :query="ctx.query"
        :allowSelection="true"
        :loading="ctx.loading"
        @filtersUpdated="loadSpecimens"
        @selectedRows="selectSpecimens"
        ref="listView"
      />
    </div>
  </div>
</template>

<script>

import alertsSvc   from '@/common/services/Alerts.js';
import cartSvc     from '@/biospecimen/services/SpecimenCart.js';

import listSchema  from "@/biospecimen/schemas/carts/picked-specimens.js";

export default {
  props: ['cart', 'pick-list', 'filters', 'active-tab'],

  data() {
    return {
      ctx: {
        list: null,

        query: this.filters,

        selected: null,

        unpicked: 0
      },

      listSchema
    };
  },

  created() {
  },

  mounted() {
    this._loadSpecimens();
  },

  computed: {
    listViewCtx: function() {
      return {cart: this.cart, pickList: this.pickList}
    }
  },

  watch: {
    activeTab: function() {
      this._loadSpecimens();
    }
  },

  methods: {
    nullifyList: function() {
      this.ctx.list = this.ctx.selected = null;
      this.ctx.unpicked = 0;
    },

    loadSpecimens: function({filters, uriEncoding, pageSize}) {
      this.ctx.filters = filters;
      this.ctx.pageSize = pageSize;

      this.nullifyList();

      this._loadSpecimens();
      this.$emit('specimens-loaded', {uriEncoding});
    },

    selectSpecimens: function(selected) {
      this.ctx.selected = selected.map(({rowObject: {specimen}}) => ({id: specimen.id, cpId: specimen.cpId}));
    },

    toggleSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    reloadSpecimens: function() {
      this.$refs.listView.reload();
    },

    removeSpecimens: function() {
      cartSvc.unpickSpecimens(this.cart.id, this.pickList.id, this.ctx.selected).then(
        ({unpicked}) => {
          alertsSvc.success({code: 'carts.specimens_unpicked', args: {count: unpicked.length}});

          this.$refs.listView.clearSelection();
          this.ctx.selected = null;
          this.ctx.unpicked += unpicked.length;
            
          if (this.ctx.list) {
            this.ctx.list = this.ctx.list.filter(item => unpicked.indexOf(item.specimen.id) == -1);
          }
            
          if (this.ctx.unpicked >= 50) {
              this.nullifyList();
              this._loadSpecimens();
          }

          this.$emit('unpicked-specimens', unpicked);
        }
      );
    },

    _loadSpecimens: function() {
      if (this.ctx.list || !this.activeTab) {
        return;
      }

      this.ctx.loading = true;
      cartSvc.getPickedSpecimens(this.cart.id, this.pickList.id, this.ctx.filters || {}).then(
        items => {
          this.ctx.loading = false;
          this.ctx.list = items;
          this.ctx.unpicked = 0;
        }
      );
    }
  }
}
</script>
