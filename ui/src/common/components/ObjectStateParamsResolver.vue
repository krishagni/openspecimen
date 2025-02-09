<template>
  <div>
    <pre>{{stateParams}}</pre>
  </div>
</template>

<script>
import http      from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

const PATH_PARAMS = [
  'cpId',
  'cprId',
  'visitId',
  'specimenId',
  'stateName',
  'siteId'
];

export default {
  props: ['params'],

  data() {
    return {
      stateParams: {}
    }
  },

  created() {
    this._getStateParams().then(
      stateParams => {
        const {stateName} = this.params || {};
        this.stateParams = stateParams;

        const query = {...stateParams};
        for (const attr of PATH_PARAMS) {
          delete query[attr];
        }

        routerSvc.goto(stateParams.stateName || stateName, stateParams, query);
      }
    );
  },

  methods: {
    _getStateParams: function() {
      const {objectName, key, value} = this.params || {};
      return http.get('object-state-params', {objectName, key, value});
    }
  }
}
</script>
