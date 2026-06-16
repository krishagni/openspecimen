import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "task.name",
      captionCode: "container_tasks.name",
      href: ({rowObject: {task: {id: taskId}}}) => routerSvc.getUrl('ContainerTaskAddEdit', {taskId})
    },
    {
      name: "task.description",
      captionCode: "container_tasks.description",
    }
  ],

  filters: [
    {
      name: "name",
      type: "text",
      captionCode: "container_tasks.name"
    }
  ]
}
