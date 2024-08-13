
import i18n from '@/common/services/I18n.js';
import settingSvc from '@/common/services/Setting.js';

class Alerts {

  toastSvc = undefined;

  info(message, timeout, id) {
    this.add('info', message, timeout, id);
  }

  success(message, timeout, id) {
    this.add('success', message, timeout, id);
  }

  error(message, timeout, id) {
    this.add('error', message, timeout, id);
  }

  warn(message, timeout, id) {
    this.add('warn', message, timeout, id);
  }

  async add(type, message, timeout, id) {
    if (typeof message == 'object') {
      message = i18n.msg(message.code, message.args || {});
    }

    if (!timeout) {
      const [setting] = await settingSvc.getSetting('common', 'toast_disp_time');
      timeout = +setting.value > 0 ? +setting.value * 1000 : 5000;
    }

    this.toastSvc.add({severity: type, detail: message, life: timeout < 0 ? undefined : (timeout || 5000), group: id});
  }

  remove() {
  }

  underDev() {
    this.info('Hold your horses! This function is under development. We sincerely regret the inconvenience caused to you!');
  }
}

export default new Alerts();
