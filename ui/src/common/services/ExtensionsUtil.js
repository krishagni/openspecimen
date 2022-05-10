
class ExtensionsUtil {
  createExtensionFieldMap(entity, sdeMode) {
    const extensionDetail = entity.extensionDetail;
    if (!extensionDetail) {
      return;
    }

    extensionDetail.attrsMap = extensionDetail.attrsMap || {};
    Object.assign(extensionDetail.attrsMap, {id: extensionDetail.id, containerId: extensionDetail.formId});
    extensionDetail.attrs.forEach(
      (attr) => {
        if (attr.type == 'datePicker') {
          if (!isNaN(attr.value) && !isNaN(parseInt(attr.value))) {
            attr.value = parseInt(attr.value);
          } else if (!!attr.value || attr.value === 0) {
            attr.value = new Date(attr.value);
          }
        } else if (sdeMode) {
          if (attr.type == 'pvField' || attr.type == 'siteField') {
            attr.value = attr.displayValue;
          }
        }

        if (attr.type != 'subForm') {
          extensionDetail.attrsMap[attr.name] = attr.value;
          extensionDetail.attrsMap['$$' + attr.name + '_displayValue'] = attr.displayValue;
        } else {
          extensionDetail.attrsMap[attr.name] = this._getSubformFieldMap(attr, sdeMode);
        }
      }
    );
  }

  _getSubformFieldMap(sf, sdeMode) {
    const attrsMap = [];
    (sf.value || []).forEach(
      (attrs, idx) => {
        const map = attrsMap[idx] = {};
        attrs.forEach(
          (attr) => {
            if (attr.type == 'datePicker') {
              if (!isNaN(attr.value) && !isNaN(parseInt(attr.value))) {
                attr.value = parseInt(attr.value);
              } else if (!!attr.value || attr.value === 0) {
                attr.value = new Date(attr.value);
              }
            } else if (sdeMode) {
              if (attr.type == 'pvField' || attr.type == 'siteField') {
                attr.value = attr.displayValue;
              }
            }

            map[attr.name] = attr.value;
            map['$$' + attr.name + '_displayValue'] = attr.displayValue;
          }
        );
      }
    );

    return attrsMap;
  }
}

export default new ExtensionsUtil();
