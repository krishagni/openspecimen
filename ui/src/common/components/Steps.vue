
<template>
  <div class="os-steps">
    <div class="step-items">
      <div class="step-item" v-for="(step, idx) of steps" :key="idx" :class="{'active': idx == currentStep}">
        <span class="step-number">{{idx + 1}}</span>
        <span class="step-title">{{step.title}}</span>
      </div>
    </div>

    <slot></slot>
  </div>
</template>

<script>

export default {
  data() {
    return {
      steps: [],

      currentStep: 0
    }
  },

  methods: {
    addStep: function(step) {
      if (this.steps.indexOf(step) != -1) {
        return;
      }

      this.steps.push(step);
    },

    isActive: function(step) {
      return this.steps[this.currentStep] == step;
    },

    next: function() {
      let step = this.steps[this.currentStep];
      if (typeof step.validate == 'function' && !step.validate()) {
        return;
      }

      if (this.currentStep < this.steps.length - 1) {
        ++this.currentStep;
      }
    },

    previous: function() {
      let step = this.steps[this.currentStep];
      if (typeof step.validate == 'function' && !step.validate()) {
        return;
      }

      if (this.currentStep > 0) {
        --this.currentStep;
      }
    }
  }
}

</script>

<style scoped>

.os-steps .step-items {
  display: flex;
  flex-direction: row;
  margin-bottom: 1.25rem;
}

.os-steps .step-items .step-item {
  display: flex;
  flex-grow: 1;
  justify-content: center;
  border-bottom: 5px solid #ddd;
  padding: 0.5rem;
}

.os-steps .step-items .step-item .step-number {
  height: 2rem;
  width: 2rem;
  border: 1px solid #ddd;
  background: #fff;
  text-align: center;
  padding-top: 0.25rem;
  border-radius: 50%;
  margin-right: 0.5rem;
  font-weight: bold;
  opacity: 0.6;
}

.os-steps .step-items .step-item .step-title {
  padding-top: 0.3rem;
  opacity: 0.6;
  font-size: 1rem;
}

.os-steps .step-items .step-item.active {
  border-color: #3276b1;
}

.os-steps .step-items .step-item.active .step-number {
  background: #3276b1;
  color: #fff;
  opacity: 1;
}

.os-steps .step-items .step-item.active .step-title {
  opacity: 1;
}

</style>