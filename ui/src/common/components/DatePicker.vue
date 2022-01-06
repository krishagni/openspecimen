
<template>
  <div class="os-date-picker">
    <div class="p-float-label" :class="!$attrs.placeholder && 'no-label'" v-if="$attrs['md-type']">
      <Calendar v-model="inputValue" :show-time="showTime" :date-format="format"
        :show-icon="true" :month-navigator="true" :year-navigator="true" year-range="1900:2100"
        :show-button-bar="true" :hide-on-date-time-select="true" />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <Calendar v-model="inputValue" :show-time="showTime" :date-format="format"
        :show-icon="true" :month-navigator="true" :year-navigator="true" year-range="1900:2100"
        :show-button-bar="true" :hide-on-date-time-select="true" :placeholder="$attrs.placeholder" />
    </div>
  </div>
</template>

<script>
import Calendar from 'primevue/calendar';

export default {
  props: ['modelValue', 'showTime'],

  inject: ['ui'],

  components: {
    Calendar
  },

  data() {
    return {
    }
  },

  computed: {
    inputValue: {
      get() {
        if (typeof this.modelValue == 'string') {
          try {
            return new Date(parseInt(this.modelValue));
          } catch {
            return new Date(this.modelValue);
          }
        } else if (typeof this.modelValue == 'number') {
          return new Date(this.modelValue);
        } else if (this.modelValue instanceof Date) {
          return this.modelValue;
        }

        return null;
      },

      set(value) {
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

      return fmt;
    }
  },

  methods: {
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
}

.os-date-picker .p-float-label :deep(.p-inputtext:not(:enabled:focus) ~ label) {
  color: #999;
  opacity: 1;
}
</style>
