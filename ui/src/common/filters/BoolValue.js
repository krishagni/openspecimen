
export default {
  toString(value, cfg) {
    cfg = cfg || {};
    if (value == 1 || value == true || value == 'true') {
      return cfg[true] || 'Yes';
    } else if (value == 0 || value == false || value == 'false') {
      return cfg[false] || 'No';
    } else {
      return '-';
    }
  }
}
