

export default {
  dateTime(date) {
    if (!date) {
      return '-';
    }

    var dt = new Date(date);
    return dt.toLocaleDateString() + ' ' + dt.toLocaleTimeString();
  },

  date(date) {
    if (!date) {
      return '-';
    }

    return new Date(date).toLocaleDateString();
  }
}
