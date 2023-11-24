
import http from '@/common/services/HttpClient.js';

class Visit {
  async getVisits(cpr, includeStats = true, sortByDates) {
    if (!cpr.$visitsQ) {
      cpr.$visitsQ = http.get('visits', {cprId: cpr.id, includeStats, sortByDates});
    }

    return cpr.$visitsQ.then(
      (visits) => {
        for (let visit of visits) {
          visit.totalPrimarySpmns = visit.pendingPrimarySpmns + visit.plannedPrimarySpmnsColl +
            visit.uncollectedPrimarySpmns + visit.unplannedPrimarySpmnsColl;
          visit.reqStorage = visit.storedSpecimens + visit.notStoredSpecimens +
            visit.distributedSpecimens + visit.closedSpecimens;
        }

        return visits;
      }
    );
  }
}

export default new Visit();
