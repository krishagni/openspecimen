import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "event.id",
      caption: "Event ID",
      value: function(rowObject) {
        return '#' + rowObject.event.id;
      }
    },
    {
      name: "event.fromSite",
      caption: "From Site",
    },
    {
      name: "event.fromLocation",
      caption: "From Location",
      type: "storage-position",
      href: (row) => {
        const location = row.rowObject.event.fromLocation;
        if (!location) {
          return null;
        }

        return routerSvc.getUrl('ContainerDetail.Overview', {containerId: location.id})
      }
    },
    {
      name: "event.toSite",
      caption: "To Site",
    },
    {
      name: "event.toLocation",
      caption: "To Location",
      type: "storage-position",
      href: (row) => {
        const location = row.rowObject.event.toLocation;
        if (!location) {
          return null;
        }

        return routerSvc.getUrl('ContainerDetail.Overview', {containerId: location.id})
      }
    },
    {
      name: "event.user",
      caption: "User",
      type: "user"
    },
    {
      name: "event.time",
      caption: "Time",
      type: "date-time"
    }
  ]
}
