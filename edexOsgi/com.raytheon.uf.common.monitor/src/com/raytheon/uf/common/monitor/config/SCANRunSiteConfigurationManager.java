package com.raytheon.uf.common.monitor.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.raytheon.uf.common.dataplugin.radar.util.RadarsInUseUtil;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.monitor.events.MonitorConfigEvent;
import com.raytheon.uf.common.monitor.events.MonitorConfigListener;
import com.raytheon.uf.common.monitor.xml.SCANModelParameterXML;
import com.raytheon.uf.common.monitor.xml.SCANSiteRunConfigXML;
import com.raytheon.uf.common.monitor.xml.SCANSiteXML;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * ScanRunSiteConfigurationManager
 *
 * Holds the SCAN configuration
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/07/2009   2037       dhladky     Initial Creation.
 * 02/25/13     1660       dhladky     Fixed configuration bug in scan.
 * Aug 13, 2013 1742       dhladky     Concurrent mod exception on update fixed
 * Oct 02, 2013 2361       njensen     Use JAXBManager for XML
 * Jan 20, 2015 3949       nabowle     Add lightning source.
 * Mar 27, 2015 4260       dhladky     Updated default model.
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */

public class SCANRunSiteConfigurationManager implements
        ILocalizationFileObserver {

    /** Path to FFMP Source config. */
    private static final String CONFIG_FILE_NAME = "scan" + File.separatorChar
            + "SCANRunSiteConfig.xml";
    
    /** default model to set in XML **/
    private static final String DEFAULT_MODEL = "HRRR";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SCANRunSiteConfigurationManager.class);

    // This needs to initialize before the instance since the constructor will
    // makes use of JAXB. JVM spec 12.4.2 step 9 indicates this will
    // initialize ahead of the instance since it is earlier in
    // in the text source.
    private static final SingleTypeJAXBManager<SCANSiteRunConfigXML> jaxb = SingleTypeJAXBManager
            .createWithoutException(SCANSiteRunConfigXML.class);

    /** Singleton instance of this class */
    private static SCANRunSiteConfigurationManager instance = new SCANRunSiteConfigurationManager();

    /**
     * SCAN Configuration XML object.
     */
    protected SCANSiteRunConfigXML configXml;

    private LocalizationFile lf = null;

    private List<MonitorConfigListener> listeners = new CopyOnWriteArrayList<MonitorConfigListener>();

    protected boolean isPopulated;

    /* Private Constructor */
    private SCANRunSiteConfigurationManager() {
        isPopulated = false;

        try {
            readConfigXml();
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Can not read the SCAN configuration", e);
        }
    }

    /**
     * Get an instance of this singleton.
     *
     * @return Instance of this class
     */
    public static SCANRunSiteConfigurationManager getInstance() {
        return instance;
    }

    /**
     * Read the XML configuration data for the current XML file name.
     */
    public synchronized void readConfigXml() throws SerializationException {

        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);

        lf = pm.getLocalizationFile(lc, CONFIG_FILE_NAME);
        lf.addFileUpdatedObserver(this);
        File file = lf.getFile();
        // System.out.println("Reading -- " + file.getAbsolutePath());
        if (!file.exists()) {
            statusHandler.handle(
                    Priority.WARN,
                    "SCANRunSiteConfigurationManager: "
                            + file.getAbsolutePath() + " does not exist.");
            try {
                createValidConfig();
            } catch (Exception e) {
                statusHandler
                        .handle(Priority.ERROR,
                                "SCANRunSiteConfigurationManager: Couldn't create valid runnable configuration");
            }
        }

        SCANSiteRunConfigXML configXmltmp = null;

        configXmltmp = jaxb.unmarshalFromXmlFile(file.getAbsolutePath());

        configXml = configXmltmp;
        isPopulated = true;
    }

    public void addListener(MonitorConfigListener fl) {
        listeners.add(fl);
    }

    public void removeListener(MonitorConfigListener fl) {
        listeners.remove(fl);
    }

    /**
     * Save the XML configuration data to the current XML file name.
     */
    public void saveConfigXml() {
        // Save the xml object to disk
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);

        LocalizationFile newXmlFile = pm.getLocalizationFile(lc,
                CONFIG_FILE_NAME);

        if (newXmlFile.getFile().getParentFile().exists() == false) {
            // System.out.println("Creating new directory");

            if (newXmlFile.getFile().getParentFile().mkdirs() == false) {
                // System.out.println("Could not create new directory...");
            }
        }

        try {
            // System.out.println("Saving -- "
            // + newXmlFile.getFile().getAbsolutePath());
            jaxb.marshalToXmlFile(configXml, newXmlFile.getFile()
                    .getAbsolutePath());
            newXmlFile.save();
            setPopulated(true);

            lf = newXmlFile;
        } catch (Exception e) {
            statusHandler.handle(Priority.WARN,
                    "SCANRunSiteConfigurationManager: " + newXmlFile.getName()
                            + " couldn't be saved.", e);
        }
    }

    @Override
    public void fileUpdated(FileUpdatedMessage message) {

        if (message.getFileName().equals(CONFIG_FILE_NAME)) {
            try {
                readConfigXml();
                // inform listeners
                for (MonitorConfigListener fl : listeners) {
                    fl.configChanged(new MonitorConfigEvent(this));
                }

            } catch (Exception e) {
                statusHandler.handle(
                        Priority.WARN,
                        "SCANRunSiteConfigurationManager: "
                                + message.getFileName()
                                + " couldn't be updated.", e);
            }
        }
    }

    public void addRunConfig(SCANSiteRunConfigXML configXml) {
        this.configXml = configXml;
    }

    public boolean isPopulated() {
        return isPopulated;
    }

    public void setPopulated(boolean isPopulated) {
        this.isPopulated = isPopulated;
    }

    /**
     * Get a site listing
     *
     * @return
     */
    public ArrayList<String> getSiteNames() {
        if (configXml != null) {
            return configXml.getSiteNames();
        }
        return null;
    }

    /**
     * Get a local site listing
     *
     * @return
     */
    public ArrayList<String> getLocalSiteNames() {
        if (configXml != null) {
            return configXml.getLocalSiteNames();
        }
        return null;
    }

    /**
     * Get a dial site listing
     *
     * @return
     */
    public ArrayList<String> getDialSiteNames() {
        if (configXml != null) {
            return configXml.getDialSiteNames();
        }
        return null;
    }

    /**
     * Get the lightning source.
     *
     * @return
     */
    public String getLightningSource() {
        if (configXml != null) {
            return configXml.getLightningSource();
        }
        return null;
    }

    /**
     * Get the Site you are seeking
     *
     * @param name
     * @return
     */
    public SCANSiteXML getSiteConfig(String name) {
        SCANSiteXML siteXML = null;
        for (SCANSiteXML site : configXml.getSites()) {
            if (site.getScanSite().equals(name)) {
                siteXML = site;
                break;
            }
        }

        return siteXML;

    }

    /**
     * Creates a valid configuration based on radar config
     */
    public void createValidConfig() throws Exception {

        /**
         * Don't have one, so create an EDEX generated default
         */
        List<String> localsites = RadarsInUseUtil.getSite(null,
                RadarsInUseUtil.LOCAL_CONSTANT);
        String modelDefault = DEFAULT_MODEL;

        configXml = new SCANSiteRunConfigXML();

        // run over list of available sites
        int i = 0;
        for (String site : localsites) {
            if (i < 12) { // no more than 12 radars in scan config
                SCANSiteXML siteXML = new SCANSiteXML();
                siteXML.setScanSite(site);
                siteXML.setMenuLocation(RadarsInUseUtil.LOCAL_CONSTANT);

                for (DATA_TYPE param : DATA_TYPE.values()) {
                    SCANModelParameterXML paramXML = new SCANModelParameterXML();
                    paramXML.setParameterName(param.getType());
                    paramXML.setModelName(modelDefault);
                    siteXML.addModelParameter(paramXML);
                }

                configXml.addSite(siteXML);
                i++;
            }
        }

        saveConfigXml();
    }

    /**
     *
     * Enumeration for which type of ModelData. CAPE is: Convective Available
     * Potential Energy. HELI is: storm relative HELIcity.
     *
     * @author dhladky
     * @version 1.0
     */
    public enum DATA_TYPE {

        HELI("HELI"), CAPE("CAPE"), U700("U700"), V700("V700"), U500("U500"), GH1000(
                "GH1000"), GH500("GH500");

        private final String type;

        private DATA_TYPE(String name) {
            type = name;
        }

        public String getType() {
            return type;
        }
    };

}
