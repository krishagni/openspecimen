<template>
  <div class="os-specimen-events">
    <div class="event" v-for="event of events" :key="event.id" @click="onClick(event)">
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
        <div class="action-buttons" v-if="!hideActions && (event.sysForm || event.isEditable)">
          <os-button-group class="buttons" v-if="event.isEditable">
            <os-button left-icon="edit" size="small" v-os-tooltip.bottom="$t('specimens.edit_event')"
              @click="editEvent($event, event)" />
            <os-button left-icon="trash" size="small" v-os-tooltip.bottom="$t('specimens.delete_event')"
              @click="deleteEvent($event, event)" v-if="!event.sysForm" />
          </os-button-group>
          <os-tag class="info" type="info" :rounded="true" :value="$t('specimens.system_event')" v-if="event.sysForm" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: ['events', 'hide-actions'],

  emits: ['edit-event', 'delete-event', 'click'],

  methods: {
    editEvent: function(uiEvent, event) {
      uiEvent.stopPropagation();
      this.$emit('edit-event', event);
    },

    deleteEvent: function(uiEvent, event) {
      uiEvent.stopPropagation();
      this.$emit('delete-event', event);
    },

    onClick: function(event) {
      this.$emit('click', event);
    }
  }
}
</script>

<style scoped>
.os-specimen-events .event {
  display: flex;
  margin: 0rem 1rem 1rem 1rem;
  padding: 0.5rem;
  border: 0px solid #ddd;
  border-radius: 0.5rem;
  /*box-shadow: rgba(50, 50, 93, 0.25) 0px 13px 27px -5px, rgba(0, 0, 0, 0.3) 0px 8px 16px -8px;*/
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3),0 2px 6px 2px rgba(60, 64, 67, .15);
}

.os-specimen-events .event:hover {
  background: #f7f7f7;
  cursor: pointer;
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

.os-specimen-events .event .summary {
  flex: 1;
}

.os-specimen-events .event .summary .title {
  margin-top: 0rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.os-specimen-events .event .summary .description {
  display: flex;
  flex-direction: column;
  font-size: 0.8rem;
  justify-content: space-around;
}

.os-specimen-events .event .summary .action-buttons {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
}

.os-specimen-events .event .summary .action-buttons .buttons {
  flex: 1;
}

.os-specimen-events .event .summary .action-buttons .info :deep(.p-tag-value) {
  line-height: 1;
}
</style>
