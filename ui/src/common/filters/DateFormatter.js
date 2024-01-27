
import { format } from 'date-fns';
import ui from '@/global.js';

export default {
  dateTime(date, noValue) {
    if (!date) {
      return noValue || '-';
    }

    const dt = new Date(date);
    return format(dt, ui.global.locale.dateTimeFmt);
  },

  shortDateTime(date, noValue) {
    if (!date) {
      return noValue || '-';
    }

    const locale = ui.global.locale;
    return format(new Date(date), locale.shortDateFmt + ' ' + locale.timeFmt);
  },

  date(date, noValue) {
    if (!date) {
      return noValue || '-';
    }

    return format(new Date(date), ui.global.locale.dateFmt);
  },

  formatDate(date, fmt, noValue) {
    if (!date) {
      return noValue || '-';
    }

    return format(new Date(date), fmt);
  }
}
