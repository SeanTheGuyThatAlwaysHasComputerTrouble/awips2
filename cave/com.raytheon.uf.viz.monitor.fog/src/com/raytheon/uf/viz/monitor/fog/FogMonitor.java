/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.viz.monitor.fog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.dataplugin.fog.FogRecord.FOG_THREAT;
import com.raytheon.uf.common.jms.notification.NotificationMessage;
import com.raytheon.uf.common.monitor.MonitorAreaUtils;
import com.raytheon.uf.common.monitor.config.FSSObsMonitorConfigurationManager;
import com.raytheon.uf.common.monitor.config.FSSObsMonitorConfigurationManager.MonName;
import com.raytheon.uf.common.monitor.data.CommonConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.alerts.AlertMessage;
import com.raytheon.uf.viz.monitor.IMonitor;
import com.raytheon.uf.viz.monitor.Monitor;
import com.raytheon.uf.viz.monitor.ObsMonitor;
import com.raytheon.uf.viz.monitor.data.AreaContainer;
import com.raytheon.uf.viz.monitor.data.MonitoringArea;
import com.raytheon.uf.viz.monitor.data.ObMultiHrsReports;
import com.raytheon.uf.viz.monitor.data.ObReport;
import com.raytheon.uf.viz.monitor.data.ObsData;
import com.raytheon.uf.viz.monitor.data.TableData;
import com.raytheon.uf.viz.monitor.events.IMonitorConfigurationEvent;
import com.raytheon.uf.viz.monitor.events.IMonitorThresholdEvent;
import com.raytheon.uf.viz.monitor.fog.listeners.IFogResourceListener;
import com.raytheon.uf.viz.monitor.fog.threshold.FogThresholdMgr;
import com.raytheon.uf.viz.monitor.fog.ui.dialogs.FogMonitoringAreaConfigDlg;
import com.raytheon.uf.viz.monitor.fog.ui.dialogs.FogZoneTableDlg;
import com.raytheon.uf.viz.monitor.ui.dialogs.MonitoringAreaConfigDlg;
import com.raytheon.uf.viz.monitor.util.MonitorThresholdConfiguration;
import com.raytheon.viz.alerts.observers.ProductAlertObserver;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * FogMonitor, monitor Data that triggers changes to the Fog display.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2009 1981       dhladky     Initial creation.
 * 3/2/2009     2047       grichard    Made pluginName an array.
 * 3/2/2009     2047       grichard    Added stationName array.
 * 10/7/2009    ****       dhladky     reworked
 * 11/30/2009   3424       Zhidong/Slav/Wen Adds stationTableData to keep station info.
 * May 15, 2012 14510      zhao        Modified processing at startup
 * Jun 16, 2012 14386      zhao        Auto update County/Zone Table when new fog threat data arrives
 * Oct 26, 2012 1280       skorolev    Made changes for non-blocking dialog and changed HashMap to Map
 * Oct.31  2012 1297       skorolev    Clean code
 * Feb 15, 2013 1638       mschenke    Changed code to reference DataURI.SEPARATOR instead of URIFilter
 * Apr 28, 2014 3086       skorolev    Removed local getMonitorAreaConfig method.
 * Sep 04, 2014 3220       skorolev    Updated configUpdate method and added updateMonitoringArea.
 * Sep 23, 2014 3356       njensen     Remove unnecessary import
 * Mar 09, 2014 3888       dhladky     Stopped processing when dialogs are null or disposed.
 * Sep 18, 2015 3873       skorolev    Removed common definitions. Replaced deprecated NotificationMessage.
 * Dec 17, 2015 3873       dhladky     Abstracted handling of dialogTime and Zone dialog events.
 * 
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 * 
 */

public class FogMonitor extends ObsMonitor implements IFogResourceListener {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(FogMonitor.class);

    /** Singleton instance of this class */
    private static FogMonitor monitor = null;

