1. Add symbolic links to the src folder of the respective plugins.
   $ ln -s <path-to-plugin/task-manager/src/main/ui/src task-manager

2. Enable localPlugins in global.js.
   {
      server: ...,

      localPlugins: true,

      ...
   }
