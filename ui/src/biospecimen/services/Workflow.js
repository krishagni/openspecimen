import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import i18n  from '@/common/services/I18n.js';

class Workflow {
  osSvc = window.osSvc;

  wfInstanceSvc = window.osSvc.tmWfInstanceSvc;

  async collectVisitSpecimens(visit, repeat) {
    if (!this.wfInstanceSvc) {
      alert('Workflow module not installed!');
      return;
    }

    let wfName = await this._getCollectVisitsWf(visit);
    if (!wfName) {
      wfName = 'sys-collect-visits';
    }

    const {id, cprId, cpId, cpShortTitle, eventId} = visit;
    let inputItem = {cpr: {id: cprId, cpId, cpShortTitle}};
    if (eventId && eventId > 0) {
      inputItem.cpe = {id: eventId, cpId, cpShortTitle};
    }

    let inputType = undefined;
    if (id > 0) {
      inputItem.visit = {id, cpId, cpShortTitle};
      inputType = 'visit';
    }

    const params = {repeatVisit: repeat, ...this._getVisitBreadcrumb(visit, this._getVisitDescription(visit))};
    const opts = {inputType, params};
    const instance = await this.wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
    this.wfInstanceSvc.gotoInstance(instance.id);
  }

  async collectPending(visit) {
    if (!this.wfInstanceSvc) {
      alert('Workflow module not installed!');
      return;
    }

    let wfName = await this._getCollectPendingSpmnsWf(visit);
    if (!wfName) {
      wfName = 'sys-collect-pending-specimens';
    }

    const {cprId, cpId, cpShortTitle, id, eventId} = visit;
    let inputItem = {
      cpr:   {id: cprId, cpId, cpShortTitle},
      visit: {id,        cpId, cpShortTitle}
    };

    if (eventId && eventId > 0) {
      inputItem.cpe = {id: eventId, cpId, cpShortTitle};
    }

    const opts = {params: this._getVisitBreadcrumb(visit, this.osSvc.i18nSvc.msg('participants.collect_specimens'))};
    const instance = await this.wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
    this.wfInstanceSvc.gotoInstance(instance.id);
  }

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

  async createAliquots(specimen) {
    let wfName = await this._getCreateAliquotsWf(specimen);
    if (!wfName) {
      wfName = 'sys-create-adhoc-aliquots';
    }

    this._createChildSpecimens(specimen, wfName, i18n.msg('specimens.create_aliquots'));
  }

  async createDerivedSpecimens(specimen) {
    let wfName = await this._getCreateDerivativesWf(specimen);
    if (!wfName) {
      wfName = 'sys-create-adhoc-derivatives';
    }

    this._createChildSpecimens(specimen, wfName, i18n.msg('specimens.create_derived'));
  }

  async _createChildSpecimens(specimen, wfName, title) {
    if (!this.wfInstanceSvc) {
      alert('Workflow module not installed!');
      return;
    }

    const inputItem = {
      cpr  :    {id: specimen.cprId,   cpId: specimen.cpId, cpShortTitle: specimen.cpShortTitle},
      visit:    {id: specimen.visitId, cpId: specimen.cpId, cpShortTitle: specimen.cpShortTitle},
      specimen: {id: specimen.id,      cpId: specimen.cpId, cpShortTitle: specimen.cpShortTitle}
    };

    const {cpId, cprId, visitId, id} = specimen;
    const params = this._getVisitBreadcrumb(specimen, title);
    params['breadcrumb-3'] = JSON.stringify({
      label: this._getVisitDescription(specimen),
      route: {
        name: 'ParticipantsListItemVisitDetail.Overview',
        params: {cpId, cprId, visitId}
      }
    });
    params['breadcrumb-4'] = JSON.stringify({
      label: specimen.label + (specimen.barcode ? ' (' + specimen.barcode + ')' : ''),
      route: {
        name: 'ParticipantsListItemSpecimenDetail.Overview',
        params: {cpId, cprId, visitId, specimenId: id}
      }
    });

    const instance = await this.wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], {params});
    this.wfInstanceSvc.gotoInstance(instance.id);
  }

  _getCollectVisitsWf({cpId}) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'collectVisitsWf');
  }

  _getCollectPendingSpmnsWf({cpId}) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'collectPendingSpecimensWf');
  }

  _getCollectUnplannedSpmnsWf({cpId}) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'collectUnplannedSpecimensWf');
  }

  _getCreateAliquotsWf({cpId}) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'createAliquotsWf');
  }

  _getCreateDerivativesWf({cpId}) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'createDerivativesWf');
  }

  _getVisitBreadcrumb({cpId, cpShortTitle, cprId, ppid}, title) {
    return {
      returnOnExit: 'current_view',
      cpId: cpId,
      'breadcrumb-1': JSON.stringify({
        label: cpShortTitle,
        route: {name: 'ParticipantsList', params: {cpId, cprId: -1}}
      }),
      'breadcrumb-2': JSON.stringify({
        label: ppid,
        route: {name: 'ParticipantsListItemDetail.Overview', params: {cpId, cprId}}
      }),
      batchTitle: title,
      showOptions: false
    };
  }

  _getVisitDescription({description, visitDescription, eventLabel}) {
    description = description || visitDescription || eventLabel || 'Unknown';
    let idx = description.indexOf(' / ');
    if (idx >= 0) {
      description = description.substring(0, idx);
    }

    return description;
  }
}

export default new Workflow();