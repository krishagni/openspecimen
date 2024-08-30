
<template>
  <div class="os-search">
    <AutoComplete ref="searchOverlay" :placeholder="'&#xE908; ' + $t('common.quick_search.search')" v-model="selectedResult"
      :suggestions="searchResults" :delay="searchDelay" @complete="search"
      @item-select="goto($event.originalEvent, $event.value)">

      <template #header v-if="searchResults.length > 20">
        <span class="os-search-many-matches">
          <span v-t="'common.quick_search.many_matches'">Many matches found. Keep typing...</span>
        </span>
      </template>

      <template #item="slotProps">
        <a class="os-search-no-match" :href="trainingLink + slotProps.item.key" target="_blank" rel="noopener"
          v-if="slotProps.item.id == -1">
          <os-icon name="graduation-cap" />
          <os-html :content="slotProps.item.caption" />
        </a>

        <div class="os-search-match" v-else>
          <div class="category">
            <span>{{slotProps.item.category}}</span>
          </div>
          <div class="match">
            <div class="id">
              <span class="text">
                <os-html :content="slotProps.item.matchedText" />
              </span>
              <a class="link" :href="slotProps.item.url" target="_blank" rel="noopener">
                <os-icon name="external-link-alt"/>
              </a>
            </div>
            <div class="props" v-if="slotProps.item.props">
              <span>{{slotProps.item.props}}</span>
            </div>
          </div>
        </div>
      </template>
    </AutoComplete>
  </div>
</template>

<script>
import AutoComplete from 'primevue/autocomplete';

import searchSvc from '@/common/services/QuickSearch.js';
import routerSvc from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';

export default {
  components: {
    AutoComplete
  },

  data() {
    return {
      searchDelay: this.$ui.global.appProps.searchDelay,

      selectedResult: null,

      searchResults: [],

      trainingLink: ''
    }
  },

  mounted() {
    this.$watch(
      () => this.$refs.searchOverlay.overlayVisible,
      (val) => {
        if (!val) {
          this.selectedResult = null;
        }
      }
    );

    settingSvc.getSetting('training', 'manual_search_link').then(setting => this.trainingLink = setting[0].value);
  },

  methods: {
    search: async function(event)  {
      this.searchResults = await searchSvc.search(event.query.trim());
    },

    goto: function(event, match) {
      let tgt = event && event.target;
      while (tgt) {
        if (tgt.tagName && tgt.tagName.toUpperCase() == 'A') {
          return;
        }

        if (tgt.className && typeof tgt.className.indexOf == 'function' &&
            (tgt.className.indexOf('os-search-match') != -1 || tgt.className.indexOf('os-search-no-match') != -1)) {
          break;
        }

        tgt = tgt.parentNode;
      }

      if (match.id == -1) {
        window.open(this.trainingLink + match.key, '_blank');
      } else if (match.searchEntity.ngView) {
        window.location.href = match.url;
      } else {
        routerSvc.goto(match.route.name, match.route.params);
      }
    }
  }
}

</script>

<style scoped>

.os-search {
  margin: auto 1rem;
  width: 100%;
}

.os-search :deep(.p-autocomplete) {
  border-radius: 4px;
  width: 100%;
}

.os-search :deep(.p-autocomplete-input) {
  font-family: "Helvetica Neue", Helvetica, Arial, sans-serif, primeicons;
  width: 100%;
  padding: 0.375rem 0.75rem;
}

.os-search-match {
  width: 100%;
  display: block;
}

.os-search-match:after {
  clear: both;
  display: block;
  content: '';
}

.os-search-match .category {
  float: left;
  width: 50%;
  padding-right: 0.5rem;
  font-size: 85%;
}

.os-search-match .match {
  float: left;
  width: 50%;
  white-space: pre-wrap;
}

.os-search-match .match .id {
  display: block;
  margin-bottom: 0.25rem;
}

.os-search-match .match .id:after {
  clear: both;
  display: block;
  content: '';
}

.os-search-match .match .id .text {
  float: left;
  display: inline-block;
  width: calc(100% - 1.5rem);
}

.os-search-match .match .id .link {
  float: right;
  display: inline-block;
}

.os-search-match .match .props {
  font-size: 85%;
}

.os-search .p-autocomplete-input {
  width: 100%;
}

</style>

<style>
.os-search-no-match {
  width: 100%;
}

.os-search-no-match .os-icon-wrapper {
  margin-right: 0.5rem;
}

.os-search-many-matches {
  display: inline-block;
  padding: 0.5rem 1.5rem;
  border-bottom: 1px solid #ddd;
  width: 100%;
  color: #777;
}

.os-match-highlight {
  font-weight: bold;
}
</style>
