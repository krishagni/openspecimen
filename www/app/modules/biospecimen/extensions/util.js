
angular.module('os.biospecimen.extensions.util', [])
  .factory('ExtensionsUtil', function($modal, $parse, Form, Alerts, ApiUrls) {
    var filesUrl = ApiUrls.getBaseUrl() + 'form-files';

    function getFileDownloadUrl(formId, recordId, ctrlName, fileId) {
      var params = '?formId=' + formId +
        '&ctrlName=' + ctrlName +
        '&_reqTime=' + new Date().getTime();

      if (fileId) {
        params += '&fileId=' + fileId;
      } else {
        params += '&recordId=' + recordId;
      }

      return filesUrl + params;
    }

    function deleteRecord(record, onDeletion) {
      var modalInstance = $modal.open({
        templateUrl: 'modules/biospecimen/extensions/delete-record.html',
        controller: function($scope, $modalInstance) {
          $scope.record= record;

          $scope.yes = function() {
            Form.deleteRecord(record.formId, record.recordId).then(
              function(result) {
                $modalInstance.close();
                Alerts.success('extensions.record_deleted');
              },

              function() {
                $modalInstance.dismiss('cancel');
              }
            );
          }

          $scope.no = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });

      modalInstance.result.then(
        function() {
          if (typeof onDeletion == 'function') {
            onDeletion(record);
          }
        }
      );
    };
    
    function createExtensionFieldMap(entity, sdeMode) {
      var extensionDetail = entity.extensionDetail;
      if (!extensionDetail) {
        return;
      }

      extensionDetail.attrsMap = extensionDetail.attrsMap || {};
      angular.extend(extensionDetail.attrsMap, {id: extensionDetail.id, containerId: extensionDetail.formId});

      angular.forEach(extensionDetail.attrs, function(attr) {
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

        extensionDetail.attrsMap[attr.name] = attr.type != 'subForm' ? attr.value : getSubformFieldMap(attr, sdeMode);
        if (attr.type != 'subForm') {
          extensionDetail.attrsMap['$$' + attr.name + '_displayValue'] = attr.displayValue;
        }
      });
    }

    function getSubformFieldMap(sf, sdeMode) {
      var attrsMap = [];
      angular.forEach(sf.value, function(attrs, idx) {
        var map = attrsMap[idx] = {};
        angular.forEach(attrs, function(attr) {
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
        });
      })

      return attrsMap; 
    }

    function getExtnOpts(entity, extnCtxt, disableFields) {
      if (!extnCtxt) {
        return undefined;
      }

      createExtensionFieldMap(entity, extnCtxt.sdeMode);

      return {
        formId: extnCtxt.formId,
        recordId: !!entity.extensionDetail ? entity.extensionDetail.id : undefined,
        formCtxtId: parseInt(extnCtxt.formCtxtId),
        objectId: entity.id,
        formData: !!entity.extensionDetail ? (entity.extensionDetail.attrsMap || {}): {},
        showActionBtns: false,
        showPanel: false, 
        labelAlignment: 'horizontal',
        disableFields: disableFields || []
      };
    }

    function getMatchingForms(forms, rules, ctxt) {
      var matchingRule = null;
      for (var i = 0; i < rules.length; ++i) {
        if (!rules[i].when) {
          continue;
        }

        try {
          if ($parse(rules[i].when)(ctxt)) {
            matchingRule = rules[i];
            break;
          }
        } catch (err) {
          alert('Invalid rule expression: ' + (i + 1) + ': ' + err);
        }
      }

      if (matchingRule && matchingRule.forms) {
        forms = forms.filter(function(f) { return matchingRule.forms.indexOf(f.formName) > -1; });
      }

      return forms;
    }

    function sortForms(inputForms, orderSpec) {
      if (!orderSpec || orderSpec.length == 0) {
        return inputForms;
      }

      var formsByType = {};
      angular.forEach(inputForms,
        function(form) {
          if (!formsByType[form.entityType]) {
            formsByType[form.entityType] = [];
          }

          formsByType[form.entityType].push(form);
        }
      );

      var result = [];
      angular.forEach(orderSpec,
        function(typeForms) {
          Array.prototype.push.apply(result, sortForms0(formsByType[typeForms.type], typeForms.forms));
          delete formsByType[typeForms.type];
        }
      );

      angular.forEach(inputForms,
        function(form) {
          if (formsByType[form.entityType]) {
            result.push(form);
          }
        }
      );

      return result;
    }

    function sortForms0(inputForms, orderSpec) {
      var formsById = {};
      angular.forEach(inputForms,
        function(form) {
          formsById[form.formId] = form;
        }
      );

      var result = [];
      angular.forEach(orderSpec,
        function(spec) {
          var form = formsById[spec.id];
          if (form) {
            result.push(form);
            inputForms.splice(inputForms.indexOf(form), 1);
          }
        }
      );

      Array.prototype.push.apply(result, inputForms);
      return result;
    }

    function linkFormRecords(inputForms, records) {
      var recsMap = {};
      angular.forEach(records,
        function(rec) {
          if (!recsMap[rec.fcId]) {
            recsMap[rec.fcId] = [];
          }
          recsMap[rec.fcId].push(rec);
        }
      );

      angular.forEach(inputForms,
        function(form) {
          form.records = recsMap[form.formCtxtId] || [];
        }
      );
    }

    //
    // converts the DE form fields metadata to SDE metadata
    //
    function toSdeFields(prefix, formId, formDef) {
      return toSdeFields0(prefix, formId, '', formDef);
    }

    function toSdeFields0(prefix, formId, sfPrefix, formDef) {
      var fields = [];
      for (var r = 0; r < formDef.rows.length; ++r) {
        for (var c = 0; c < formDef.rows[r].length; ++c) {
          var sdeField = toSdeField(prefix, formId, sfPrefix, formDef.rows[r][c]);
          if (sdeField) {
            fields.push(sdeField);
          }
        }
      }

      return fields;
    }

    function toSdeField(prefix, formId, sfPrefix, deField) {
      var sdeField = {
        name: prefix ? prefix + '.' + deField.name : deField.name,
        caption: deField.caption,
        tooltip: deField.tooltip || deField.toolTip,
        optional: (deField.validationRules || []).every(function(r) { return r.name != 'required'; })
      };

      if (deField.type == 'stringTextField') {
        sdeField.type = 'text';
        sdeField.defaultValue = deField.defaultValue;
      } else if (deField.type == 'numberField') {
        sdeField.type = 'text';
        sdeField.defaultValue = deField.defaultValue;
        if (deField.noOfDigitsAfterDecimal) {
          sdeField.pattern = '/^([0-9]+|[0-9]*\\.?[0-9]+[e]?[+-]?[0-9]*)$/';
        } else {
          sdeField.pattern = '/^[0-9]+$/';
        }
      } else if (deField.type == 'textArea') {
        sdeField.type = 'textarea';
        sdeField.defaultValue = deField.defaultValue;
      } else if (deField.type == 'radiobutton') {
        sdeField.type = 'radio';
        sdeField.options = deField.pvs.map(function(pv) { return pv.value; });
        sdeField.defaultValue = deField.defaultValue && deField.defaultValue.value;
      } else if (deField.type == 'booleanCheckbox') {
        sdeField.type = 'toggle-checkbox';
        if (deField.defaultChecked) {
          sdeField.defaultValue = true;
        }
      } else if (deField.type == 'combobox' || deField.type == 'multiSelectListbox' || deField.type == 'checkbox') {
        sdeField.type = 'dropdown';
        sdeField.multiple = (deField.type != 'combobox');
        sdeField.listSource = {
          selectProp: 'value',
          displayProp: 'value',
          apiUrl: 'forms/' + formId + '/permissible-values',
          queryParams: {
            static: {
              controlName: sfPrefix ? sfPrefix + '.' + deField.name : deField.name
            }
          }
        };

        if (deField.defaultValue) {
          var value = deField.defaultValue.value;
          sdeField.defaultValue = (deField.type == 'combobox') ? value : [value];
        }
      } else if (deField.type == 'datePicker') {
        sdeField.type = deField.format && deField.format.indexOf('HH:mm') != -1 ? 'datetime' : 'date';
        if (deField.defaultType == 'CURRENT_DATE') {
          sdeField.defaultValue = 'current_date';
        }
      } else if (deField.type == 'userField') {
        sdeField.type = 'user';
        sdeField.selectProp = 'id';
        sdeField.defaultValue = 'current_user';
      } else if (deField.type == 'siteField') {
        sdeField.type = 'dropdown';
        sdeField.listSource = {
          selectProp: 'name',
          displayProp: 'name',
          queryParams : {
            search: 'name'
          },
          apiUrl: 'sites'
        };
      } else if (deField.type == 'pvField') {
        sdeField.type = 'pvs';
        sdeField.attr = deField.attribute;
        sdeField.showOnlyLeafValues = deField.leafNode;
      } else if (deField.type == 'fileUpload') {
        sdeField.type = 'file';
      } else if (deField.type == 'subForm') {
        sdeField.type = 'collection';
        sfPrefix = sfPrefix ? sfPrefix + '.' + deField.name : deField.name;
        sdeField.fields = toSdeFields0('', formId, sfPrefix, deField);
        if (!sdeField.fields || sdeField.fields.length == 0) {
          sdeField = null;
        }
      } else if (deField.type == 'label') {
        sdeField.type = 'note';
        sdeField.note = deField.heading || deField.caption;
        sdeField.caption = '';
      } else {
        sdeField = null;
      }

      return sdeField;
    }

    var viewTmpls = {};

    return {
      getFileDownloadUrl: getFileDownloadUrl,

      deleteRecord: deleteRecord,

      createExtensionFieldMap: createExtensionFieldMap,

      getExtnOpts: getExtnOpts,

      getMatchingForms: getMatchingForms,

      sortForms: sortForms,

      linkFormRecords: linkFormRecords,

      registerView: function(name, viewTmpl) {
        viewTmpls[name] = viewTmpl;
      },

      getViewTmpl: function(name) {
        return viewTmpls[name];
      },

      toSdeFields: toSdeFields
    }
 
  });
