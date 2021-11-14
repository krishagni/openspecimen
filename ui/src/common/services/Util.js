
import useClipboard from 'vue-clipboard3'

import alertSvc from '@/common/services/Alerts.js';
import http from '@/common/services/HttpClient.js';
import exprUtil from '@/common/services/ExpressionUtil.js';


class Util {
  httpsRe = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|])/gim;

  wwwRe   = /(^|[^/])(www\.[\S]+(\b|$))/gim;

  mailRe  = /(([a-zA-Z0-9-_.])+@[a-zA-Z_]+?(\.[a-zA-Z]{2,6})+)/gim;

  toClipboard = useClipboard().toClipboard;

  spmnTypeProps = null;

  fileTypes = [
    'bmp', 'csv', 'css', 'doc', 'docx', 'gif', 'html', 'jar', 'java', 'jpeg', 'jpg',
    'js', 'json', 'pdf', 'png', 'tif', 'tiff', 'txt', 'xls', 'xlsx', 'xml', 'zip'
  ];

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
    if (!text) {
      return text;
    }

    return text.replace(this.httpsRe, '<a href="$1" target="_blank">$1</a>')
      .replace(this.wwwRe, '$1<a href="http://$2" target="_blank">$2</a>')
      .replace(this.mailRe, '<a href="mailto:$1">$1</a>');
  }

  addIfAbsent(dstArray, srcArray, keyProp) {
    if (!keyProp) {
      return;
    }

    const map = {};
    dstArray.forEach(obj => map[exprUtil.eval(obj, keyProp)] = obj);

    const added = [];
    srcArray.forEach(
      obj => {
        let key = exprUtil.eval(obj, keyProp);
        if (!map[key]) {
          dstArray.push(obj);
          map[key] = obj;
          added.push(obj);
        }
      }
    );

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
    let resp = await http.get('specimen-type-props');
    this.spmnTypeProps = resp.reduce(
      (acc, typeProps) => {
        acc[typeProps.specimenClass + ':' + typeProps.specimenType] = typeProps.props;
        return acc;
      },
      {}
    );

    return this.spmnTypeProps;
  }

  getSpecimenMeasureUnit({specimenClass, type}, measure) {
    if (!specimenClass || !type) {
      return '-';
    }

    let props = this.spmnTypeProps[specimenClass + ':' + type];
    switch (measure) {
      case 'quantity':
        return props.qtyHtmlDisplayCode || props.qtyUnit;

      case 'concentration':
        return props.concHtmlDisplayCode || props.concUnit;
    }

    return '-';
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

      http.downloadFile(http.getUrl('query/export', {query: {fileId: result.dataFile, filename: filename}}));
    } else if (result.dataFile) {
      alertSvc.info(msgs.emailed || 'Report generation is taking more time than anticipated. Report will be emailed');
    }
  }
}

export default new Util();