    /**
     * This object contains all observation data necessary for the table dialogs
     * and trending plots [this replaces the objects of ObsData and TableData
     * below Jan 21, 2010, zhao]
     */
    private ObMultiHrsReports obData;

    /** data holder for FOG **/
    private ObsData obsData;

    /** data holder for FOG ALG data **/
    private SortedMap<Date, Map<String, FOG_THREAT>> algorithmData = null;

    /** list of coordinates for each zone **/
    private Map<String, Geometry> zoneGeometries = null;

    /** zone table dialog **/
    private FogZoneTableDlg zoneDialog;

    /** zone table dialog **/
    private MonitoringAreaConfigDlg areaDialog = null;

    /** area config manager **/
    private FSSObsMonitorConfigurationManager fogConfig = null;

    /** table data for the station table **/
    private final TableData stationTableData = new TableData(
            CommonConfig.AppName.FOG);

    /** List of fogAlg listeners **/
    private final List<IFogResourceListener> fogResources = new ArrayList<IFogResourceListener>();

    /**
     * Private constructor, singleton
     */
    private FogMonitor() {
        pluginPatterns.add(fssPattern);
        fogConfig = FSSObsMonitorConfigurationManager.getInstance(MonName.fog);
        updateMonitoringArea();
        initObserver(OBS, this);
        createDataStructures();
        processProductAtStartup();
        obData.getZoneTableData();
        readTableConfig(MonitorThresholdConfiguration.FOG_THRESHOLD_CONFIG);
    }

    /**
     * Static factory
     * 
     * @return fog monitor
     */
    public static synchronized FogMonitor getInstance() {
        if (monitor == null) {
            monitor = new FogMonitor();
            // Pre-populate dialog with an observations from DB
            monitor.fireMonitorEvent(monitor);
        }
        return monitor;
    }

    /**
     * Re-initialization of monitor.
     * 
     * DR#11279: When monitor area configuration is changed, this module is
     * called to re-initialize monitor using new monitor area configuration
     */
    public void reInitialize() {
        if (monitor != null) {
            monitor.nullifyMonitor();
            monitor = new FogMonitor();
        }
    }

    /**
     * Creates the maps
     */
    private void createDataStructures() {
        // [Jan 21, 2010, zhao]
        obData = new ObMultiHrsReports(CommonConfig.AppName.FOG);
        obData.setThresholdMgr(FogThresholdMgr.getInstance());
        obsData = new ObsData();
        algorithmData = new TreeMap<Date, Map<String, FOG_THREAT>>();
        for (String zone : MonitoringArea.getPlatformMap().keySet()) {
            obsData.addArea(zone, MonitoringArea.getPlatformMap().get(zone));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.ObsMonitor#filterNotifyMessage(com.raytheon
     * .uf.viz.core.notification.NotificationMessage)
     */
    @Override
    public boolean filterNotifyMessage(NotificationMessage alertMessage) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.ObsMonitor#processNotifyMessage(com.raytheon
     * .uf.viz.core.notification.NotificationMessage)
     */
    @Override
    public void processNotifyMessage(NotificationMessage filtered) {
        // Not used
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.ObsMonitor#processProductMessage(com.raytheon
     * .uf.viz.core.alerts.AlertMessage)
     */
    @Override
    public void processProductMessage(final AlertMessage filtered) {
        if (fssPattern.matcher(filtered.dataURI).matches()) {
            processURI(filtered.dataURI, filtered);
        }
    }

