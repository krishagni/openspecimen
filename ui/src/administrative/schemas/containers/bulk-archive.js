
import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      "name": "container.name",
      "labelCode": "containers.name",
      "type": "span",
      "value": ({container}) => {
        if (container.displayName) {
          return container.displayName + ' (' + container.name + ')';
        }     
                
        return container.name;
      },
      "uiStyle": {
        "min-width": "125px"
      },
      "href": (rowObject) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: rowObject.container.id})
    },
    {
      "name": "container.dimension",
      "labelCode": "containers.dimension",
      "type": "span",
      "value": ({container}) => {
        if (container.positionLabelingMode != 'NONE') {
          return container.noOfRows + ' x ' + container.noOfColumns;
        }

        return '-'
      },
      "uiStyle": {
        "min-width": "75px"
      }
    },
    {
      "name": "container.siteName",
      "labelCode": "containers.site",
      "type": "span",
    },
    {
      "name": "container.storageLocation",
      "labelCode": "containers.parent_container",
      "type": "span",
      "displayType": "storage-position"
    },
    {
      "name": "container.transferredBy",
      "labelCode": "containers.archived_by",
      "type": "user",
      "validations": {
        "required": {
          "messageCode": "containers.archived_by_required"
        }
      },
      "uiStyle": {
        "min-width": "200px"
      },
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.transferDate",
      "labelCode": "containers.transfer_date_time",
      "type": "datePicker",
      "showTime": true,
      "validations": {
        "required": {
          "messageCode": "containers.transfer_date_time_required"
        }
      },
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.transferComments",
      "labelCode": "containers.transfer_reasons",
      "type": "textarea",
      "rows": "5",
      "validations": {
        "required": {
          "messageCode": "containers.transfer_reasons_required"
        }
      },
      "enableCopyFirstToAll": true
    }   
  ]
}
