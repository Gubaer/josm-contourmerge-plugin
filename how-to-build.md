BUILD 
=====
 o copy 'build.properties.template' to 'build.properties' and adjust the build configuration
   settings

 o run ant in the root directory
   % ant dist
    
DEPLOY
======
The latest build for the 'contourmerge' plugin should always be built on the branch 'deploy'
and then pushed to GitHub. 

The following steps mimic the deployment infrastructure JOSM uses for plugins maintained
on the OSM SVN site, see <http://josm.openstreetmap.de/wiki/DevelopersGuide/DevelopingPlugins>
for more info. JOSM reads meta-data about available plugins from <http://josm.openstreetmap.de/plugin>
and the following steps ensure that the 'contourmerge' plugin is properly listed in this
plugin directory. 


There is a special branch 'deploy' for deploying.   
  % git checkout deploy
  % git merge master          # merge the development work from the branch 'master'
  % ant dist                  # this creates dist/scripting.jar which is part
                              # of the Git tree under this branch
  % git push origin deploy    # Pushes the latest build to git hub  

If the build requires a minimal JOSM version, i.e. JOSM 1234, then run the following
steps:
 1. Remember the current plugin version and the JOSM version it is compatible with by adding
    a configuration entry to build.properties, i.e.
      plugin.1111.requires=8888
 
 1. Increase the plugin version in build.properties by setting the property 'plugin.version', i.e.
      plugin.version=2222
          
 2. Set the lowest JOSM version the plugin is compatible. Use property 'josm.required.version' in
    'build.properties', i.e.
       josm.required.version=9999

The plugin Manifest will then include version information used by JOSMs plugin update system, for instance:

    1111-Plugin-Url: 1111;https://raw.github.com/Gubaer/josm-contourmerge-plugin/for-josm-8888/dist/contourmerge.jar

Finally, switch to branch deploy, merge and upload 
    % git checkout -f deploy
    % git merge master
    % git push origin
    
Build the jar and push it too  
    % ant dist                      # creates the scripting.jar, dependency specifications 
                                    # become part of the Manifest in the plugin                            
    % git push origin deploy        # push the jar to Git Hub
    
Tag the latest build and push the new tag     
    % git tag for-josm-9999  HEAD   # tag the latest commit 
    % git push origin for-josm-999  # makes sure the latest build is tagged with 'for-josm-1234'
    
This ensures, that the latest scripting plugin is available from 
    <https://raw.github.com/Gubaer/josm-contourmerge-plugin/deploy/dist/contourmerge.jar>
    
The scripting plugin requiring JOSM 8888 is available from
    <https://raw.github.com/Gubaer/josm-contourmerge-plugin/for-josm-8888/dist/contourmerge.jar>