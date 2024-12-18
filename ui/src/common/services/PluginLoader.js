
import ui from '@/global.js';

class PluginLoader {

  load(name) {
    let lookupName = 'os-plugins.' + name;
    if (window[lookupName]) {
      return window[lookupName];
    }

    if (ui.localPlugins) {
      window[lookupName] = import('@/plugins/' + name);
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

      const style = document.createElement('link');
      style.setAttribute('rel', 'stylesheet');
      style.href = 'plugins/' + name + '/' + name + '.css';
      style.async = true;

      style.addEventListener('error', () => {
        console.warn('No CSS for plugin: ' + name);
      });

      document.head.appendChild(style);
    });

    return window[lookupName];
  }
}

export default new PluginLoader();
