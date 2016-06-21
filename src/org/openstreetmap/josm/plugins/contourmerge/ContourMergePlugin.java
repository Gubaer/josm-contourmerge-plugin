package org.openstreetmap.josm.plugins.contourmerge;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * <strong>ContourMergePlugin</strong> is the main class for the
 * <tt>contourmerge</tt> plugin.
 */
public class ContourMergePlugin extends Plugin {

    public ContourMergePlugin(PluginInformation info) {
        super(info);
        ContourMergeModelManager.getInstance().wireToJOSM();
        ContourMergeView.getInstance().wireToJOSM();
    }

    static private boolean modeEnabled;

    static public ContourMergeModelManager getModelManager() {
        return ContourMergeModelManager.getInstance();
    }

    static public ContourMergeView getView() {
        return ContourMergeView.getInstance();
    }

    static public boolean isEnabled() {
        return modeEnabled;
    }

    static public void setEnabled(boolean enabled) {
        modeEnabled = enabled;
    }
}
