<template>
  <ul class="os-activity-cards">
    <li class="activity-card" v-for="activity of activitiesList" :key="activity.id" @click="onClick(activity)">
      <div class="date-time">
        <div class="month">
          <span>{{$filters.formatDate(activity._uiProps.time, 'MMM yyyy')}}</span>
        </div>
        <div class="date">
          <span v-if="activity.time">{{activity._uiProps.time.getDate()}}</span>
          <span v-else>?</span>
        </div>

        <div class="time">
          <span>{{$filters.formatDate(activity._uiProps.time, 'hh:mm a')}}</span>
        </div>
      </div>
      <div class="summary">
        <h4 class="title">{{activity._uiProps.title}}</h4>
        <div class="description">
          <slot name="activity-description" v-bind="activity" />
        </div>
      </div>
    </li>
  </ul>
</template>

<script>
import exprUtil from '@/common/services/ExpressionUtil.js';

export default {
  props: ['activities', 'time', 'title'],

  emits: ['click'],

  computed: {
    activitiesList: function() {
      const result = [];
      for (const activity of this.activities || []) {
        const item = {...activity};
        let time  = exprUtil.getValue(item, this.time);
        if (!(time instanceof Date)) {
          try {
            time = new Date(time);
          } catch (e) {
            console.log('Error converting activity time to date object');
          }
        }

        const title = exprUtil.getValue(item, this.title || 'n/a');
        item._uiProps = { time, title };
        result.push(item);
      }

      return result;
    }
  },

  methods: {
    onClick: function(activity) {
      this.$emit('click', activity);
    }
  }
}
</script>

<style scoped>
.os-activity-cards {
  list-type: none;
  padding: 0;
  margin: 0;
}

.os-activity-cards .activity-card {
  display: flex;
  margin: 0rem 1rem 1rem 1rem;
  padding: 0.5rem;
  border: 0px solid #ddd;
  border-radius: 0.5rem;
  /*box-shadow: rgba(50, 50, 93, 0.25) 0px 13px 27px -5px, rgba(0, 0, 0, 0.3) 0px 8px 16px -8px;*/
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3),0 2px 6px 2px rgba(60, 64, 67, .15);
}

.os-activity-cards .activity-card:hover {
  background: #f7f7f7;
  cursor: pointer;
}

.os-activity-cards .activity-card .date-time {
  flex: 0 0 5rem;
  height: 5.5rem;
  border-right: 1px solid #ddd;
  margin-right: 0.5rem;
  display: flex;
  flex-direction: column;
}

.os-activity-cards .activity-card .date-time .month {
  flex: 0 0 1rem;
  width: 5rem;
  text-align: center;
}

.os-activity-cards .activity-card .date-time .date {
  flex: 1;
  text-align: center;
  font-size: 2rem;
}

.os-activity-cards .activity-card .date-time .time {
  flex: 0 0 1rem;
  width: 5rem;
  text-align: center;
}

.os-activity-cards .activity-card .summary {
  flex: 1;
}

.os-activity-cards .activity-card .summary .title {
  margin-top: 0rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.os-activity-cards .activity-card .summary .description {
  display: flex;
  flex-direction: column;
  font-size: 0.8rem;
  justify-content: space-around;
}
</style>
