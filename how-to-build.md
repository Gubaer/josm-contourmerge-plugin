## Build

```bash
# this will download and install gradle if necessary 
% ./gradlew clean build
```

## Deploy and publish

The latest build for the `contourmerge` plugin should always be built on the branch `deploy`
and then pushed to GitHub. 

The following steps mimic the deployment infrastructure JOSM uses for plugins maintained
on the OSM SVN site, see [doc](http://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins)
for more info. JOSM reads meta-data about available plugins from http://josm.openstreetmap.de/plugin
and the following steps ensure that the `contourmerge` plugin is properly listed there.


Before you deploy, open `releases.conf` in a text editor and add a new line to the version table:

```groovy
releases = [
    [
        // replace with a new plugin version 
     	pluginVersion    : 1021,     
     	// the smallest JOSM version this plugin version is compatible with
     	josmVersion      : 10659,    
     	// a short description
     	description      : "OsmPrimitive::isSelectablePredicate now a Java8 closure"
    ],
    ...
}
```

Typical steps:

```bash
% git checkout deploy
% git merge master             # merge the development work from the branch 'master'
% git push origin deploy       # push the deploy branch

% ./gradlew clean build        # build the plugin
% ./gradlew deploy             # deploys the plugin to GitHub where it is picked up
                               # by the JOSM plugin installer
```
