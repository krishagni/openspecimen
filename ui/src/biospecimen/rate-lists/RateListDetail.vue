<template>
  <os-page>
    <os-page-head :noNavButton="true">
      <span class="os-title">
        <h3>#{{ctx.rateList.id}} {{ctx.rateList.name}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div>
        <os-tab-menu>
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Services')">
                <span v-t="'lab_services.services'">Services</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('CollectionProtocols')">
                <span v-t="'lab_services.cps'">Collection Protocols</span>
              </router-link>
            </li>
          </ul>
        </os-tab-menu>

        <router-view :rate-list="ctx.rateList" v-if="ctx.rateList.id > 0"
          @rate-list-saved="onRateListSave($event)"
          @rate-list-services-added="onRateListServicesAdd($event)"
          @rate-list-cps-added="onRateListCpsAdd($event)"
          @rate-list-cps-removed="onRateListCpsRemove($event)" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import rateListSvc from '@/biospecimen/services/RateList.js';

export default {
  props: ['rateListId'],

  emits: ['rate-list-saved', 'rate-list-services-added', 'rate-list-cps-added', 'rate-list-cps-removed'],

  data() {
    return {
      ctx : {
        rateList: {}
      }
    };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }

    this._loadRateList();
  },

  watch: {
    rateListId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._loadRateList();
      }
    }
  },

  methods: {
    onRateListSave: function(rateList) {
      this.ctx.rateList = rateList;
      this.$emit('rate-list-saved', rateList);
    },

    onRateListServicesAdd: function({count}) {
      const {rateList} = this.ctx;
      this.$emit('rate-list-services-added', {rateList, count});
    },

    onRateListCpsAdd: function({count}) {
      const {rateList} = this.ctx;
      this.$emit('rate-list-cps-added', {rateList, count});
    },

    onRateListCpsRemove: function({count}) {
      const {rateList} = this.ctx;
      this.$emit('rate-list-cps-removed', {rateList, count});
    },

    getRoute: function(routeName, params, query) {
      return {
        name: this.detailRouteName + '.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    },

    _loadRateList: async function() {
      this.ctx.rateList = await rateListSvc.getRateList(+this.rateListId);
    }
  }
}
</script>
