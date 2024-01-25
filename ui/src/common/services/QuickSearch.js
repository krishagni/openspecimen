
import ui   from '@/global.js';
import http from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

class QuickSearch {
  searchEntities = {
    'collection_protocol': {
      caption: 'Collection Protocol',
      ngView: true,
      url: ui.ngServer + '#/object-state-params-resolver?' +
        'stateName=cp-detail.overview&' +
        'objectName=collection_protocol&' +
        'key=id&' +
        'value=:entityId',
    },

    'collection_protocol_registration': {
      caption: 'Participant',
      ngView: true,
      url: ui.ngServer + '#/object-state-params-resolver?' +
           'stateName=participant-detail.overview&' +
           'objectName=collection_protocol_registration&' +
           'vueView=ParticipantsListItemDetail.Overview&' +
           'key=id&' +
           'value=:entityId',
    },

    'visit': {
      caption: 'Visit',
      ngView: true,
      url: ui.ngServer + '#/object-state-params-resolver?' +
           'stateName=visit-detail.overview&' +
           'objectName=visit&' +
           'key=id&' +
           'value=:entityId',
    },

    'specimen': {
      caption: 'Specimen',
      ngView: true,
      url: ui.ngServer + '#/object-state-params-resolver?' +
           'stateName=specimen-detail.overview&' +
           'objectName=specimen&' +
           'key=id&' +
           'value=:entityId',
    },

    'institute': {
      caption: 'Institute',
      ngView: false,
      viewName: 'InstituteDetail.Overview',
      params: {entityId: 'instituteId'}
    },

    'user': {
      caption: 'User',
      ngView: false,
      viewName: 'UserDetail.Overview',
      params: {entityId: 'userId'}
    },

    'storage_container': {
      caption: 'Container',
      ngView: false,
      viewName: 'ContainerDetail.Locations',
      params: {entityId: 'containerId'}
    },

    'site': {
      caption: 'Site',
      ngView: false,
      viewName: 'SiteDetail.Overview',
      params: {entityId: 'siteId'}
    },

    'distribution_protocol': {
      caption: 'Distribution Protocol',
      ngView: false,
      viewName: 'DpDetail.Overview',
      params: {entityId: 'dpId'}
    },

    'distribution_order': {
      caption: 'Order',
      ngView: false,
      viewName: 'OrderDetail.Overview',
      params: {entityId: 'orderId'}
    },

    'shipment': {
      caption: 'Shipment',
      ngView: false,
      viewName: 'ShipmentDetail.Overview',
      params: {entityId: 'shipmentId'}
    },

    'specimen_list': {
      caption: 'Cart',
      ngView: true,
      url: ui.ngServer + '#/object-state-params-resolver?' +
           'stateName=specimen-list&' +
           'objectName=specimen_list&' +
           'key=id&' +
           'value=:entityId',
    },

    /* TODO: Need to move to supply plugin */
    'supply': {
      caption: 'Supply',
      ngView: true,
      url: ui.ngServer + '#/object-state-params-resolver?' +
           'stateName=supplies-detail.overview&' +
           'objectName=supply&' +
           'key=id&' +
           'value=:entityId',
    },

    'supply_type': {
      caption: 'Supply Type',
      ngView: true,
      url: ui.ngServer + '#/object-state-params-resolver?' +
           'stateName=supplies-type-detail.overview&' +
           'objectName=supply_type&' +
           'key=id&' +
           'value=:entityId',
    }
  };

  async search(term) {
    let termRegEx = new RegExp(term, 'ig');
    return http.get('search', {term: term}).then(
      (results) => {
        results.forEach(match => this.processMatch(termRegEx, match));
        if (results.length == 0) {
          results.push({
            id: -1,
            key: term,
            caption: 'Search user manual for \'<span class="os-match-highlight">' + term + '</span>\''
          });
        }

        return results;
      }
    );
  }

  processMatch(termRegEx, match) {
    let searchEntity   = this.searchEntities[match.entity];
    match.searchEntity = searchEntity;
    match.category     = (searchEntity && searchEntity.caption) || match.entity;
    match.matchedText  = match.value.replace(termRegEx, (matchedText) => this.highlightMatch(matchedText));
    match.props        = Object.values(match.entityProps || []).join(', ');

    if (searchEntity && searchEntity.url) {
      match.url = searchEntity.url.replace(':entityId', match.entityId);
    } else if (searchEntity && !searchEntity.ngView) {
      let params = {};
      if (searchEntity.params && searchEntity.params.entityId) {
        params[searchEntity.params.entityId] = match.entityId;
      }

      match.url = routerSvc.getUrl(searchEntity.viewName, params);
      match.route = {name: searchEntity.viewName, params: params, query: {}};
    }
  }

  highlightMatch(matchedText) {
    return '<span class="os-match-highlight">' + matchedText + '</span>';
  }
}

export default new QuickSearch();
