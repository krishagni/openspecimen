import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import i18n  from '@/common/services/I18n.js';

class Workflow {
  osSvc = window.osSvc;

  wfInstanceSvc = window.osSvc.tmWfInstanceSvc;

  async addSpecimen(visit) {
    if (!this.wfInstanceSvc) {
      alert('Workflow module not installed!');
      return;
    }

    let wfName = await this._getCollectUnplannedSpmnsWf(visit);
    if (!wfName) {
      wfName = 'sys-collect-adhoc-specimens';
    } 
          
    const inputItem = {
      cpr  : {id: visit.cprId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle},
      visit: {id: visit.id,    cpId: visit.cpId, cpShortTitle: visit.cpShortTitle}
    };
          
    const params = this._getVisitBreadcrumb(visit, i18n.msg('participants.add_specimen'));
    params['breadcrumb-3'] = JSON.stringify({
      label: this._getVisitDescription(visit),
      route: { 
        name: 'ParticipantsListItemVisitDetail.Overview',
        params: {cpId: visit.cpId, cprId: visit.cprId, visitId: visit.id}
      }
    });

    const opts = {inputType: 'visit', params};
    const instance = await this.wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
    this.wfInstanceSvc.gotoInstance(instance.id);
  }

  _getCollectUnplannedSpmnsWf(visit) {
    return cpSvc.getWorkflowProperty(visit.cpId, 'common', 'collectUnplannedSpecimensWf');
  }

  _getVisitBreadcrumb(visit, title) {
    return {
      returnOnExit: 'current_view',
      cpId: visit.cpId,
      'breadcrumb-1': JSON.stringify({
        label: visit.cpShortTitle,
        route: {name: 'ParticipantsList', params: {cpId: visit.cpId, cprId: -1}}
      }),
      'breadcrumb-2': JSON.stringify({
        label: visit.ppid,
        route: {name: 'ParticipantsListItemDetail.Overview', params: {cpId: visit.cpId, cprId: visit.cprId}}
      }),
      batchTitle: title,
      showOptions: false
    };
  }

  _getVisitDescription(visit) {
    let description = visit.description || 'Unknown';
    let idx = description.indexOf(' / ');
    if (idx >= 0) {
      description = description.substring(0, idx);
    }

    return description;
  }
}

export default new Workflow();
