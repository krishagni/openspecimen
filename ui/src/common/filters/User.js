
export default {
  name(user) {
    if (!user) {
      return '-';
    }

    let result = '';
    if (user.firstName) {
      result = user.firstName;
    }

    if (user.lastName) {
      if (result) {
        result += ' ';
      }

      result += user.lastName;
    }

    return result || '-';
  }
}
