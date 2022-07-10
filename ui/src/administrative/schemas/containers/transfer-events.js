import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "event.id",
      captionCode: "containers.event_id",
      value: function(rowObject) {
        return '#' + rowObject.event.id;
      }
    },
    {
      name: "event.fromSite",
      captionCode: "containers.from_site",
    },
    {
      name: "event.fromLocation",
      captionCode: "containers.from_location",
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
      captionCode: "containers.to_site",
    },
    {
      name: "event.toLocation",
      captionCode: "containers.to_location",
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
      captionCode: "containers.user",
      type: "user"
    },
    {
      name: "event.time",
      captionCode: "containers.transfer_date_time",
      type: "date-time"
    }
  ]
}
