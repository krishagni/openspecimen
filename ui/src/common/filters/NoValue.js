
export default {
  noValue(value, marker) {
    if (value == null || value == undefined || value == '') {
      return marker || '-';
    }

    return value;
  }
}
