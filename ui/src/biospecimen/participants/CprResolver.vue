<template>
  <span>resolving registration view...</span>
</template>

<script>
import http      from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cprId', 'query'],

  created() {
    http.get('object-state-params', this._getQueryParams()).then(
      ({cpId, cprId}) => {
        const {view, ...otherQuery} = this.query || {};
        if (view) {
          routerSvc.goto(view, {cpId, cprId}, otherQuery);
        } else {
          routerSvc.goto('ParticipantsListItemDetail.Overview', {cpId, cprId});
        }
      }
    );
  },

  methods: {
    _getQueryParams: function() {
      return {objectName: 'collection_protocol_registration', key: 'id', value: this.cprId};
    }
  }
}
</script>
