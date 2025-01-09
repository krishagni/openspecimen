
import http from '@/common/services/HttpClient.js';

class PermissibleValue {
  // remap for backward compatibility with older dictionaries.
  attrMap = {
    'clinical-status'     : 'clinical_status',
    'specimen-class'      : 'specimen_type',
    'pathology-status'    : 'pathology_status',
    'collection-procedure': 'collection_procedure',
    'collection-container': 'collection_container',
    'vital-status'        : 'vital_status',
    'received-quality'    : 'receive_quality',
    'anatomic-site'       : 'anatomic_site',
    'site-type'           : 'site_type',
    'clinical-diagnosis'  : 'clinical_diagnosis',
    'specimen-biohazard'  : 'specimen_biohazard',
    'consent_response'    : 'consent_response',
    'missed-visit-reason' : 'missed_visit_reason'
  };

  getAttribute(attribute) {
    return this.attrMap[attribute] || attribute;
  }

  async getPvs(attribute, searchTerm, opts) {
    attribute = this.getAttribute(attribute);
    opts = opts || {};
    const queryParams = {...opts, attribute, searchString: searchTerm};
    if (searchTerm instanceof Array) {
      delete queryParams.searchString;
      queryParams.value = searchTerm;
    }

    return http.get('permissible-values/v', queryParams);
  }
}

export default new PermissibleValue();
