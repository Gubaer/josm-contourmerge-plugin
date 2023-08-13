package org.openstreetmap.josm.plugins.contourmerge;

import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * <strong>ContourMergePlugin</strong> is the main class for the
 * <tt>contourmerge</tt> plugin.
 */
public class ContourMergePlugin extends Plugin {

    public ContourMergePlugin(PluginInformation info) {
        super(info);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (newFrame != null){
            newFrame.addMapMode(new IconToggleButton(
                new ContourMergeMode(newFrame))
            );
            ContourMergeModelManager.getInstance().wireToJOSM();
            ContourMergeView.getInstance().wireToJOSM();
        } else {
            ContourMergeModelManager.getInstance().unwireFromJOSM();
            ContourMergeView.getInstance().unwireFromJOSM();
        }
    }

    static private boolean modeEnabled;

    static public ContourMergeModelManager getModelManager() {
        return ContourMergeModelManager.getInstance();
    }

    static public boolean isEnabled() {
        return modeEnabled;
    }

    static public void setEnabled(boolean enabled) {
        modeEnabled = enabled;
    }
}
