
class JobResources {
  runOpts       = {resource: 'ScheduledJob', operations: ['Read']};

  createOpts    = {resource: 'ScheduledJob', operations: ['Create']};

  updateOpts    = {resource: 'ScheduledJob', operations: ['Update']};

  deleteOpts    = {resource: 'ScheduledJob', operations: ['Delete']};

  importOpts    = {resource: 'ScheduledJob', operations: ['Export Import']};
}

export default new JobResources();
