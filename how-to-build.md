# Build

```bash
% ./gradlew clean build
```

# Deploy and publish

1. Create a new entry at the top of `releases.yml`. Example:
```yml
    releases:
        # mandatory: choose a new release label
      - label: v0.0.20
        # mandatory: the lowest josm version this plugin release is compatible with
        minJosmVersion: 12345
        # optional: a release description. You may refer to github issues.
        description: fixes issue #14
```

1. Create the GitHub release for the current release label
```bash
$ ./gradlew createGithubRelease
```

1. Build the plugin
```bash
$ ./gradlew clean build
```

1. Publish the plugin to the Github releases

The plugin is published to the release for the current release label.

```bash
$ ./gradlew publishToGithubRelease
```
Make sure the release exists and the `contourmerge.jar` is available for this
release: [https://github.com/Gubaer/josm-contourmerge-plugin/releases]


* Open [https://josm.openstreetmap.de/wiki/PluginsSource]
* Edit the page and update the link to the `contourmerge.jar`

