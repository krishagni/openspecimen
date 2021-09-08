
class PluginLoader {

  load(name) {
    let lookupName = 'os-plugins.' + name;
    if (window[lookupName]) {
      return window[lookupName];
    }

    window[lookupName] = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      let url = script.src = 'plugins/' + name + '/' + name + '.umd.js';
      script.async = true;

      script.addEventListener('load', () => {
        resolve(window[name]);
      });

      script.addEventListener('error', () => {
        reject(new Error(`Error loading ${url}`));
      });

      document.head.appendChild(script);
    });

    return window[lookupName];
  }
}

export default new PluginLoader();
