<template>
  <span>resolving visit view...</span>
</template>

<script>
import http      from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['visitId', 'query'],

  created() {
    http.get('object-state-params', this._getQueryParams()).then(
      ({cpId, cprId, visitId}) => {
        const {view, ...otherQuery} = this.query || {};
        if (view) {
          routerSvc.goto(view, {cpId, cprId, visitId}, otherQuery);
        } else {
          routerSvc.goto('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId});
        }
      }
    );
  },

  methods: {
    _getQueryParams: function() {
      return {objectName: 'visit', key: 'id', value: this.visitId};
    }
  }
}
</script>
