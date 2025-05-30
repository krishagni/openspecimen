<template>
  <div class="os-date-picker">
    <div class="p-float-label" :class="!$attrs.placeholder && 'no-label'" v-if="$attrs['md-type']">
      <Calendar ref="cal" v-model="inputValue" :show-time="showTime" :date-format="format" :tabindex="tabOrder"
        :show-icon="true" :month-navigator="true" :year-navigator="true" year-range="1900:2100"
        :show-button-bar="true" :hide-on-date-time-select="true" :selectOtherMonths="true"
        :manual-input="true" :disabled="disabled"
        @date-select="onDateSelect" @today-click="onTodaySelect">
        <template #footer>
          <os-button text :label="$t('common.buttons.done')" class="os-datepicker-done" @click="hideCalendar" />
        </template>
      </Calendar>
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <Calendar ref="cal" v-model="inputValue" :show-time="showTime" :date-format="format" :tabindex="tabOrder"
        :show-icon="true" :month-navigator="true" :year-navigator="true" year-range="1900:2100"
        :show-button-bar="true" :hide-on-date-time-select="true" :placeholder="$attrs.placeholder"
        :manual-input="true" :disabled="disabled" :selectOtherMonths="true"
        @date-select="onDateSelect" @today-click="onTodaySelect">
        <template #footer>
          <os-button text :label="$t('common.buttons.done')" class="os-datepicker-done" @click="hideCalendar" />
        </template>
      </Calendar>
    </div>
  </div>
</template>

<script>
import Calendar from 'primevue/calendar';

import utilSvc from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'showTime', 'tabOrder', 'disabled', 'dateOnly'],

  inject: ['ui'],

  components: {
    Calendar
  },

  data() {
    return {
    }
  },

  mounted() {
    if (!this.inputValue && Object.prototype.hasOwnProperty.call(this.$attrs, 'default-current-date')) {
      this.inputValue = new Date();
    }

    if (this.$refs.cal && this.$refs.cal.input) {
      this.$refs.cal.input.setAttribute('inputmode', 'text');
    }

    if (this.$refs.cal && this.$refs.cal.input) {
      let fmt = this.ui.global.locale.shortDateFmt || 'dd-mm-yyyy';
      if (this.showTime) {
        fmt += ' HH:mm';
      }

      this.$refs.cal.input.setAttribute('placeholder', fmt);
    }
  },

  computed: {
    inputValue: {
      get() {
        if (typeof this.modelValue == 'string') {
          try {
            if (this.dateOnly && this.modelValue.length == 10 && this.modelValue[4] == '-') {
              const [year, month, day] = this.modelValue.split('-');
              return new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
            }

            const intValue = parseInt(this.modelValue);
            if (intValue.toString().length == this.modelValue.trim().length) {
              return new Date(intValue);
            }

            return new Date(this.modelValue);
          } catch {
            return new Date(this.modelValue);
          }
        } else if (typeof this.modelValue == 'number') {
          if (this.dateOnly) {
            return utilSvc.getLocalDate(this.modelValue);
          }

          return new Date(this.modelValue);
        } else if (this.modelValue instanceof Date) {
          return this.modelValue;
        }

        return null;
      },

      set(value) {
        if (value && this.dateOnly) {
          const dt = new Date(value);
          const year  = "" + dt.getFullYear();
          const month = (dt.getMonth() < 9 ? "0" : "") + (dt.getMonth() + 1);
          const date  = (dt.getDate() < 10 ? "0" : "") + dt.getDate();
          value = year + "-" + month + "-" + date;
        }

        this.$emit('update:modelValue', value);
      }
    },

    format: function() {
      let fmt = this.ui.global.locale.shortDateFmt || 'dd-mm-yyyy';
      fmt = fmt.toLowerCase();
      if (fmt.indexOf('yyyy') >= 0) {
        fmt = fmt.replaceAll('yyyy', 'yy');
      } else if (fmt.indexOf('yy') >= 0) {
        fmt = fmt.replaceAll('yy', 'y');
      }      

      if (fmt.indexOf('mmmm') >= 0) {
        fmt = fmt.replaceAll('mmmm', 'MM');
      } else if (fmt.indexOf('mmm') >= 0) {
        fmt = fmt.replaceAll('mmm', 'M');
      }

      return fmt;
    }
  },

  methods: {
    getDisplayValue: function() {
      if (!this.inputValue) {
        return null;
      }

      let format = this.ui.global.locale.shortDateFmt || 'dd-MM-yyyy';
      if (this.showTime) {
        format += ' HH:mm';
      }

      return utilSvc.formatDate(this.inputValue, format);
    },

    onTodaySelect: function() {
      this.$emit('update:modelValue', new Date());
    },

    onDateSelect: function(date) {
      if (date && typeof date == 'object' && typeof date.setHours == 'function') {
        date.setHours(this.$refs.cal.currentHour);
      }
    },

    hideCalendar: function(event) {
      this.$refs.cal.overlayVisible = false;
      event.preventDefault();
    }
  }
}
</script>

<style scoped>
.os-date-picker :deep(.p-calendar) {
  width: 100%;
}

.os-date-picker .p-float-label:not(.no-label) {
  margin-top: 10px;
}

.os-date-picker .p-float-label :deep(.p-inputtext) {
  border: 0px;
  border-bottom: 2px solid #ced4da;
  border-radius: 0px;
  padding: 2px 0px;
  box-shadow: none;
}

.os-date-picker .p-float-label :deep(.p-button) {
  padding: 0;
  border: 0;
  border-bottom: 2px solid #ced4da;
  border-radius: 0;
}

.os-date-picker .p-float-label :deep(.p-inputtext:enabled:focus) {
  border-bottom-color: #007bff;
}

.os-date-picker .p-float-label :deep(label) {
  left: 0rem;
  right: 0rem;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.os-date-picker .p-float-label :deep(.p-inputtext:not(:enabled:focus) ~ label) {
  color: #999;
  opacity: 1;
}

</style>

<style>
.p-calendar-disabled input {
  opacity: 0.65;
  pointer-events: none;
}

.p-datepicker.p-component {
  max-width: 350px!important;
  width: 350px!important;
  min-width: 350px!important;
}

.p-datepicker .p-datepicker-header {
  padding: 0rem;
}

.p-datepicker table {
  margin: 0rem;
}

.p-datepicker table td {
  padding: 0.25rem;
}

.p-datepicker table td > span {
  height: auto;
  width:  auto;
}

.p-datepicker .p-timepicker {
  padding: 0rem;
}

.p-datepicker .p-timepicker span {
  font-size: 1rem;
}

.p-datepicker .p-timepicker button {
  height: 1rem;
  width: 1rem;
}

.p-datepicker .p-datepicker-buttonbar {
  padding: 0rem;
  justify-content: flex-start;
}

button.os-datepicker-done.btn.text {
  position: absolute;
  bottom: 0px;
  right: 0px;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  color: #007bff;
}

button.os-datepicker-done.btn.text:hover {
  background: rgba(0, 123, 255, 0.04);
  color: #007bff;
  border-color: transparent;
  border: 0px;
}
</style>
