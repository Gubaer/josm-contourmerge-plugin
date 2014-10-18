## Build

```bash
# this will download and install gradle if necessary 
% ./gradlew build
```

## Deploy and publish

The latest build for the `contourmerge` plugin should always be built on the branch `deploy`
and then pushed to GitHub. 

The following steps mimic the deployment infrastructure JOSM uses for plugins maintained
on the OSM SVN site, see [doc](http://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins)
for more info. JOSM reads meta-data about available plugins from http://josm.openstreetmap.de/plugin
and the following steps ensure that the `contourmerge` plugin is properly listed there.


Before you deploy, open `build.gradle` in a text editor and add a new line to the version table:

```groovy
project.ext.pluginVersions = [
    // TODO: add a new line with the current version and a reference to the compatible JOSM version
    /* plugin version,  required JOSM version */
    [1009,              6317],
    // ...
]
```

Typical steps:

```bash
% git checkout deploy
% git merge master          # merge the development work from the branch 'master'

   # edit build.gradle, add a new plugin version, and commit build.gradle

% ./gradlew build              # build the plugin
% ./gradlew deploy             # deploys the plugin to GitHub where it is picked up
                               # by the JOSM plugin installer
```
