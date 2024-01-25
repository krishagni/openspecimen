
import { format } from 'date-fns';
import ui from '@/global.js';

export default {
  dateTime(date) {
    if (!date) {
      return '-';
    }

    const dt = new Date(date);
    return format(dt, ui.global.locale.dateTimeFmt);
  },

  shortDateTime(date) {
    if (!date) {
      return '-';
    }

    const locale = ui.global.locale;
    return format(new Date(date), locale.shortDateFmt + ' ' + locale.timeFmt);
  },

  date(date) {
    if (!date) {
      return '-';
    }

    return format(new Date(date), ui.global.locale.dateFmt);
  },

  formatDate(date, fmt) {
    if (!date) {
      return '-';
    }

    return format(new Date(date), fmt);
  }
}
