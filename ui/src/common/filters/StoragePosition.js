
export default {
  toString(location) {
    if (!location || !location.name) {
      return null;
    }

    let result = location.name;
    if (location.mode == 'TWO_D') {
      if (location.positionY && location.positionX) {
        result += ' (' + location.positionY + ', ' + location.positionX + ')';
      }
    } else if (location.mode == 'LINEAR') {
      if (location.position) {
        result += ' (' + location.position + ')';
      }
    }

    return result;
  }
}
