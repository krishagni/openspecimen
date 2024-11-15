import savedQuerySvc from './SavedQuery.js';

class QueriesCache {
  queriesQ = {};

  init() {
    this.queriesQ = {};
  }

  destroy() {
    this.queriesQ = {};
  }

  getQuery(queryId) {
    if (!this.queriesQ[queryId]) {
      this.queriesQ[queryId] = savedQuerySvc.getQueryById(queryId);
    }

    return this.queriesQ[queryId];
  }
}

export default new QueriesCache();
