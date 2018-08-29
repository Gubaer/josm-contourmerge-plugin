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
        numeric_josm_version: 12345
        # optional: a release description. You may refer to github issues.
        description: fixes issue #14
```


2. Make sure the build parameters in `build.gradle` are up to date

```gradle
// the same label as in releases.yml
version="v0.0.20"
josm {
    manifest {
        //  the same  version as in releases.yml
        minJosmVersion = 12345
...
```

3. Create the GitHub release for the current release label
```bash
$ ./gradlew createGithubRelease
```

4. Build the plugin
```bash
$ ./gradlew clean build
```

4. Publish the plugin to the Github releases

The plugin is published to two releases:

* to the release for the current release label
* to the pickup release with a version independent name, where the JOSM plugin infrastructures picks up the latest plugin release

```bash
$ ./gradlew publishToGithubRelease --publish-to-pickup-release
```
