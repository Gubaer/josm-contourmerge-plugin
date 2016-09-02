package org.openstreetmap.josm.plugins.contourmerge.fixtures;

import static org.junit.Assert.fail;

import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.I18n;

public class JOSMFixture {
    static private final Logger logger = Logger.getLogger(JOSMFixture.class.getName());

    static public JOSMFixture createUnitTestFixture() {
        return new JOSMFixture();
    }

    public JOSMFixture() {
    }

    public void init() {
        String josmHome = System.getProperty("josm.home");
        if (josmHome == null) {
            fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
        } else {
            josmHome = josmHome.trim();
            File f = new File(josmHome);
            if (! f.exists() || ! f.canRead()) {
                fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing or not readable.\nUpdate system property '{0}'", "josm.home", f));
            }
        }

        I18n.init();
        // initialize the platform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we do anything else
        Main.platform.preStartupHook();

        Main.pref.init(false);

        // init projection
        ProjectionPreference.setProjection("core:mercator", null);

        // make sure we don't upload to or test against production
        //
        String url = OsmApi.getOsmApi().getBaseUrl().toLowerCase().trim();
        if (url.startsWith("http://www.openstreetmap.org")
                || url.startsWith("http://api.openstreetmap.org")) {
            fail(MessageFormat.format("configured server url ''{0}'' seems to be a productive url, aborting.", url));
        }
    }
}
