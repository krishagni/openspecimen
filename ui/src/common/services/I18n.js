
class I18n {
  msg(key, args) {
    return window.osI18n.global.t(key, args);
  }
}

export default new I18n();
