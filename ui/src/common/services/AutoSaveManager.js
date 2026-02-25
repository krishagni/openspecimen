const DEBOUNCE_DELAY = 5000;  // 5 seconds of silence

const MAX_WAIT       = 30000; // 30 seconds of max wait before initiating save

export class AutoSaveManager {

  constructor(saveFn) {
    this.saveFn        = saveFn;
    this.autoSaveTimer = null;
    this.maxWaitTimer  = null;
    this.lastSavedData = null;
    this.currentData   = null;

    this.initLifecycleListeners();
  }

  handleInput(data) {
    this.currentData = data;

    clearTimeout(this.autoSaveTimer);
    this.autoSaveTimer = setTimeout(() => this.save(), DEBOUNCE_DELAY);

    if (!this.maxWaitTimer) {
      this.maxWaitTimer = setTimeout(() => this.save(), MAX_WAIT);
    }
  }

  async save() {
    if (JSON.stringify(this.currentData) === JSON.stringify(this.lastSavedData)) {
      this.cleanupTimers();
      return;
    }

    const {alertsSvc, util} = window.osSvc;
    try {
      alertsSvc.info({code: 'common.auto_saving_draft'});
      const toSave = util.clone(this.currentData);
      const {endpoint, method, headers, payload} = this.saveFn(toSave);
      const response = await fetch(endpoint, {method, headers, body: JSON.stringify(payload)});
      if (response.ok) {
        this.lastSavedData = toSave;
        alertsSvc.success({code: 'common.auto_saved_draft'});
      }
    } catch (err) {
      console.error("Auto-save failed:", err);

      let message = err;
      if (typeof err == 'object') {
        message = err.message;
      }

      alertsSvc.error({code: 'common.auto_save_failed', args: {message}});
    } finally {
      this.cleanupTimers();
    }
  }

  cleanupTimers() {
    clearTimeout(this.autoSaveTimer);
    clearTimeout(this.maxWaitTimer);
    this.autoSaveTimer = null;
    this.maxWaitTimer = null;
  }

  // Tab Closure / Visibility Logic
  initLifecycleListeners() {
    document.addEventListener('visibilitychange', this._handleVisibilityChange);
  }

  destroy() {
    this.cleanupTimers();
    document.removeEventListener('visibilitychange', this._handleVisibilityChange);
  }

  // 'hidden' fires when tab is closed, switched, or browser is minimized
  _handleVisibilityChange = () => {
    if (document.visibilityState === 'hidden' && this.currentData) {
      // Check if there are unsaved changes
      if (JSON.stringify(this.currentData) !== JSON.stringify(this.lastSavedData)) {
        const {endpoint, method, headers, payload} = this.saveFn(this.currentData);
        fetch(endpoint, {method, headers, body: JSON.stringify(payload)});
      }
    }
  }
}
