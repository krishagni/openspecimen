
import { format } from 'date-fns';
import sanitizeHtml from "sanitize-html";
import useClipboard from 'vue-clipboard3'

import dateFormatter from '@/common/filters/DateFormatter.js';
import alertSvc from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import i18n from '@/common/services/I18n.js';
import pluginReg from '@/common/services/PluginViewsRegistry.js';
import ui from '@/global.js';

class Util {
  httpsRe = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|])/gim;

  wwwRe   = /(^|[^/])(www\.[\S]+(\b|$))/gim;

  mailRe  = /(([a-zA-Z0-9-_.])+@[a-zA-Z_]+?(\.[a-zA-Z]+)+)/gim;

  toClipboard = useClipboard().toClipboard;

  spmnTypeProps = null;

  specimenTypeUnits = null;

  fileTypes = [
    'bmp', 'csv', 'css', 'doc', 'docx', 'gif', 'html', 'jar', 'java', 'jpeg', 'jpg',
    'js', 'json', 'pdf', 'png', 'tif', 'tiff', 'txt', 'xls', 'xlsx', 'xml', 'zip'
  ];

  mask = null;


  setMask(mask) {
    this.mask = mask;
  }

  enableMask() {
    if (this.mask) {
      this.mask.enable();
    }
  }

  disableMask() {
    if (this.mask) {
      this.mask.disable();
    }
  }

  clone(obj) {
    if (obj == null || typeof obj != 'object') {
      return obj;
    } else if (obj instanceof Date) {
      let copy = new Date();
      copy.setTime(obj.getTime());
      return copy;
    } else if (obj instanceof Array) {
      return obj.map(elem => this.clone(elem));
    } else if (obj instanceof Object) {
      let copy = {};
      for (let prop in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, prop)) {
          copy[prop] = this.clone(obj[prop]);
        }
      }
      return copy;
    }

    throw new Error("Unrecognised object field.");
  }

  queryString(params) {
    return Object.keys(params || {}).sort().reduce(
      (result, param) => {
        if (result) {
          result += '&';
        }

        if (params[param]) {
          result += param + '=' + params[param];
        }

        return result;
      },
      ''
    );
  }

  queryParams(form, ls) {
    const params = {};

    if (!ls.queryParams) {
      return params;
    }

    if (ls.queryParams.static) {
      Object.keys(ls.queryParams.static).forEach(name => params[name] = ls.queryParams.static[name]);
    }

    if (ls.queryParams.dynamic) {
      Object.keys(ls.queryParams.dynamic).forEach(
        (name) => {
          const expr = ls.queryParams.dynamic[name];
          params[name] = exprUtil.eval(form, expr);
        }
      );
    }

    return params;
  }

  uriEncode(params) {
    if (!params || Object.keys(params).length <= 0) {
      return undefined;
    }

    let curatedParams = {};
    for (const [key, value] of Object.entries(params)) {
      if (value || value == 0 || value == false || value == 'false') {
        curatedParams[key] = value;
      }
    }

    let result = undefined;
    if (Object.keys(curatedParams).length > 0) {
      result = btoa(encodeURIComponent(JSON.stringify(curatedParams)));
    }

    return result;
  }

  linkify(text) {
    if (!text || typeof text != 'string') {
      return text;
    }

    return text.replace(this.httpsRe, '<a href="$1" target="_blank">$1</a>')
      .replace(this.wwwRe, '$1<a href="http://$2" target="_blank">$2</a>')
      .replace(this.mailRe, '<a href="mailto:$1">$1</a>');
  }

  getAbsentItems(dstArray, srcArray, keyProp) {
    if (!keyProp) {
      return srcArray;
    }

    const map = dstArray.reduce(
      (acc, obj) => {
        acc[exprUtil.eval(obj, keyProp)] = obj;
        return acc;
      },
      {}
    );

    const result = [];
    for (let obj of srcArray) {
      let key = exprUtil.eval(obj, keyProp);
      if (!map[key]) {
        result.push(obj);
        map[key] = obj;
      }
    }

    return result;
  }

  addIfAbsent(dstArray, srcArray, keyProp) {
    if (!keyProp) {
      return;
    }

    const added = this.getAbsentItems(dstArray, srcArray, keyProp);
    dstArray.push(...added);
    return added;
  }

  validateItems(items, itemLabels, labelProp) {
    // for checking presence of element, using map is faster than list
    const labelsMap = items.reduce(
      (acc, item) => {
        let key = exprUtil.eval(item, labelProp);
        acc[key] = true;
        return acc;
      }, {}
    );

    const found = [], notFound = [];
    itemLabels.forEach(
      (label) => {
        if (labelsMap[label]) {
          found.push(label);
          delete labelsMap[label];
        } else {
          notFound.push(label);
        }
      }
    );

    return { found, notFound, extra: Object.keys(labelsMap) };
  }

  async copyToClipboard(text) {
    return this.toClipboard(text);
  }

  async loadSpecimenTypeProps() {
    const qp = {attribute: 'specimen_type', includeParentValue: true, includeProps: true};
    const resp = await http.get('permissible-values', qp);
    this.spmnTypeProps = resp.reduce(
      (acc, type) => {
        if (type.parentValue) {
          let props = acc[type.parentValue + ':' + type.value] = type.props || {};
          props.specimenClass = type.parentValue;
          props.type = type.value;
        } else {
          let props = acc[type.value + ':*'] = type.props;
          props.specimenClass = type.value;
        }

        return acc;
      },
      {}
    );

    return this.spmnTypeProps;
  }

  async loadSpecimenUnits() {
    const resp = await http.get('specimen-type-units', {maxResults: 1000000});
    this.specimenTypeUnits = resp.reduce(
      (acc, unit) => {
        acc[this._specimenUnitKey(unit)] = unit;
        return acc;
      },
      {}
    );

    return this.specimenTypeUnits;
  }
      
  getSpecimenTypes() {
    const props = this.spmnTypeProps || {};
    return Object.values(props)
      .filter(prop => !!prop.type)
      .map(prop => ({specimenClass: prop.specimenClass, type: prop.type}));
  }

  getSpecimenMeasureUnit({cpShortTitle, specimenClass, type, specimenType}, measure) {
    type = type || specimenType;
    const queries = [
      {cpShortTitle, specimenClass, type},
      {cpShortTitle, specimenClass, type: null},
      {cpShortTitle: null, specimenClass, type},
      {cpShortTitle: null, specimenClass, type: null}
    ];

    let result = '';
    for (let query of queries) {
      const unit = this.specimenTypeUnits[this._specimenUnitKey(query)];
      if (!unit) {
        continue;
      }

      if ((!measure || measure == 'quantity') && unit.quantityUnit) {
        result = unit.quantityUnit;
        break;
      } else if (measure == 'concentration' && unit.concentrationUnit) {
        result = unit.concentrationUnit;
        break;
      }
    }

    return result || '-';
  }

  getUnit(props, measure) {
    alert('Deprecated method is called!');

    let unit = null;

    switch (measure) {
      case 'quantity':
        unit = props.qtyHtmlDisplayCode || props.quantity_display_unit || props.qtyUnit || props.quantity_unit;
        break;

      case 'concentration':
        unit = props.concHtmlDisplayCode || props.concentration_display_unit || props.concUnit || props.concentration_unit;
        break;
    }

    return unit;
  }

  getContainerColorCode({specimenClass, type, specimenType}) {
    type = type || specimenType;
    if (!specimenClass || !type) {
      return {};
    }

    let props = this.spmnTypeProps[specimenClass + ':' + type] || {};
    if (!props['container_color_code']) {
      props = this.spmnTypeProps[specimenClass + ':*'] || {};
    }

    if (props['container_color_code']) {
      try {
        return JSON.parse(props['container_color_code']);
      } catch (e) {
        console.log('Could not parse the following string into JSON: ' + props['container_color_code']);

        const kvList = props['container_color_code'].split(',');
        const style = {};
        kvList.forEach(
          (kv) => {
            const kvPair = kv.split('=');
            if (!kvPair || kvPair.length <= 1) {
              return;
            }

            style[kvPair[0].trim()] = kvPair[1].trim();
          }
        );

        return style;
      }
    }

    return {};
  }

  isBool(value) {
    return value == true || value == false || value == 'true' || value == 'false';
  }

  isFalse(value) {
    return value == false || value == 'false';
  }

  isTrue(value) {
    return value == true || value == 'true';
  }

  async downloadReport(downloadFn, {msgs = {}, filename = 'report.csv'} = {}) {
    msgs = msgs || {};

    alertSvc.info(msgs.initiated || 'Generating the report. Please wait for a moment...');
    let result = await downloadFn();
    if (result.completed) {
      alertSvc.info(msgs.downloading || 'Downloading the report...');

      let extn = filename.substr(filename.lastIndexOf('.') + 1).toLowerCase();
      if (this.fileTypes.indexOf(extn) == -1) {
        // no known extension, by default, append .csv
        filename += '.csv';
      }

      filename = (filename || 'report.csv').replace(/[^\w.]+/g, '_').replace(/__+/g, '_');
      http.downloadFile(http.getUrl('query/export', {query: {fileId: result.dataFile, filename: filename}}));
    } else if (result.dataFile) {
      alertSvc.info(msgs.emailed || 'Report generation is taking more time than anticipated. Report will be emailed');
    }
  }

  formatDate(date, pattern) {
    if (!(date instanceof Date)) {
      return date;
    }

    return format(date, pattern);
  }

  i18n(msg, args) {
    return window.osI18n.global.t(msg, args);
  }

  splitStr(str, re, returnEmpty) {
    const result = [];
    const map = this._getEscapeMap(str);

    let token = '', escUntil = undefined;
    for (let i = 0; i < str.length; ++i) {
      if (escUntil == undefined) {
        escUntil = map[i];
      }

      if (i <= escUntil) {
        token += str[i];
        if (i == escUntil) {
          escUntil = undefined;
        }
      } else {
        if (re.exec(str[i]) == null) {
          token += str[i];
        } else {
          token = this._getToken(token);
          if (token.length > 0 || !!returnEmpty) {
            result.push(token);
          }
          token = '';
        }
      }
    }

    token = this._getToken(token);
    if (token.length > 0) {
      result.push(token);
    }

    return result;
  }

  getDupItems(stringItems) {
    const itemsMap = {}, result = [];
    for (let item of stringItems) {
      let instance = itemsMap[item] || 0;
      if (instance == 1) {
        result.push(item);
      }

      itemsMap[item] = ++instance;
    }

    return result;
  }

  promise(result) {
    if (typeof result == 'function') {
      return new Promise((resolve) => resolve(result()));
    }

    return new Promise((resolve) => resolve(result));
  }

  fns() {
    return this._fns;
  }

  getPluginMenuOptions(menuRef, page, view, ctxt) {
    const options = [];
    return new Promise((resolve) => {
      const pluginOptions = pluginReg.getOptions(page, view) || [];

      let count = pluginOptions.length;
      for (let option of pluginOptions) {
        if (typeof option.showIf == 'function') {
          const ret = option.showIf(ctxt);
          Promise.all([ret]).then(
            ([answer]) => {
              --count;
              if (answer) {
                options.push(this._getPluginMenuOption(menuRef, ctxt, option));
              }

              if (count <= 0) {
                resolve(options);
              }
            }
          );
        } else {
          options.push(this._getPluginMenuOption(menuRef, ctxt, option));
          --count;
        }
      }

      if (count <= 0) {
        resolve(options);
      }
    });
  }

  getFieldDisplayValue(form, field, inputValue, displayType) {
    switch (displayType) {
      case 'storage-position':
        return this._getStorageLocation(inputValue);

      case 'user':
        return this._getUser(inputValue);

      case 'specimen-quantity':
      case 'specimen-measure':
        return this._getSpecimenMeasure(form, inputValue, field, field.measure || 'quantity');

      case 'dateOnly':
        if (inputValue instanceof Date) {
          return this._getDate(this._getLocalDate(inputValue.getTime()));
        } else if (typeof inputValue == 'number') {
          return this._getDate(this._getLocalDate(inputValue));
        } else if (typeof inputValue == 'string' && ('' + parseInt(inputValue)) == inputValue) {
          return this._getDate(this._getLocalDate(parseInt(inputValue)));
        }

        return inputValue;

      case 'date':
      case 'datePicker':
        return this._getDate(inputValue);

      case 'datetime':
        return this._getDate(inputValue, true);

      case 'multiselect':
      case 'pv':
      case 'checkbox':
        if (inputValue instanceof Array) {
          return inputValue.join(', ');
        }
        break;

      case 'subform':
        if (inputValue instanceof Array && inputValue.length > 0) {
          const result = {header: [], columnTypes: [], rows: []};

          for (let sfField of field.fields) {
            result.header.push(sfField.label);
            result.columnTypes.push(sfField.displayType || sfField.type);
          }

          for (let sfRow of inputValue) {
            const row = [];
            for (let sfField of field.fields) {
              row.push(sfRow[sfField.name]);
            }

            result.rows.push(row);
          }

          return result;
        } else {
          return '-';
        }
    }

    if (field.name && field.name.indexOf('.extensionDetail.attrsMap.') >= 0) {
      const displayValue = exprUtil.eval(form, field.name + '$displayValue');
      if (displayValue) {
        return displayValue;
      }
    }

    return inputValue || '-';
  }

  getLocalDate(timeInMillisSinceEpoch) {
    return this._getLocalDate(timeInMillisSinceEpoch);
  }

  getSpecimenDescription(specimen, opts) {
    specimen = specimen || {};
    opts = opts || {};

    const ns = i18n.msg('pvs.not_specified');
    const detailed = opts.detailed == 'true' || opts.detailed == true;

    let result = '';
    if (specimen.lineage == 'New' || detailed) {
      if (specimen.pathology && specimen.pathology != ns) {
        result += specimen.pathology + ' ';
      }

      result += specimen.type;

      if (specimen.specimenClass == 'Tissue' && specimen.anatomicSite && specimen.anatomicSite != ns) {
        result += ' ' + i18n.msg('specimens.extracted_from', {anatomicSite: specimen.anatomicSite});
      }

      if (specimen.specimenClass == 'Fluid' && specimen.collectionContainer && specimen.collectionContainer != ns) {
        result += ' ' + i18n.msg('specimens.collected_in', {container: specimen.collectionContainer});
      }
    } else if (specimen.lineage == 'Derived') {
      result += specimen.lineage + ' ' + specimen.type;
    } else if (specimen.lineage == 'Aliquot') {
      result += specimen.lineage;
    }

    if ((specimen.name || specimen.reqLabel) && specimen.lineage != 'Aliquot') {
      result += ' (' + (specimen.name || specimen.reqLabel) + ')';
    } else if (specimen.code || specimen.reqCode) {
      result += ' (' + (specimen.code || specimen.reqCode) + ')';
    }

    return result;
  }

  toSnakeCase(input) {
    return (input || '').replaceAll(/^\d+|\W+/g, ' ') // convert whitespaces and other special character into a single space
      .replace(/([a-z])([A-Z])/g, '$1 $2') // camelCaseID = camel Case ID
      .trim()
      .split(' ')
      .join('_')
      .toLowerCase();
  }

  sanitizeHtml(content) {
    if (!content) {
      return content;
    }

    return sanitizeHtml(
      content,
      {
        allowedAttributes: {
          "*": ["style"],
          "a": ["href", "target"]
        },
        allowedStyles: {
          "*": {
            "color": [/^#(0x)?[0-9a-f]+$/i, /^rgb\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*\)$/],
            "text-align": [/^left$/, /^right$/, /^center$/],
            "font-size": [/^\d+(?:px|em|%)$/],
            "background-color": [/^#(0x)?[0-9a-f]+$/i, /^rgb\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*\)$/]
          }
        }
      }
    );
  }

  fd(formData, name) {
    let object = formData;
    if (!name) {
      return object;
    }

    const {objName, objCustomFields} = formData;
    if (objName && name.indexOf('$extendedObj') == 0) {
      name = name.replaceAll('$extendedObj', objName);
    }

    let props = name.split('.');
    for (let i = 0; i < props.length; ++i) {
      if (object == null || object == undefined || typeof object != 'object') {
        break;
      }
      object = object[props[i]];
    }

    if ((object == null || object == undefined) && objCustomFields && props.length == 1) {
      props = (objCustomFields + '.' + name).split('.');
      object = formData;

      for (let i = 0; i < props.length; ++i) {
        if (object == null || object == undefined || typeof object != 'object') {
          break;
        }
        object = object[props[i]];
      }
    }

    return object;
  }

  _getEscapeMap(str) {
    let map = {}, insideSgl = false, insideDbl = false;
    let lastIdx = -1;

    for (let i = 0; i < str.length; ++i) {
      if (str[i] == "'" && !insideDbl) {
        if (insideSgl) {
          map[lastIdx] = i;
        } else {
          lastIdx = i;
        }

        insideSgl = !insideSgl;
      } else if (str[i] == '"' && !insideSgl) {
        if (insideDbl) {
          map[lastIdx] = i;
        } else {
          lastIdx = i;
        }

        insideDbl = !insideDbl;
      }
    }

    return map;
  }

  _getToken(token) {
    token = token.trim();
    if (token.length != 0) {
      if ((token[0] == "'" && token[token.length - 1] == "'") ||
        (token[0] == '"' && token[token.length - 1] == '"')) {
        token = token.substring(1, token.length - 1);
      }
    }

    return token;
  }

  _specimenUnitKey({cpShortTitle, specimenClass, type}) {
    return (cpShortTitle || '*') + ':' + specimenClass + ':' + (type || '*');
  }

  _getPluginMenuOption(menuRef, ctxt, option) {
    const url = typeof option.url == 'function' ? option.url(this._wrapRefs(menuRef, ctxt), ctxt) : option.url;
    return {
      icon: option.icon,
      caption: option.caption,
      url: url,
      onSelect: () => {
        if (url) return;
        option.exec(this._wrapRefs(menuRef, ctxt), ctxt)
      }
    };
  }

  _wrapRefs(elRef, ctxt) {
    return {...ctxt, pluginViews: elRef};
  }

  _getStorageLocation(value) {
    let result = undefined;

    if (value && typeof value == 'object' && value.id != -1) {
      let position = value;
      result = position.name;
      if (position.mode == 'TWO_D' && position.positionY && position.positionX) {
        result += ' (' + position.positionY + ', ' + position.positionX + ')';
      } else if (position.mode == 'LINEAR' && position.position > 0) {
        result += ' (' + position.position + ')';
      }
    }

    return result || i18n.msg('specimens.not_stored');
  }

  _getUser(value) {
    let result = value;
    if (value && typeof value == 'object') {
      let user = value;
      result = user.firstName;
      if (user.lastName) {
        if (result) {
          result += ' ';
        }

        result += user.lastName;
      }
    }

    return result || '-';
  }

  _getSpecimenMeasure(form, value, attrs, measure) {
    if (value == null || value == undefined) {
      return '-';
    }

    const specimen = exprUtil.eval(form || {}, attrs.entity || attrs.specimen || 'specimen');
    const unit = this.getSpecimenMeasureUnit(specimen, measure || 'quantity');
    return value + ' ' + unit;
  }

  _getDate(value, showTime) {
    if (value instanceof Date || typeof value == 'number') {
      return showTime ? dateFormatter.dateTime(value) : dateFormatter.date(value);
    }

    return value || '-';
  }

  _getLocalDate(timeInMillisSinceEpoch) {
    const tzOffset = new Date(timeInMillisSinceEpoch).getTimezoneOffset() * 60 * 1000;
    const adjustment = (12 * 60 * 60 * 1000);
    return new Date(timeInMillisSinceEpoch - ui.global.locale.utcOffset + tzOffset + adjustment);
  }

  _fns = {
    set: function(object, expr, value) {
      exprUtil.setValue(object, expr, value)
      return object;
    },

    get: function(object, expr) {
      return exprUtil.eval(object, expr);
    },

    ifnull: function(cond, truth, falsy) {
      return (cond == null || cond == undefined) ? truth : falsy;
    },

    ifNull: function(cond, truth, falsy) {
      return (cond == null || cond == undefined) ? truth : falsy;
    },

    ifNotNull: function(cond, truth, falsy) {
      return (cond !== null && cond !== undefined) ? truth : falsy;
    },

    split: function(inputStr, regex, limit, retElIdx) {
      const result = (inputStr || '' ).split(regex, limit);
      if (typeof retElIdx == 'number') {
        return retElIdx < result.length ? result[retElIdx] : undefined;
      }

      return result;
    },

    join: function(inputStrs, separator) {
      return (inputStrs || []).join(separator);
    },

    concatList: function(list, expr, separator) {
      return (list || []).map(e => exprUtil.eval(e, expr)).filter(e => !!e).join(separator);
    },

    concat: function() {
      var result = '';
      for (var i = 0; i < arguments.length; ++i) {
        if (!arguments[i]) {
          continue;
        }

        if (result.length > 0) {
          result += ' ';
        }

        result += arguments[i];
      }

      return result;
    },

    minValue: function(coll, expr) {
      let minValue = null;
      for (let idx = 0; idx < coll.length; ++idx) {
        const value = exprUtil.eval(coll[idx], expr);
        if (idx == 0 || (value != null && value != undefined && value < minValue)) {
          minValue = value;
        }
      }

      return minValue;
    },

    toDateStr: function(input, fmt) {
      return dateFormatter.formatDate(input, fmt);
    },

    toDateTimeStr: function(input, fmt) {
      return dateFormatter.formatDateTime(input, fmt);
    },

    ageInYears: function(input, today) {
      return this._dateDiffInYears(input, (today && new Date(today)) || new Date());
    },

    now: function() {
      return new Date().getTime();
    },

    currentTime: function() {
      return new Date().getTime();
    },

    dateDiffInYears: function(d1, d2) {
      return this._dateDiffInYears(d1, d2);
    },

    dateDiffInDays: function(d1, d2) {
      return Math.floor(this._dateDiffInMs(d1, d2) / (24 * 60 * 60 * 1000));
    },

    dateDiffInHours: function(d1, d2) {
      return Math.floor(this._dateDiffInMs(d1, d2) / (60 * 60 * 1000));
    },

    dateDiffInMinutes: function(d1, d2, ignoreSeconds) {
      return Math.floor(this._dateDiffInMs(d1, d2, ignoreSeconds) / (60 * 1000));
    },

    dateDiffInSeconds: function(d1, d2) {
      return Math.floor(this._dateDiffInMs(d1, d2) /  1000);
    },

    _toUtc: function(dt, ignSecs) {
      const seconds = ignSecs ? 0 : dt.getSeconds();
      const ms      = ignSecs ? 0 : dt.getMilliseconds();

      return Date.UTC(
        dt.getFullYear(), dt.getMonth(), dt.getDate(),
        dt.getHours(), dt.getMinutes(), seconds, ms
      );
    },

    _dateDiffInMs: function(i1, i2, ignSecs) {
      return this._toUtc(new Date(i2), ignSecs) - this._toUtc(new Date(i1), ignSecs);
    },

    _dateDiffInYears: function(i1, i2) {
      const d1 = new Date(i1);
      const d2 = new Date(i2);
      let diff = d2.getFullYear() - d1.getFullYear();

      const m = d2.getMonth() - d1.getMonth();
      if (m < 0 || (m === 0 && d2.getDate() < d1.getDate())) {
        --diff;
      }

      return diff;
    }
  }
}

export default new Util();
