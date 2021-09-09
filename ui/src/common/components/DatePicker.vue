
<template>
  <Calendar class="os-date-picker" v-model="inputValue" :show-time="showTime" :date-format="format" 
    :show-icon="true" :month-navigator="true" :year-navigator="true" year-range="1900:2100"
    :show-button-bar="true" :hide-on-date-time-select="true" />
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
      let fmt = this.ui.global.shortDateFmt || 'dd-mm-yyyy';
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

.os-date-picker {
  width: 100%;
}

</style>
