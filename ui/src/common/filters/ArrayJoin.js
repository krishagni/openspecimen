
export default {
  join(value) {
    if (value instanceof Array) {
      return value.join(', ');
    } else if (!value) {
      return '-';
    } else {
      return value;
    }
  }
}
