
class PluginLoader {
  dev = true;

  load(name) {
    if (window[name]) {
      return window[name];
    }

    window[name] = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      let url = script.src = 'plugins/' + name + '/' + name + '.umd.js';
      script.async = true;

      script.addEventListener('load', () => {
        alert('loaded: ' + name);
        // console.log(name);
        resolve(window[name]);
        // resolve(eval(name));
      });

      script.addEventListener('error', () => {
        reject(new Error(`Error loading ${url}`));
      });

      document.head.appendChild(script);
    });

    return window[name];
  }
}

export default new PluginLoader();
