
import http from '@/common/services/HttpClient.js';
import util from '@/queries/services/Util.js';

class Query {
  async getCount(query) {
    const payload = {
      savedQueryId: query.id,
      cpId: query.cpId,
      cpGroupId: query.cpGroupId,
      drivingForm: 'Participant',
      caseSensitive: query.caseSensitive,
      runType: 'Count',
      aql: await util.getCountAql(query)
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

  async getData(query, addPropIds, addLimit) {
    const payload = {
      savedQueryId: query.id,
      cpId: query.cpId,
      cpGroupId: query.cpGroupId,
      drivingForm: 'Participant',
      runType: 'Data',
      aql: await util.getDataAql(query, addPropIds, addLimit),
      wideRowMode: query.wideRowMode || 'OFF',
      outputIsoDateTime: true, // TODO: (outputIsoFmt || false),
      outputColumnExprs: query.outputColumnExprs || false,
      caseSensitive: (query.caseSensitive == undefined || query.caseSensitive == null || query.caseSensitive)
    };

    return http.post('query', payload);
  }
}

export default new Query();
