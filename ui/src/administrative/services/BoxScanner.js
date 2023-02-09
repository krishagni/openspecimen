
class BoxScanner {
  types = {};

  addScanner(type, impl) {
    this.types[type] = impl;
  }

  async scan(cfg) {
    const { type } = cfg;
    if (!type) {
      alert('Scanner type not specified');
      return null;
    }

    const scanner = this.types[type];
    if (!scanner) {
      alert('Unknown scanner type: ' + type);
      return null;
    }

    return scanner.scan(cfg);
  }
}

export default new BoxScanner();
