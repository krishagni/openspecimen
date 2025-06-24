
import http from '@/common/services/HttpClient.js';
import routerSvc from '@/common/services/Router.js';

class QuickSearch {
  searchEntities = {
    'collection_protocol': {
      caption: 'Collection Protocol',
      ngView: false,
      viewName: 'CpDetail.Overview',
      params: {entityId: 'cpId'}
    },

    'collection_protocol_registration': {
      caption: 'Participant',
      ngView: false,
      viewName: 'CprResolver',
      params: {entityId: 'cprId'}
    },

    'visit': {
      caption: 'Visit',
      ngView: false,
      viewName: 'VisitResolver',
      params: {entityId: 'visitId'}
    },

    'specimen': {
      caption: 'Specimen',
      ngView: false,
      viewName: 'SpecimenResolver',
      params: {entityId: 'specimenId'}
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
      ngView: false,
      viewName: 'CartSpecimensList',
      params: {entityId: 'cartId'}
    },
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
