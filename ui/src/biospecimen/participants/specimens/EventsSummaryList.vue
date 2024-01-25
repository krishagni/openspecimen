<template>
  <div class="os-specimen-events">
    <div class="event" v-for="event of events" :key="event.id">
      <div class="date-time">
        <div class="month">
          <span>{{$filters.formatDate(event.time, 'MMM yyyy')}}</span>
        </div>
        <div class="date">
          <span v-if="event.time">{{event.time.getDate()}}</span>
          <span v-else>?</span>
        </div>

        <div class="time">
          <span>{{$filters.formatDate(event.time, 'hh:mm a')}}</span>
        </div>
      </div>
      <div class="summary">
        <h4 class="title">{{event.name}}</h4>
        <div class="description">
          <span><i>by</i> {{event.user}}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: ['events']
}
</script>

<style scoped>
.os-specimen-events .event {
  display: flex;
  margin-bottom: 1rem;
  padding: 0.5rem;
  border: 0px solid #ddd;
  border-radius: 0.5rem;
  cursor: pointer;
  box-shadow: rgba(50, 50, 93, 0.25) 0px 13px 27px -5px, rgba(0, 0, 0, 0.3) 0px 8px 16px -8px;
}

.os-specimen-events .event .date-time {
  flex: 0 0 5rem;
  height: 5.5rem;
  border-right: 1px solid #ddd;
  margin-right: 0.5rem;
  display: flex;
  flex-direction: column;
}

.os-specimen-events .event .date-time .month {
  flex: 0 0 1rem;
  width: 5rem;
  text-align: center;
}

.os-specimen-events .event .date-time .date {
  flex: 1;
  text-align: center;
  font-size: 2rem;
}

.os-specimen-events .event .date-time .time {
  flex: 0 0 1rem;
  width: 5rem;
  text-align: center;
}

.os-specimen-events .event .summary .title {
  margin-top: 0rem;
  color: #666;
}

.os-specimen-events .event .summary .description {
  display: flex;
  flex-direction: column;
  font-size: 0.8rem
}
</style>