    /**
     * Method that reads the table configuration and updates the zone monitor
     * threshold map
     * 
     * @param file
     *            -- the xml configuration filename
     */
    public void readTableConfig(String file) {
        Map<String, List<String>> zones = new HashMap<String, List<String>>();
        // create zones and stations list
        try {
            for (String zone : fogConfig.getAreaList()) {
                // add the unique
                List<String> stations = fogConfig.getAreaStations(zone);
                zones.put(zone, stations);
            }
        } catch (Exception ve) {
            String msg = "FOG Monitor failed to load configuration..."
                    + this.getClass().getName();
            ErrorDialog.openError(Display.getCurrent().getActiveShell(),
                    "FOG Monitor failed to load configuration", msg,
                    new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, ve));

        }
        MonitoringArea.setPlatformMap(zones);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.monitor.Monitor#initObserver(java.lang.String,
     * com.raytheon.uf.viz.monitor.Monitor)
     */
    @Override
    public void initObserver(String pluginName, Monitor monitor) {
        ProductAlertObserver.addObserver(pluginName, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.ObsMonitor#thresholdUpdate(com.raytheon.uf
     * .viz.monitor.events.IMonitorThresholdEvent)
     */
    @Override
    public void thresholdUpdate(IMonitorThresholdEvent me) {
        fireMonitorEvent(zoneDialog.getClass().getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.ObsMonitor#configUpdate(com.raytheon.uf.viz
     * .monitor.events.IMonitorConfigurationEvent)
     */
    @Override
    public void configUpdate(IMonitorConfigurationEvent me) {
        fogConfig = (FSSObsMonitorConfigurationManager) me.getSource();
        updateMonitoringArea();
        if (zoneDialog != null && !zoneDialog.isDisposed()) {
            zoneDialog.refreshZoneTableData(obData);
            fireMonitorEvent(zoneDialog.getClass().getName());
        }
    }

    /**
     * Kills this monitor by nullifying the monitor's private instance variable.
     */
    @Override
    public void nullifyMonitor() {
        if (zoneDialog != null) {
            monitor.removeMonitorListener(zoneDialog);
        }
        ProductAlertObserver.removeObserver(OBS, this);
        monitor = null;
    }

    /**
     * Finds the zone based on the icao passed into it
     * 
     * @param icao
     * @return zone
     */
    public String findZone(String icao) {
        for (String zone : MonitoringArea.getPlatformMap().keySet()) {
            if (MonitoringArea.getPlatformMap().get(zone).contains(icao)) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Gets the main map
     * 
     * @return obsData
     */
    public ObsData getTableData() {
        return obsData;
    }

    /**
     * This method processes the incoming messages
     * 
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.monitor.ObsMonitor#process(com.raytheon.uf.viz.monitor.data.ObReport)
     */
    @Override
    protected void process(ObReport result) throws Exception {

        if (zoneDialog != null && !zoneDialog.isDisposed()) {

            obData.addReport(result);
            String zone = findZone(result.getPlatformId());
            if (zone != null) {
                AreaContainer ac = getTableData().getArea(zone);
                if (ac != null) {
                    ac.addReport(result.getObservationTime(), result);
                    fireMonitorEvent(this);
                }
            }
        } else {
            if (monitor != null) {
                monitor.nullifyMonitor();
            }
        }
    }

    /**
     * Gets the station Table Data.
     * 
     * @return stationTableData
     */
    public TableData getStationTableData() {
        return stationTableData;
    }

    /**
     * Launches the zone table dialog
     * 
     * @param type
     * @param shell
     */
    public void launchDialog(String type, Shell shell) {
        if (type.equals("zone")) {
            if (zoneDialog == null) {
                zoneDialog = new FogZoneTableDlg(shell, obData);
                addMonitorListener(zoneDialog);
                zoneDialog.addMonitorControlListener(this);
                fireMonitorEvent(zoneDialog.getClass().getName());
            }
            zoneDialog.open();
        } else if (type.equals("area")) {
            if (areaDialog == null) {
                areaDialog = new FogMonitoringAreaConfigDlg(shell,
                        "Fog Monitor Area Configuration");
                areaDialog.setCloseCallback(new ICloseCallback() {

                    @Override
                    public void dialogClosed(Object returnValue) {
                        areaDialog = null;
                    }

                });
            }
            areaDialog.open();
        }
    }

    /**
     * Sets the algorithm threat by time and zone
     * 
     * @param time
     * @param algData
     */
    public void setAlgorithmData(Date time, Map<String, FOG_THREAT> algData) {
        if (algorithmData.containsKey(time)) {
            algorithmData.remove(time);
        }
        algorithmData.put(time, algData);
        updateDialogTime(time);
    }

    /**
     * Gets algorithm threat by time
     * 
     * @param time
     * @return algData
     */
    public Map<String, FOG_THREAT> getAlgorithmData(Date time) {
        Map<String, FOG_THREAT> algData = new HashMap<String, FOG_THREAT>();

        if (algorithmData.containsKey(time)) {
            algData = algorithmData.get(time);
        } else {
            // by default is nothing in the ALG column
            for (String zone : MonitoringArea.getPlatformMap().keySet()) {
                algData.put(zone, FOG_THREAT.GRAY);
            }
        }
        return algData;
    }

    /**
     * Gets the monitoring geometries
     * 
     * @return zoneGeometries
     */
    public Map<String, Geometry> getMonitoringAreaGeometries() {

        if (zoneGeometries == null) {

            List<String> zones = fogConfig.getAreaList();
            zoneGeometries = new HashMap<String, Geometry>();

            for (String zone : zones) {
                try {
                    zoneGeometries.put(zone,
                            MonitorAreaUtils.getZoneGeometry(zone));
                } catch (Exception e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error get Monitoring Area Config", e);
                }
            }
        }

        return zoneGeometries;
    }

    /**
     * Gets observation data
     * 
     * @return
     */
    public ObMultiHrsReports getObData() {
        return obData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.monitor.fog.listeners.IFogResourceListener#
     * algorithmUpdate()
     */
    @Override
    public void algorithmUpdate() {

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                Iterator<IFogResourceListener> iter = fogResources.iterator();

                while (iter.hasNext()) {
                    IFogResourceListener listener = iter.next();
                    listener.algorithmUpdate();
                }
            }
        });
    }

    /**
     * Adds a listener
     * 
     * @param ifru
     */
    public void addFogResourceListener(IFogResourceListener ifru) {
        fogResources.add(ifru);
    }

    /**
     * Removes a listener
     * 
     * @param ifru
     *            Fog Resource Listener
     */
    public void removeFogResourceListener(IFogResourceListener ifru) {
        fogResources.remove(ifru);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.fog.listeners.IFogResourceListener#closeDialog
     * ()
     */
    @Override
    public void closeDialog() {
        if (zoneDialog != null) {
            monitor.removeMonitorListener(zoneDialog);
            monitor.nullifyMonitor();

            zoneDialog.close();
            zoneDialog = null;
        }

        if (areaDialog != null) {
            areaDialog.close();
            areaDialog = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.IMonitor#getTimeOrderedKeys(com.raytheon.
     * uf.viz.monitor.IMonitor, java.lang.String)
     */
    @Override
    public ArrayList<Date> getTimeOrderedKeys(IMonitor monitor, String type) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.monitor.ObsMonitor#processAtStartup(com.raytheon.
     * uf.viz.monitor.data.ObReport)
     */
    @Override
    protected void processAtStartup(ObReport report) {
        obData.addReport(report);
    }

    /**
     * Gets Fog zone table dialog
     * 
     * @return zoneDialog
     */
    public FogZoneTableDlg getZoneDialog() {
        return zoneDialog;
    }

    /**
     * Reads Table Configuration.
     * 
     * Method that reads the table configuration and updates the zone monitor
     * threshold map
     * 
     */
    private void updateMonitoringArea() {
        Map<String, List<String>> zones = new HashMap<String, List<String>>();
        // create zones and station list
        for (String zone : fogConfig.getAreaList()) {
            List<String> stations = fogConfig.getAreaStations(zone);
            zones.put(zone, stations);
        }
        MonitoringArea.setPlatformMap(zones);
    }
}
