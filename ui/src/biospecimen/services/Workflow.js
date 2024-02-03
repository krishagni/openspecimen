import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import i18n  from '@/common/services/I18n.js';

class Workflow {
  async collectVisitSpecimens(visit, repeat) {
    if (!this._wfInstanceSvc()) {
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
    const instance = await this._wfInstanceSvc().createInstance({name: wfName}, null, null, null, [inputItem], opts);
    this._wfInstanceSvc().gotoInstance(instance.id);
  }

  async collectPending(visit) {
    if (!this._wfInstanceSvc()) {
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

    const opts = {params: this._getVisitBreadcrumb(visit, i18n.msg('participants.collect_specimens'))};
    const instance = await this._wfInstanceSvc().createInstance({name: wfName}, null, null, null, [inputItem], opts);
    this._wfInstanceSvc().gotoInstance(instance.id);
  }

  async addSpecimen(visit) {
    if (!this._wfInstanceSvc()) {
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
    const instance = await this._wfInstanceSvc().createInstance({name: wfName}, null, null, null, [inputItem], opts);
    this._wfInstanceSvc().gotoInstance(instance.id);
  }

  async createAliquots(specimens) {
    let {cpId, cpShortTitle} = specimens[0];
    for (let specimen of specimens) {
      if (specimen.cpId != cpId) {
        cpId = -1;
        cpShortTitle = null;
        break;
      }
    }

    let wfName = await this._getCreateAliquotsWf(cpId);
    if (!wfName) {
      wfName = 'sys-create-adhoc-aliquots';
    }

    this._createChildSpecimens(cpId, cpShortTitle, specimens, wfName, i18n.msg('specimens.create_aliquots'));
  }

  async createDerivedSpecimens(specimens) {
    let {cpId, cpShortTitle} = specimens[0];
    for (let specimen of specimens) {
      if (specimen.cpId != cpId) {
        cpId = -1;
        cpShortTitle = null;
        break;
      }
    }

    let wfName = await this._getCreateDerivativesWf(cpId);
    if (!wfName) {
      wfName = 'sys-create-adhoc-derivatives';
    }

    this._createChildSpecimens(cpId, cpShortTitle, specimens, wfName, i18n.msg('specimens.create_derived'));
  }

  async createPooledSpecimens(specimens) {
    if (!this._wfInstanceSvc()) {
      alert('Workflow module not installed!');
      return;
    }

    let cpId = specimens && specimens[0].cpId;
    for (let specimen of specimens) {
      if (cpId != specimen.cpId) {
        cpId = -1;
        break;
      }
    }

    let wfName = await this._getPooledSpmnWf(cpId);
    if (!wfName) {
      wfName = 'sys-create-pooled-specimen';
    }

    const params = {
      returnOnExit: 'current_view',
      batchTitle: i18n.msg('specimens.create_pooled_specimen'),
      showOptions: false
    };

    if (cpId >= 1) {
      Object.assign(params, {
        cpId: cpId,
        'breadcrumb-1': JSON.stringify({
          label: specimens[0].cpShortTitle,
          route: {name: 'ParticipantsList', params: {cpId, cprId: -1}}
        })
      });
    }

    const inputItems = specimens.map(specimen => ({specimen}));
    const instance = await this._wfInstanceSvc().createInstance({name: wfName}, null, null, null, inputItems, {params});
    this._wfInstanceSvc().gotoInstance(instance.id);
  }

  async transferSpecimens(specimens) {
    if (!this._wfInstanceSvc()) {
      alert('Workflow module not installed!');
      return;
    }

    let cpId = specimens && specimens[0].cpId;
    for (let specimen of specimens) {
      if (cpId != specimen.cpId) {
        cpId = -1;
        break;
      }
    }

    let wfName = await this._getTransferSpmnsWf(cpId);
    if (!wfName) {
      wfName = 'sys-transfer-specimens';
    }

    const params = {
      returnOnExit: 'current_view',
      batchTitle: i18n.msg('specimens.transfer_specimens'),
      showOptions: false
    };

    if (cpId >= 1) {
      Object.assign(params, {
        cpId: cpId,
        'breadcrumb-1': JSON.stringify({
          label: specimens[0].cpShortTitle,
          route: {name: 'ParticipantsList', params: {cpId, cprId: -1}}
        })
      });
    }

    const inputItems = specimens.map(specimen => ({specimen}));
    const instance = await this._wfInstanceSvc().createInstance({name: wfName}, null, null, null, inputItems, {params});
    this._wfInstanceSvc().gotoInstance(instance.id);
  }

  async _createChildSpecimens(cpId, cpShortTitle, specimens, wfName, title) {
    if (!this._wfInstanceSvc()) {
      alert('Workflow module not installed!');
      return;
    }

    const inputItems = specimens.map(
      specimen => ({
        cpr  :    {id: specimen.cprId,   cpId: specimen.cpId, cpShortTitle: specimen.cpShortTitle},
        visit:    {id: specimen.visitId, cpId: specimen.cpId, cpShortTitle: specimen.cpShortTitle},
        specimen: {id: specimen.id,      cpId: specimen.cpId, cpShortTitle: specimen.cpShortTitle}
      })
    );

    let params = {}
    if (inputItems.length > 1) {
      params = this._getCpBreadcrumb(cpId, cpShortTitle, title);
    } else if (inputItems.length == 1) {
      const specimen = specimens[0];
      const {cpId, cprId, visitId, id} = specimen;
      params = this._getVisitBreadcrumb(specimen, title);
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
    }

    const instance = await this._wfInstanceSvc().createInstance({name: wfName}, null, null, null, inputItems, {params});
    this._wfInstanceSvc().gotoInstance(instance.id);
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

  _getPooledSpmnWf(cpId) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'createPooledSpecimensWf');
  }

  _getTransferSpmnsWf(cpId) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'transferSpecimensWf');
  }

  _getCreateAliquotsWf(cpId) {
    return cpSvc.getWorkflowProperty(cpId, 'common', 'createAliquotsWf');
  }

  _getCreateDerivativesWf(cpId) {
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

  _getCpBreadcrumb(cpId, cpShortTitle, title) {
    const params = {
      returnOnExit: 'current_view',
      cpId: cpId,
      batchTitle: title,
      showOptions: false
    };

    if (cpId > 0) {
      params['breadcrumb-1'] = JSON.stringify({
        label: cpShortTitle,
        route: {name: 'ParticipantsList', params: {cpId, cprId: -1}}
      })
    }

    return params;
  }

  _getVisitDescription({description, visitDescription, eventLabel}) {
    description = description || visitDescription || eventLabel || 'Unknown';
    let idx = description.indexOf(' / ');
    if (idx >= 0) {
      description = description.substring(0, idx);
    }

    return description;
  }

  _wfInstanceSvc() {
    return window.osSvc.tmWfInstanceSvc;
  }
}

export default new Workflow();
