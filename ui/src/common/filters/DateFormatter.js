
import { format } from 'date-fns';
import ui from '@/global.js';

export default {
  dateTime(date) {
    if (!date) {
      return '-';
    }

    var dt = new Date(date);
    return format(dt, ui.global.locale.dateTimeFmt);
  },

  date(date) {
    if (!date) {
      return '-';
    }

    return format(new Date(date), ui.global.locale.dateFmt);
  }
}
