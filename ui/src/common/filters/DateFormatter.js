
import { format } from 'date-fns';
import ui from '@/global.js';

const _format = function(date, fmt, noValue) {
  if (!date) {
    return noValue || '-';
  }

  try {
    return format(new Date(date), fmt);
  } catch (e) {
    console.log('Error converting ' + date + ' (' + (typeof date) + ') to date object: ' + e);
    return date;
  }
};

export default {
  dateTime(date, noValue) {
    return _format(date, ui.global.locale.dateTimeFmt, noValue);
  },

  shortDateTime(date, noValue) {
    const {shortDateFmt, timeFmt} = ui.global.locale;
    return _format(date, shortDateFmt + ' ' + timeFmt, noValue);
  },

  date(date, noValue) {
    return _format(date, ui.global.locale.dateFmt, noValue);
  },

  formatDate(date, fmt, noValue) {
    return _format(date, fmt || ui.global.locale.dateFmt, noValue);
  },

  formatDateTime(date, fmt, noValue) {
    return format(date, fmt || ui.global.locale.dateTimeFmt, noValue);
  }
}
