
import alertsSvc from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import util from '@/queries/services/Util.js';

class Query {
  async getCount(query, facets) {
    const payload = {
      savedQueryId: query.id,
      cpId: query.cpId,
      cpGroupId: query.cpGroupId,
      drivingForm: 'Participant',
      caseSensitive: query.caseSensitive,
      runType: 'Count',
      aql: await util.getCountAql(query, facets)
    };

    return http.post('query', payload).then(
      ({rows}) => {
        const cprs    = +rows[0][0];
        const visits  = +rows[0][1];
        let specimens = +rows[0][2];
        for (let idx = 3; idx < rows[0].length; ++idx) {
          specimens += +rows[0][idx];
        }

        return {cprs, visits, specimens};
      }
    );
  }

  async getData(query, facets, addPropIds, addLimit) {
    const payload = {
      savedQueryId: query.id,
      cpId: query.cpId,
      cpGroupId: query.cpGroupId,
      drivingForm: 'Participant',
      runType: 'Data',
      aql: await util.getDataAql(query, facets, addPropIds, addLimit),
      wideRowMode: query.wideRowMode || 'OFF',
      outputIsoDateTime: true, // TODO: (outputIsoFmt || false),
      outputColumnExprs: query.outputColumnExprs || false,
      caseSensitive: (query.caseSensitive == undefined || query.caseSensitive == null || query.caseSensitive)
    };

    return http.post('query', payload);
  }

  async exportData(query, facets) {
    const payload = {
      savedQueryId: query.id,
      cpId: query.cpId,
      cpGroupId: query.cpGroupId,
      drivingForm: 'Participant',
      runType: 'Export',
      aql: await util.getDataAql(query, facets, false, false),
      wideRowMode: query.wideRowMode || 'OFF',
      outputIsoDateTime: false, // TODO: (outputIsoFmt || false),
      outputColumnExprs: query.outputColumnExprs || false,
      caseSensitive: (query.caseSensitive == undefined || query.caseSensitive == null || query.caseSensitive)
    };

    alertsSvc.info({code: 'queries.results_csv_download_initiated', args: query});
    return http.post('query/export', payload).then(
      ({completed, dataFile}) => {
        if (!completed) {
          alertsSvc.info({code: 'queries.more_time_to_export', args: query});
          return;
        }

        http.downloadFile(http.getUrl('query/export', {query: {fileId: dataFile}}));
        alertsSvc.info({code: 'queries.results_csv_downloaded', args: query});
      }
    );
  }

  async getFacetValues(query, facet, searchTerm) {
    const restriction = await util.getWhereAql(query);
    const payload = {
      cpId: query.cpId,
      cpGroupId: query.cpGroupId,
      facets: [facet],
      searchTerm,
      restriction
    };

    return http.post('query/facet-values', payload).then(values => values && values.length > 0 ? values[0] : null);
  }
}

export default new Query();
