
class DpResources {
  createOpts    = {resource: 'DistributionProtocol', operations: ['Create']};

  updateOpts    = {resource: 'DistributionProtocol', operations: ['Update']};

  deleteOpts    = {resource: 'DistributionProtocol', operations: ['Delete']};

  importOpts    = {resource: 'DistributionProtocol', operations: ['Export Import']};

  orderOpts     = {resource: 'Order', operations: ['Read']};
}

export default new DpResources();
