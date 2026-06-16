<template>
  <os-card>
    <template #header>
      <div class="toolbar" v-if="!ctx.showSearch">
        <span class="title">
          <os-icon :name="icon" />
          <span>{{title}}</span>
        </span>
        <span class="actions">
          <os-button size="small" left-icon="search" @click="showSearchText"
            v-os-tooltip.bottom="$t('common.buttons.search')" v-if="!hideSearch" />
          <os-button-link size="small" left-icon="expand" :url="detailedListUrl" :no-outline="true"
            v-os-tooltip.bottom="$t('common.expand_to_show_more_details')" v-if="listUrl" />
        </span>
      </div>
      <div class="toolbar" v-else>
        <os-input-text md-type="true" v-model="ctx.search" />
        <span class="actions">
          <os-button size="small" left-icon="times" @click="closeSearchText" />
        </span>
      </div>
    </template>
    <template #body>
      <table class="os-table">
        <tbody class="os-table-body">
          <tr class="row" v-for="item in list" :key="item.id">
            <td class="col" style="width: 40px;" v-show="showStar">
              <os-star :starred="item.starred" @star-toggled="toggleStar(item)" />
            </td>
            <td class="col">
              <a :href="item.url">
                <span>{{item.displayName}}</span>
              </a>
            </td>
            <td class="col" v-if="$slots.actions">
              <slot name="actions" v-bind:item="item" v-bind:model="item"> </slot>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
  </os-card>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['icon', 'title', 'show-star', 'list-url', 'list', 'hide-search'],

  emits: ['search', 'toggle-star'],

  data() {
    return {
      ctx: {
        showSearch: false,

        search: null
      }
    };
  },

  watch: {
    'ctx.search': function() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }

      this.searchTimer = setTimeout(() => this.$emit('search', {searchTerm: this.ctx.search}), 500);
    }
  },

  computed: {
    detailedListUrl: function() {
      const {name, params, query} = this.listUrl || {};
      return routerSvc.getUrl(name, params, query);
    }
  },

  methods: {
    showSearchText: function() {
      this.ctx.showSearch = true;
    },

    closeSearchText: function() {
      this.ctx.showSearch = false;
      this.ctx.search = null;
    },

    toggleStar: function(item) {
      this.$emit('toggle-star', item);
    }
  }
}
</script>
