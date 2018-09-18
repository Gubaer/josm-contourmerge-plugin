package org.openstreetmap.josm.plugins.contourmerge.fixtures;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.logging.Logger;

import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.date.DateUtils;
import java.util.TimeZone;

//TODO: updated with code in josm/test/unit/org/openstreetmap/josm/JOSMFixture.java
// should be replaced by josm/test/unit/org/openstreetmap/josm/JOSMFixture.java

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
                fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing "
                       + "or not readable.\nUpdate system property '{0}'", "josm.home", f));
            }
        }

        System.setProperty("josm.home", josmHome);
        TimeZone.setDefault(DateUtils.UTC);
        Preferences pref = Preferences.main();
        Config.setPreferencesInstance(pref);
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
        Config.setUrlsProvider(JosmUrls.getInstance());
        pref.resetToInitialState();
        pref.enableSaveOnPut(false);
        I18n.init();
        // initialize the plaform hook, and
        // call the really early hook before we anything else
        PlatformManager.getPlatform().preStartupHook();

        Logging.setLogLevel(Logging.LEVEL_INFO);
        pref.init(false);
        String url = Config.getPref().get("osm-server.url");
        if (url == null || url.isEmpty() || isProductionApiUrl(url)) {
            Config.getPref().put("osm-server.url", "https://api06.dev.openstreetmap.org/api");
        }
        I18n.set(Config.getPref().get("language", "en"));

        try {
            CertificateAmendment.addMissingCertificates();
        } catch (IOException | GeneralSecurityException ex) {
            throw new JosmRuntimeException(ex);
        }

        // init projection
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator

        // setup projection grid files
        MainApplication.setupNadGridSources();

        // make sure we don't upload to or test against production
        url = OsmApi.getOsmApi().getBaseUrl().toLowerCase(Locale.ENGLISH).trim();
        if (isProductionApiUrl(url)) {
            fail(MessageFormat.format("configured server url ''{0}'' seems to be a productive url, aborting.", url));
        }

        // Setup callbacks
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);

//        if (createGui) {
//            GuiHelper.runInEDTAndWaitWithException(() -> setupGUI());
//        }
    }

    private static boolean isProductionApiUrl(String url) {
        return url.startsWith("http://www.openstreetmap.org") || url.startsWith("http://api.openstreetmap.org")
                || url.startsWith("https://www.openstreetmap.org") || url.startsWith("https://api.openstreetmap.org");
    }
}
