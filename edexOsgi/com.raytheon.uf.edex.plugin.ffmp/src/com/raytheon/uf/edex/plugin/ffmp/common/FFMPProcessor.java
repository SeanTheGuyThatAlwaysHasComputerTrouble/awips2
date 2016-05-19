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
package com.raytheon.uf.edex.plugin.ffmp.common;

import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.dataplugin.exception.MalformedDataException;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPBasin;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPBasinData;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPDataContainer;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPRecord;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPTemplates;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPUtils;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPVirtualGageBasin;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPVirtualGageBasinMetaData;
import com.raytheon.uf.common.dataplugin.ffmp.SourceBin;
import com.raytheon.uf.common.dataplugin.ffmp.SourceBinEntry;
import com.raytheon.uf.common.dataplugin.ffmp.SourceBinList;
import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataplugin.radar.RadarRecord;
import com.raytheon.uf.common.dataplugin.radar.util.RadarConstants.DHRValues;
import com.raytheon.uf.common.dataplugin.radar.util.RadarDataInterrogator;
import com.raytheon.uf.common.dataplugin.radar.util.RadarRecordUtil;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.ReferencedObject.Type;
import com.raytheon.uf.common.hydro.spatial.HRAPCoordinates;
import com.raytheon.uf.common.hydro.spatial.HRAPSubGrid;
import com.raytheon.uf.common.monitor.config.FFMPSourceConfigurationManager;
import com.raytheon.uf.common.monitor.config.FFMPSourceConfigurationManager.SOURCE_TYPE;
import com.raytheon.uf.common.monitor.processing.IMonitorProcessing;
import com.raytheon.uf.common.monitor.scan.ScanUtils;
import com.raytheon.uf.common.monitor.xml.DomainXML;
import com.raytheon.uf.common.monitor.xml.FFTISettingXML;
import com.raytheon.uf.common.monitor.xml.FFTISourceXML;
import com.raytheon.uf.common.monitor.xml.ProductRunXML;
import com.raytheon.uf.common.monitor.xml.ProductXML;
import com.raytheon.uf.common.monitor.xml.SourceXML;
import com.raytheon.uf.common.mpe.util.XmrgFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.database.cluster.ClusterLockUtils;
import com.raytheon.uf.edex.database.cluster.ClusterLockUtils.LockState;
import com.raytheon.uf.edex.database.cluster.ClusterTask;
import com.raytheon.uf.edex.plugin.ffmp.FFMPGenerator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * FFMP Processor
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 * 07/14/09      2152       D. Hladky   Initial release
 * 10/25/12     DR 15514    G. Zhang    Fix ConcurrentModificationException
 * 02/01/13     1569        D. Hladky   Added constants
 * 02/25/13     1660        D. Hladky   FFTI design change to help mosaic processing.
 * 05/01/2013   15684       zhao        Unlock when Exception caught
 * Jul 15, 2013 2184        dhladky     Remove all HUC's for storage except ALL
 * 09/03/2013   DR 13083    G. Zhang    Added a fix in processRADAR(ArrayList<SourceBinEntry>).
 * 03 April 2014 2940       dhladky     Better error message for bad configurations.
 * Apr 15, 2014  3026       mpduff      Set the xmrg filename into the metadata column.
 * </pre>
 * @author dhladky
 * @version 1
 */
public class FFMPProcessor {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(FFMPProcessor.class);

    private final FFMPConfig config;

    private final FFMPTemplates template;

    private final FFMPGenerator generator;

    /** HRAP subGrid **/
    private HRAPSubGrid hrapgrid = null;

    private final DecimalFormat df = new DecimalFormat();

    private IMonitorProcessing imp = null;

    private XmrgFile xmrg = null;

    private short[][] xmrgData = null;

    private RadarRecord radarRec = null;

    private FFMPRecord ffmpRec = null;

    private GridRecord gribRec = null;

    private float[] gribData = null;

    private SourceXML source = null;

    private String dataKey = null;

    // only used by FFG and XMRG(HPE)
    private String siteKey = null;

    private String sourceId = null;

    private SourceBinList sbl = null;

    private boolean isSBL = false;

    private Geometry[][] pointGeometries = null;

    private Map<Long, Geometry> cwaGeometries = null;

    /** FFG geometry **/
    private int ffgNx = 0;

    /** FFG geometry **/
    private int ffgNy = 0;

    /** FFG geometry **/
    private GridGeometry2D ffgGeometry = null;

    /** the DHR values */
    private Map<DHRValues, Double> dhrMap = null;

    private GridGeometry2D radarGeometry = null;

    private Rectangle extent = null;

    private static final String sourceBinTaskName = "FFMP Source bin";

    private boolean isFFTI = false;
    
    private List<String> fftiAttribute = new ArrayList<String>();

    private FFTISourceXML fftiSource = null;

    private RadarDataInterrogator rdi = null;

    /**
     * Public constructor
     * 
     * @param config
     */
    public FFMPProcessor(FFMPConfig config, FFMPGenerator generator,
            FFMPRecord ffmpRec, FFMPTemplates template) {
        this.config = config;
        this.ffmpRec = ffmpRec;
        this.template = template;
        this.generator = generator;
        this.siteKey = ffmpRec.getSiteKey();
        this.dataKey = ffmpRec.getDataKey();
        df.setMaximumFractionDigits(2);
    }

    /**
     * Process data files for the FFMP
     */
    public FFMPRecord processFFMP(SourceXML source) throws Exception {
        // get the data key and the source to which this dataKey belongs
        this.source = source;
        long time = System.currentTimeMillis();

        // check if source is an FFTI source
        setFFTI();

        if (source.getSourceType().equals(
                FFMPSourceConfigurationManager.SOURCE_TYPE.GUIDANCE
                        .getSourceType())) {
            processGuidances();
        } else if (source.getSourceType()
                .equals(FFMPSourceConfigurationManager.SOURCE_TYPE.GAGE
                        .getSourceType())) {
            processVirtualGageBasins();
        } else {
            processSource();
        }

        statusHandler.handle(Priority.INFO,
                "Processed Source: " + source.getSourceName() + " sitekey: "
                        + siteKey + " dataKey: " + dataKey + " time: "
                        + (System.currentTimeMillis() - time));

        return ffmpRec;
    }

    /**
     * Process source
     * 
     * @throws Exception
     */
    private void processSource() throws Exception {
        // process source
        try {
            FFMPSourceConfigurationManager.DATA_TYPE type = FFMPSourceConfigurationManager
                    .getInstance().getDataType(source.getDataType());

            Date recdate = null;

            if (type == FFMPSourceConfigurationManager.DATA_TYPE.RADAR) {
                try {
                    radarRec = (RadarRecord) config.getSourceData(
                            source.getSourceName()).get(dataKey);

                    if (radarRec.getMnemonic().equals("DHR")) {
                        dhrMap = RadarRecordUtil.getDHRValues(radarRec);
                        statusHandler.handle(Priority.INFO, "DHR Bias: "
                                + dhrMap.get(DHRValues.BIAS_TO_USE));
                        statusHandler.handle(Priority.INFO, "DHR HailCap: "
                                + dhrMap.get(DHRValues.MAXPRECIPRATEALLOW));
                    }

                    recdate = radarRec.getDataTime().getRefTime();
                } catch (Exception e) {
                    fireBadConfigMessage(type, e);
                    return;
                }

            } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.XMRG) {
                try {
                    xmrg = (XmrgFile) config.getSourceData(
                            source.getSourceName()).get(dataKey);
                    xmrgData = getXMRGData();
                    ffmpRec.setMetaData(xmrg.getFile().getName());
                } catch (Exception e) {
                    fireBadConfigMessage(type, e);
                    return;
                }

                if (xmrg.getHeader().getValidDate().getTime() > 0l) {
                    recdate = xmrg.getHeader().getValidDate();
                } else {
                    if (source.getDateFormat() != null) {
                        SimpleDateFormat formatter = new SimpleDateFormat(
                                source.getDateFormat());
                        int length = source.getDateFormat().length();

                        String dateString = xmrg
                                .getFile()
                                .getName()
                                .substring(
                                        (xmrg.getFile().getName().length() - 1)
                                                - length,
                                        (xmrg.getFile().getName().length() - 1));

                        recdate = formatter.parse(dateString);
                    } else {
                        statusHandler.handle(Priority.ERROR, "Source: "
                                + ffmpRec.getSourceName() + " sitekey: "
                                + siteKey + " File: "
                                + xmrg.getFile().getName()
                                + " : Invalid date header");
                        return;
                    }
                }

            } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.PDO) {
                try {
                    imp = (IMonitorProcessing) config.getSourceData(
                            source.getSourceName()).get(dataKey);
                    recdate = imp.getDataTime().getRefTime();
                } catch (Exception e) {
                    fireBadConfigMessage(type, e);
                    return;
                }

            } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.GRID) {
                try {
                    gribRec = (GridRecord) config.getSourceData(
                            source.getSourceName()).get(dataKey);
                    gribData = config.getGribData(gribRec);
                    recdate = gribRec.getDataTime().getRefTime();
                } catch (Exception e) {
                    fireBadConfigMessage(type, e);
                    return;
                }
            }

            statusHandler.handle(
                    Priority.INFO,
                    "Source Expiration: "
                            + source.getExpirationMinutes(siteKey));

            // set the time for this record
            ffmpRec.setDataTime(new DataTime(recdate));

            // process each domain separately, but within the same URI/HDF5
            // named by primary domain
            for (DomainXML domain : template.getDomains()) {

                // reset the geometries
                cwaGeometries = null;

                try {

                    LinkedHashMap<Long, ?> map = template.getMap(siteKey,
                            domain.getCwa(), FFMPRecord.ALL);

                    // this means the data is outside your domain
                    if (map.keySet().size() > 0) {

                        isSBL = false;
                        sbl = null;

                        if (type == FFMPSourceConfigurationManager.DATA_TYPE.RADAR) {
                            // if bin list dosen't exist create it for future
                            // use
                            // when
                            // running
                            this.sourceId = source.getSourceName() + "-"
                                    + domain.getCwa() + "-" + dataKey;

                            if (sourceId != null) {
                                if (generator.isExistingSourceBin(sourceId)) {
                                    sbl = generator.getSourceBinList(sourceId);
                                    isSBL = true;
                                } else {
                                    if (checkLockStatus()) {
                                        lock();
                                        if (cwaGeometries == null) {
                                            cwaGeometries = template
                                                    .getRawGeometries(dataKey,
                                                            domain.getCwa());
                                        }
                                        //DR15684
                                        try {
                                            sbl = (new RadarSBLGenerator(config)
                                                    .generate(sourceId,
                                                            map.keySet(),
                                                            cwaGeometries, radarRec));
                                        } catch (Exception e) {
                                            statusHandler.handle(Priority.WARN, "caught an Exception while generating Source Bin List");
                                            if (!checkLockStatus()) {
                                                ClusterLockUtils.unlock(sourceBinTaskName, sourceId);
                                            }
                                        }
                                        if (sbl != null) {
                                            generator.setSourceBinList(sbl);
                                            isSBL = true;
                                        }

                                    } else {
                                        continue;
                                    }
                                }
                            } else {
                                ffmpRec = null;
                                return;
                            }
                        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.XMRG) {
                            // all the HPE source use a t HRAP grid so, create
                            // it only once
                            this.sourceId = FFMPSourceConfigurationManager.DATA_TYPE.XMRG
                                    .getDataType()
                                    + "-"
                                    + domain.getCwa()
                                    + "-" + dataKey;

                            if (sourceId != null) {
                                if (generator.isExistingSourceBin(sourceId)) {
                                    sbl = generator.getSourceBinList(sourceId);
                                    isSBL = true;
                                } else {
                                    if (checkLockStatus()) {
                                        lock();
                                        sbl = new SourceBinList(sourceId);
                                        setXMRGPointCache(extent);
                                    } else {
                                        continue;
                                    }
                                }
                            } else {
                                ffmpRec = null;
                                return;
                            }
                        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.PDO) {
                            // if bin list dosen't exist create it for future
                            // use
                            // when
                            // running
                            this.sourceId = source.getSourceName() + "-"
                                    + domain.getCwa() + "-" + dataKey;

                            if (sourceId != null) {
                                if (generator.isExistingSourceBin(sourceId)) {
                                    sbl = generator.getSourceBinList(sourceId);
                                    isSBL = true;
                                } else {
                                    if (checkLockStatus()) {
                                        lock();
                                        sbl = new SourceBinList(sourceId);
                                        setPDOPointCache();
                                    } else {
                                        continue;
                                    }
                                }
                            } else {
                                ffmpRec = null;
                                return;
                            }
                        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.GRID) {
                            // if bin list dosen't exist create it for future
                            // use
                            // when
                            // running
                            this.sourceId = source.getSourceName() + "-"
                                    + domain.getCwa() + "-" + dataKey;

                            if (sourceId != null) {
                                if (generator.isExistingSourceBin(sourceId)) {
                                    sbl = generator.getSourceBinList(sourceId);
                                    isSBL = true;
                                } else {
                                    if (checkLockStatus()) {
                                        lock();
                                        sbl = new SourceBinList(sourceId);
                                        setGridPointCache();
                                    } else {
                                        continue;
                                    }
                                }
                            } else {
                                ffmpRec = null;
                                return;
                            }
                        }

                        if (sourceId != null) {
                            for (Long key : map.keySet()) {

                                FFMPBasin basin = getBasin(key);
                                Date date = null;
                                Float val = null;

                                if (type == FFMPSourceConfigurationManager.DATA_TYPE.XMRG) {
                                    date = xmrg.getHeader().getValidDate();
                                    val = processXMRG(key, domain.getCwa());
                                } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.RADAR) {
                                    date = radarRec.getDataTime().getRefTime();
                                    val = processRADAR(key, domain.getCwa());
                                } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.PDO) {
                                    date = imp.getDataTime().getRefTime();
                                    val = processPDO(key, domain.getCwa());
                                } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.GRID) {
                                    date = gribRec.getDataTime().getRefTime();
                                    val = processGrib(key, domain.getCwa());
                                }

                                basin.setValue(date, val);
                            }

                            if (!isSBL && (sbl != null)) {
                                generator.setSourceBinList(sbl);
                            }
                        } else {
                            ffmpRec = null;
                            return;
                        }

                    } else {
                        if (ffmpRec != null) {
                            statusHandler.handle(Priority.DEBUG, "Source: "
                                    + ffmpRec.getSourceName() + " sitekey: "
                                    + siteKey + " domain: " + domain.getCwa()
                                    + " : Data outside of domain");
                        }
                    }
                } catch (Exception e) {
                    throw new Exception(
                            "FFMPProcessor: Failed to process source domain: "
                                    + source.getSourceName() + ": "
                                    + domain.getCwa(), e);
                } finally {
                    if (sourceId != null) {
                        if (!checkLockStatus()) {
                            ClusterLockUtils
                                    .unlock(sourceBinTaskName, sourceId);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new Exception("FFMPProcessor: Failed to process source: "
                    + source.getSourceName(), e);
        }
    }

    /**
     * Process the Guidance types
     * 
     * @throws Exception
     */
    private void processGuidances() throws Exception {
        // process Guidance sources

        try {

            gribRec = (GridRecord) config.getSourceData(source.getSourceName())
                    .get(dataKey);
            setGridGeometry(gribRec);
            gribData = config.getGribData(gribRec);
            Date recdate = gribRec.getDataTime().getRefTime();
            ffmpRec.setDataTime(new DataTime(recdate));

            // process each domain separately, but within the same URI/HDF5
            // named by primary domain

            ArrayList<DomainXML> domains = template.getDomains();

            synchronized (domains) {
                for (DomainXML domain : domains) {

                    // rest the cwa geometries
                    cwaGeometries = null;

                    try {
                        LinkedHashMap<Long, ?> map = template.getMap(siteKey,
                                domain.getCwa(), "ALL");

                        // outside my domain?
                        if (map.keySet().size() > 0) {

                            String sourceFamilyName = source.getDisplayName();
                            this.sourceId = sourceFamilyName + "-"
                                    + domain.getCwa() + "-" + dataKey + "-"
                                    + siteKey;

                            if (sourceId != null) {
                                isSBL = false;
                                sbl = null;
                                // if bin list dosen't exist
                                // create it for future use
                                // when running
                                if (generator.isExistingSourceBin(sourceId)) {
                                    sbl = generator.getSourceBinList(sourceId);
                                    isSBL = true;
                                } else {
                                    if (checkLockStatus()) {
                                        lock();
                                        sbl = new SourceBinList(sourceId);
                                        setGridPointCache();
                                    } else {
                                        return;
                                    }
                                }
                            } else {
                                ffmpRec = null;
                                return;
                            }

                            for (Long key : map.keySet()) {

                                FFMPBasin basin = getBasin(key);
                                float val = 0.0f;
                                val = processGrib(key, domain.getCwa());
                                setBasin(basin, val);
                            }

                            if (!isSBL && (sbl != null)) {
                                generator.setSourceBinList(sbl);
                            }
                        } else {
                            statusHandler.handle(Priority.INFO, "Source: "
                                    + ffmpRec.getSourceName() + " sitekey: "
                                    + siteKey + " domain: " + domain.getCwa()
                                    + " : Data outside of domain");
                        }
                    }

                    catch (Exception e) {
                        ffmpRec = null;
                        throw new Exception(
                                "FFMPProcessor: Failed to process source domain: "
                                        + source.getSourceName() + ": "
                                        + domain.getCwa());
                    } finally {
                        if (sourceId != null) {
                            if (!checkLockStatus()) {
                                ClusterLockUtils.unlock(sourceBinTaskName,
                                        sourceId);
                            }
                        }
                    }
                }
            }

            if (source.getInterpolatedGuidanceDelay()) {
                try {
                    // FFG , we still use the display name for it
                    String sourceNameString = null;

                    if (source.isMosaic()) {
                        sourceNameString = source.getDisplayName();
                    } else {
                        sourceNameString = ffmpRec.getSiteKey() + "-"
                                + source.getDisplayName();
                    }

                    Date backDate = new Date(ffmpRec.getDataTime().getRefTime()
                            .getTime()-(FFMPGenerator.SOURCE_CACHE_TIME * TimeUtil.MILLIS_PER_HOUR));
    
                    FFMPDataContainer ffgContainer = generator
                            .getFFMPDataContainer(sourceNameString, 
                                    backDate);

                    if (ffgContainer != null
                            && ffgContainer.containsKey(source.getSourceName())) {

                        Date previousDate = ffgContainer.getNewest();

                        if (previousDate != null) {

                            long guidFrequency = ffmpRec.getDataTime()
                                    .getRefTime().getTime()
                                    - previousDate.getTime();

                            // used reverse logic from AWIPS I code here,
                            // instead of
                            // returning
                            // I switched the greater than and less than so it
                            // will
                            // process
                            if (guidFrequency < (FFMPGenerator.SOURCE_CACHE_TIME * TimeUtil.MILLIS_PER_HOUR)
                                    && guidFrequency >= (TimeUtil.MILLIS_PER_HOUR)) {

                                long newTime = recdate.getTime()
                                        + (int) (source.getDurationHour() * TimeUtil.MILLIS_PER_HOUR);
                                // this is the new date
                                recdate = new Date(newTime);
                                ProductRunXML productRunner = generator
                                        .getRunConfig().getProduct(siteKey);
                                ProductXML product = generator
                                        .getSourceConfig().getProduct(
                                                productRunner.getProductName());
                                SourceXML qpeSource = generator
                                        .getSourceConfig().getSource(
                                                product.getQpe());
                                // populate previous rec

                                FFMPInterpolatedGuidanceDelay figd = new FFMPInterpolatedGuidanceDelay(
                                        siteKey, guidFrequency, source,
                                        qpeSource, previousDate, recdate,
                                        generator,
                                        ffgContainer.getBasinData(),
                                        ffmpRec);
                                
                                boolean delayGuidance = figd
                                        .calculateDelayedGuidance();
                                // sets the new data time for the record
                                if (delayGuidance) {
                                    ffmpRec.setDataTime(new DataTime(recdate));
                                }

                            }
                        }
                    }

                } catch (Exception e) {
                    throw new Exception(
                            "FFMPProcessor: Failed to Guidance Transition Delay source "
                                    + source.getSourceName());
                }
            }

        } catch (Exception e) {
            ffmpRec = null;
            throw new Exception("FFMPProcessor: Failed to process source: "
                    + source.getSourceName());
        }
    }

    /**
     * Process the Virtual Gage Basins
     */
    private void processVirtualGageBasins() {
        FFMPSourceConfigurationManager.DATA_TYPE type = FFMPSourceConfigurationManager
                .getInstance().getDataType(source.getDataType());

        Date recdate = null;

        if (type == FFMPSourceConfigurationManager.DATA_TYPE.RADAR) {
            try {
                radarRec = (RadarRecord) config.getSourceData(
                        source.getSourceName()).get(dataKey);
                if (radarRec.getMnemonic().equals("DHR")) {
                    dhrMap = RadarRecordUtil.getDHRValues(radarRec);
                }
                recdate = radarRec.getDataTime().getRefTime();
            } catch (Exception e) {
                fireBadConfigMessage(type, e);
                return;
            }

        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.XMRG) {
            try {
                xmrg = (XmrgFile) config.getSourceData(source.getSourceName()).get(
                        dataKey);
                xmrgData = getXMRGData();
                recdate = xmrg.getHeader().getValidDate();
            } catch (Exception e) {
                fireBadConfigMessage(type, e);
                return;
            }
        }

        // set the time
        ffmpRec.setDataTime(new DataTime(recdate));

        // process each domain separately, but within the same URI/HDF5
        // named by primary domain
        for (DomainXML domain : template.getDomains()) {

            LinkedHashMap<String, FFMPVirtualGageBasinMetaData> vmap = template
                    .getVirtualGageBasins(siteKey, domain.getCwa());

            for (Entry<String, FFMPVirtualGageBasinMetaData> entry : vmap
                    .entrySet()) {
                try {
                    FFMPVirtualGageBasinMetaData fvgbmd = entry.getValue();

                    if (fvgbmd != null) {

                        FFMPVirtualGageBasin basin = getVirtualBasin(
                                fvgbmd.getLid(), fvgbmd.getLookupId(), "ALL");

                        Date date = null;
                        Float val = null;

                        if (type == FFMPSourceConfigurationManager.DATA_TYPE.XMRG) {
                            date = xmrg.getHeader().getValidDate();
                            val = processXMRG(fvgbmd.getCoordinate(), 1.0);
                        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.RADAR) {
                            date = radarRec.getDataTime().getRefTime();
                            SourceBinEntry sbe = new SourceBinEntry();
                            sbe.setCoor(fvgbmd.getCoordinate());
                            sbe.setArea(1.0);
                            ArrayList<SourceBinEntry> sourceBins = new ArrayList<SourceBinEntry>();
                            sourceBins.add(sbe);
                            val = processRADAR(sourceBins);
                        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.PDO) {
                            date = imp.getDataTime().getRefTime();
                            ReferencedCoordinate rc = new ReferencedCoordinate(
                                    fvgbmd.getCoordinate());
                            Coordinate coor = null;
                            try {
                                coor = rc.asGridCell(imp.getGridGeometry(),
                                        PixelInCell.CELL_CENTER);
                            } catch (TransformException e) {
                                statusHandler.error("VGB PDO transform error!", e);
                                continue;
                            } catch (FactoryException e) {
                                statusHandler.error("VGB PDO factory error!", e);
                                continue;
                            }
                            val = processPDO(coor, 1.0);
                        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.GRID) {
                            date = gribRec.getDataTime().getRefTime();
                            val = processGrib(fvgbmd.getCoordinate(), 1.0);
                        }

                        basin.setValue(date, val);
                    }

                } catch (Exception e) {
                    ffmpRec = null;
                    statusHandler.error("Unable to process VGB: "+type, e);
                }
            }
        }
    }

    /**
     * Setting all of the data values to create FFMPRecord
     * 
     * @param ffmp
     */
    private FFMPBasinData getBasinData() {

        return ffmpRec.getBasinData();
    }

    /**
     * Gets the basins for data
     * 
     * @param pfaf
     * @param huc
     * @return
     */
    private FFMPBasin getBasin(Long pfaf) {
        FFMPBasin basin = getBasinData().get(pfaf);
        if (basin == null) {
            basin = new FFMPBasin(pfaf, false);
            getBasinData().put(pfaf, basin);
        }
        return basin;
    }

    /**
     * Gets the basins for data
     * 
     * @param pfaf
     * @param huc
     * @return
     */
    private FFMPVirtualGageBasin getVirtualBasin(String lid, Long pfaf,
            String huc) {
        FFMPVirtualGageBasin basin = (FFMPVirtualGageBasin) getBasinData()
                .get(pfaf);
        if (basin == null) {
            basin = new FFMPVirtualGageBasin(lid, pfaf, false);
            getBasinData().put(pfaf, basin);
        }
        return basin;
    }

    /**
     * Process the PDO for a geometry
     * 
     * @param pfaf
     * @param geo
     * @return
     */
    private float processPDO(Long pfaf, String cwa) throws Exception {

        ArrayList<SourceBinEntry> entries = null;
        float arealWeight = 0;
        float val = 0.0f;

        if (isSBL) {
            SourceBin bin = sbl.getMap(pfaf);
            if (bin != null) {
                entries = bin.getEntries();
            }
        } else {

            if (cwaGeometries == null) {
                cwaGeometries = template.getRawGeometries(dataKey, cwa);
            }

            Geometry geo = cwaGeometries.get(pfaf);

            if (geo != null) {
                ArrayList<SourceBinEntry> newPoints = new ArrayList<SourceBinEntry>();
                ReferencedCoordinate rc = new ReferencedCoordinate(
                        geo.getCoordinate());
                Coordinate center = null;
                try {
                    center = rc.asGridCell(imp.getGridGeometry(),
                            PixelInCell.CELL_CENTER);
                } catch (TransformException e) {
                    statusHandler.error("PDO transform error!", e);
                    throw new Exception(e);
                } catch (FactoryException e) {
                    statusHandler.error("PDO factory error!", e);
                    throw new Exception(e);
                }

                if ((center.x >= 0) && (center.x < imp.getNx())
                        && (center.y >= 0) && (center.y < imp.getNy())) {
                    // add the center one at least
                    SourceBinEntry sbe = new SourceBinEntry();
                    sbe.setCoor(center);
                    sbe.setArea(1.0);
                    newPoints.add(sbe);
                    // keep processing nest's until exhausted
                    int p = 1;
                    while (p > 0) {

                        ArrayList<SourceBinEntry> nestPoints = processNest(geo,
                                p, imp.getNx(), imp.getNy(), (int) center.x,
                                (int) center.y, false);

                        if (nestPoints != null && nestPoints.size() > 0) {
                            p++;
                            // nestPoints are already in grid coordinates
                            newPoints.addAll(nestPoints);
                        } else {
                            p = 0;
                        }
                    }

                    SourceBin bin = new SourceBin(newPoints);
                    entries = bin.getEntries();
                    sbl.addBin(pfaf, bin);
                }
            }
        }

        if (entries != null) {
            // process the values for the points
            for (SourceBinEntry sbe : entries) {
                val += processPDO(sbe.getCoor(), sbe.getArea());
                arealWeight += sbe.getArea();
            }
        }

        return (val / arealWeight);
    }

    /**
     * Process the PDO for a geometry
     * 
     * @param geo
     * @return
     */
    private float processPDO(Coordinate coor, double area) {

        double val = 0.0f;

        if (((int) coor.x >= 0) && (coor.x < imp.getNx())
                && ((int) coor.y >= 0) && ((int) coor.y < imp.getNy())) {
            val = (imp.getDataArray()[(imp.getNx() * (int) coor.y)
                    + (int) coor.x] * source.getConversion(siteKey));

            val = (val * area);
        }

        return (float) val;
    }

    /**
     * Process XMRG at a basin
     * 
     * @param pfaf
     * @param geo
     * @return
     */
    private float processXMRG(Long pfaf, String cwa) throws Exception {

        ArrayList<SourceBinEntry> entries = null;
        float arealWeight = 0.0f;
        float val = 0.0f;

        if (isSBL) {
            SourceBin bin = sbl.getMap(pfaf);
            if (bin != null) {
                entries = bin.getEntries();
            }
        } else {

            if (cwaGeometries == null) {
                cwaGeometries = template.getRawGeometries(dataKey, cwa);
            }

            Geometry geo = cwaGeometries.get(pfaf);

            if (geo != null) {
                ArrayList<SourceBinEntry> newPoints = new ArrayList<SourceBinEntry>();
                ReferencedCoordinate rc = new ReferencedCoordinate(geo
                        .getCentroid().getCoordinate());
                Coordinate center = null;
                try {
                    center = rc.asGridCell(getHRAPSubGrid().getHRAP()
                            .getGridGeometry(), PixelInCell.CELL_CENTER);

                } catch (TransformException e) {
                    statusHandler.error("SBL Transform exception: ", e);
                    throw new Exception(e);
                } catch (FactoryException e) {
                    statusHandler.error("SBL Factory exception: ", e);
                    throw new Exception(e);
                } catch (Exception e) {
                    statusHandler.error("SBL General exception: ", e);
                    throw new Exception(e);
                }

                double xx = center.x - hrapgrid.getExtent().x;
                double yy = center.y - hrapgrid.getExtent().y;

                if (((int) xx >= 0) && ((int) xx < xmrgData[0].length)
                        && ((int) yy >= 0) && ((int) yy < xmrgData[1].length)) {

                    ArrayList<SourceBinEntry> nestPoints = null;
                    // add the center one at least
                    SourceBinEntry sbe = new SourceBinEntry();
                    Geometry ptGeo = null;
                    ptGeo = pointGeometries[(int) yy][(int) xx];
                    sbe.setCoor(ptGeo.getCentroid().getCoordinate());
                    sbe.setArea(1.0);
                    newPoints.add(sbe);
                    // keep processing nest's until exhausted
                    int p = 1;
                    while (p > 0) {

                        try {
                            nestPoints = processNest(geo, p, getHRAPSubGrid()
                                    .getNy(), getHRAPSubGrid().getNx(),
                                    (int) yy, (int) xx, true);

                            if (nestPoints != null && nestPoints.size() > 0) {
                                p++;
                                newPoints.addAll(nestPoints);
                            } else {
                                p = 0;
                            }
                        } catch (Exception e) {
                            p = 0;
                            statusHandler.handle(Priority.INFO, "Source: "
                                    + ffmpRec.getSourceName() + " sitekey: "
                                    + siteKey + " domain: " + cwa
                                    + " : Data outside of domain");
                        }
                    }

                    SourceBin bin = new SourceBin(newPoints);
                    entries = bin.getEntries();
                    sbl.addBin(pfaf, bin);
                }
            }
        }

        if (entries != null) {
            for (SourceBinEntry sbe : entries) {
                val += processXMRG(sbe.getCoor(), sbe.getArea());
                arealWeight += sbe.getArea();
            }
        }

        return (val / arealWeight);
    }

    /**
     * Process XMRG at a Coordinate
     * 
     * @param geo
     * @return
     */
    private float processXMRG(Coordinate coor, double area) {

        double val = 0.0;
        Coordinate gridCoor = getHRAPXY(coor);
        gridCoor.x = (int) (gridCoor.x + 0.5);
        gridCoor.y = (int) (gridCoor.y + 0.5);
        if ((gridCoor.x >= 0) && (gridCoor.x < xmrgData[0].length)
                && (gridCoor.y >= 0) && (gridCoor.y < xmrgData.length)) {

            if (xmrgData[(int) gridCoor.y][(int) gridCoor.x] != -899) {
                val = ((xmrgData[(int) gridCoor.y][(int) gridCoor.x]) * source
                        .getConversion(siteKey));
                val = val * area;
            }
        }

        return (float) val;
    }

    /**
     * Get average radar value for a given geometry
     * 
     * @param pfaf
     * @param geo
     * @return
     */
    private float processRADAR(Long pfaf, String cwa) {

        ArrayList<SourceBinEntry> entries = null;
        float val = 0.0f;

        if (isSBL) {
            SourceBin bin = sbl.getMap(pfaf);
            if (bin != null) {
                entries = bin.getEntries();
            }
        }

        if (entries != null) {
            val = processRADAR(entries);
        }

        return val;
    }

    /**
     * Get average radar value for a given coordinate
     * 
     * @param entries
     * @return
     */
    private float processRADAR(ArrayList<SourceBinEntry> entries) {

        double val = 0.0f;
        double area = 0.0f;

        if (rdi == null) {
            rdi = new RadarDataInterrogator(radarRec);
        }

        Coordinate[] coors = new Coordinate[entries.size()];
        double[] areas = new double[entries.size()];

        for (int i = 0; i < entries.size(); i++) {
            coors[i] = entries.get(i).getCoor();
            areas[i] = entries.get(i).getArea();
        }

        int[] dataVals = rdi.getDataValues(coors);

        if (radarRec.getMnemonic().equals("DHR")) {

            for (int j = 0; j < dataVals.length; j++) {
                try {
                    val += ScanUtils.getZRvalue2(dataVals[j],//fval,// DR 13083
                            dhrMap.get(DHRValues.ZRMULTCOEFF),
                            dhrMap.get(DHRValues.MAXPRECIPRATEALLOW),
                            dhrMap.get(DHRValues.ZRPOWERCOEFF),
                            dhrMap.get(DHRValues.BIAS_TO_USE))
                            * areas[j];
                    area += areas[j];
                } catch (Exception e) {
                    statusHandler
                            .error("DHR parameters are NULL, can't process!"
                                    + e.getMessage());
                }
            }

        } else if (radarRec.getMnemonic().equals("DPR")) {

            for (int j = 0; j < dataVals.length; j++) {

                float fval = 0.0f;

                if (dataVals[j] > 0) {

                    fval = ScanUtils.decodeDPRValue(dataVals[j]);
                    val += fval * areas[j];
                }

                val += fval * areas[j];
                area += areas[j];
            }
        }

        return (float) (val / area);
    }

    /**
     * Process the grib types
     * 
     * @param pfaf
     * @param geo
     * @return
     */
    private float processGrib(Long pfaf, String cwa) throws Exception {

        ArrayList<SourceBinEntry> entries = null;
        float arealWeight = 0.0f;
        float val = 0.0f;

        if (isSBL) {
            SourceBin bin = sbl.getMap(pfaf);
            if (bin != null) {
                entries = bin.getEntries();
            }
        } else {

            if (cwaGeometries == null) {
                cwaGeometries = template.getRawGeometries(siteKey, cwa);
            }

            Geometry geo = cwaGeometries.get(pfaf);

            if (geo != null) {
                ArrayList<SourceBinEntry> newPoints = new ArrayList<SourceBinEntry>();
                ReferencedCoordinate rc = new ReferencedCoordinate(geo
                        .getCentroid().getCoordinate());
                Coordinate center = null;
                try {
                    center = rc.asGridCell(getGridGeometry(),
                            PixelInCell.CELL_CENTER);
                } catch (TransformException e) {
                    statusHandler.handle(Priority.ERROR, "Error transforming pfaf! " +pfaf);
                    throw new Exception(e);
                } catch (FactoryException e) {
                    statusHandler.handle(Priority.ERROR, "Error in geometry! " +pfaf);
                    throw new Exception(e);
                }

                if (((int) center.x >= 0) && ((int) center.x < getNx())
                        && ((int) center.y >= 0) && ((int) center.y < getNy())) {
                    // add at least the center point
                    SourceBinEntry sbe = new SourceBinEntry();
                    sbe.setCoor(center);
                    sbe.setArea(1.0);
                    newPoints.add(sbe);
                    // keep processing nest's until exhausted
                    int p = 1;
                    while (p > 0) {

                        ArrayList<SourceBinEntry> nestPoints = processNest(geo,
                                p, getNx(), getNy(), (int) center.x,
                                (int) center.y, false);

                        if (nestPoints != null && nestPoints.size() > 0) {
                            newPoints.addAll(nestPoints);
                            p++;

                        } else {
                            p = 0;
                        }
                    }

                    SourceBin bin = new SourceBin(newPoints);
                    entries = bin.getEntries();
                    sbl.addBin(pfaf, bin);
                }
            }
        }

        if (entries != null) {
            // process the values for the points
            for (SourceBinEntry sbe : entries) {
                float thisVal = processGrib(sbe.getCoor(), sbe.getArea());
                if (thisVal != FFMPUtils.MISSING) {
                    val += thisVal;
                    arealWeight += sbe.getArea();
                }
            }
        }

        if (val == 0.0f) {
            return FFMPUtils.MISSING;
        }

        return (val / arealWeight);
    }

    /**
     * Process the grib coor
     * 
     * @param geo
     * @return
     */
    private float processGrib(Coordinate coor, double area) {

        double val = FFMPUtils.MISSING;

        if (((int) coor.x >= 0) && ((int) coor.x <= getNx())
                && ((int) coor.y >= 0) && ((int) coor.y <= getNy())) {
            int dataBin = (getNx() * (int) coor.y) + (int) coor.x;
            if (gribData[dataBin] > 0.0) {
                val = (gribData[dataBin] * source.getConversion(siteKey));
                val = val * area;
            }
        }

        return (float) val;
    }

    /**
     * Sets up the grid for HRAP's
     * 
     * @param rectangle
     */
    private void setHRAPSubGrid(Rectangle rectangle, int hrapGribFactor) {
        try {
            hrapgrid = new HRAPSubGrid(rectangle, hrapGribFactor);
        } catch (Exception e) {
            statusHandler.error("Cant load HRAP sub grid!", e);
        }
    }

    /**
     * The HRAP sub grid
     * 
     * @return
     */
    public HRAPSubGrid getHRAPSubGrid() {
        return hrapgrid;
    }

    /**
     * gets the XMRG local extents
     * 
     * @return
     */
    private Rectangle getExtents(int hrapGridFactor) {
        Rectangle rect = null;

        try {
            rect = HRAPCoordinates.getHRAPCoordinates();
            rect.setBounds(rect.x * hrapGridFactor, rect.y * hrapGridFactor,
                    rect.width * hrapGridFactor, rect.height * hrapGridFactor);
        } catch (Exception e) {
            statusHandler.error("Can't get HRAP extents! ", e);
        }
        return rect;
    }

    /**
     * Sets the Basin values
     * 
     * @param basin
     * @param val
     */
    private void setBasin(FFMPBasin basin, Float val) {

        basin.setValue(config.getDate(), val);
    }

    /**
     * Sets up the gridgeometry for FFG
     */
    public void setGridGeometry(GridRecord rec) {
        ffgNx = rec.getSpatialObject().getNx();
        ffgNy = rec.getSpatialObject().getNy();
        ffgGeometry = MapUtil.getGridGeometry(rec.getSpatialObject());
    }

    /**
     * Gets FFG geometry
     * 
     * @return
     */
    public GridGeometry2D getGridGeometry() {
        return ffgGeometry;
    }

    /**
     * Gets FFG nx
     * 
     * @return
     */
    public int getNx() {
        return ffgNx;
    }

    /**
     * Gets FFG ny
     * 
     * @return
     */
    public int getNy() {
        return ffgNy;
    }

    /**
     * Get HRAP coordinate
     * 
     * @param latLon
     * @return
     */
    private Coordinate getHRAPXY(Coordinate latLon) {
        Coordinate gridCell = null;
        try {
            ReferencedCoordinate rc = new ReferencedCoordinate(latLon);
            HRAPSubGrid subGrid = getHRAPSubGrid();
            Coordinate gridCell2 = rc.asGridCell(subGrid.getHRAP()
                    .getGridGeometry(), PixelInCell.CELL_CENTER);
            // gridCell is in terms of parent HRAP, need to modify by extent
            // Rectangle extent = subGrid.getExtent();
            // gridCell.x += extent.x;
            // gridCell.y += extent.y;
            int x = (int) gridCell2.x;
            int y = (int) gridCell2.y;

            x = x - hrapgrid.getExtent().x;
            y = (hrapgrid.getExtent().y + hrapgrid.getNy()) - y;

            gridCell = new Coordinate(x, y, 0.0);

        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, "Unable translate lat lon coordinate! " +latLon);
        }
        return gridCell;
    }

    /**
     * Gets the gribPoint
     * 
     * @param gridPoint
     * @return
     */
    private Coordinate getHRAPLatLon(Coordinate gridPoint) {
        try {
            ReferencedCoordinate rc = new ReferencedCoordinate(gridPoint,
                    getHRAPSubGrid().getHRAP().getGridGeometry(),
                    Type.GRID_CORNER);
            gridPoint = rc.asLatLon();
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, "Unable translate grid coordinate! " +gridPoint);
        }
        return gridPoint;
    }

    /**
     * Gets the Radar Geometry
     * 
     * @return
     * 
     * @return
     */
    public GridGeometry2D getRadarGeometry() {
        if (radarGeometry == null) {
            ProjectedCRS crs = radarRec.getCRS();
            GeneralEnvelope generalEnvelope = new GeneralEnvelope(2);
            generalEnvelope.setCoordinateReferenceSystem(crs);

            double maxExtent = radarRec.getGateResolution()
                    * radarRec.getNumBins()
                    * Math.cos(Math.toRadians(radarRec.getTrueElevationAngle()));

            generalEnvelope.setRange(0, -maxExtent, maxExtent);
            generalEnvelope.setRange(1, -maxExtent, maxExtent);

            radarGeometry = new GridGeometry2D(new GeneralGridEnvelope(
                    new int[] { 0, 0 }, new int[] { radarRec.getNumRadials(),
                            radarRec.getNumBins() }, false), generalEnvelope);
        }
        return radarGeometry;
    }

    /**
     * Process gradually increasing nest looking for points that fall within the
     * geometry
     * 
     * @param geom
     * @param p
     * @param nx
     * @param ny
     * @param x
     * @param y
     * @param points
     * @return
     */
    private ArrayList<SourceBinEntry> processNest(Geometry geom, int p, int nx,
            int ny, int x, int y, boolean returnLatLon) {
        ArrayList<SourceBinEntry> myPoints = new ArrayList<SourceBinEntry>();

        try {
            for (int i = p * (-1); i <= p; i++) {
                int xx = x + i;
                // process entire row
                if (i == p * (-1) || i == p) {
                    for (int j = p * (-1); j <= p; j++) {
                        int yy = y + j;
                        if ((yy > ny - 1) || (xx > nx - 1) || (yy < 0)
                                || (xx < 0)) {
                            continue;
                        } else if (geom.contains(pointGeometries[xx][yy]
                                .getCentroid())) {
                            SourceBinEntry sbe = new SourceBinEntry();

                            if (returnLatLon) {
                                sbe.setCoor(pointGeometries[xx][yy]
                                        .getCentroid().getCoordinate());
                            } else {
                                sbe.setCoor(new Coordinate(xx, yy));
                                double arealPercent = getArealPercentage(
                                        pointGeometries[xx][yy], geom);
                                sbe.setArea(arealPercent);
                                myPoints.add(sbe);
                            }
                        }
                    }
                }
                // process only book ends
                else {
                    for (int j = p * (-1); j <= p; j++) {
                        int yy = y + j;
                        if (yy == y - p || yy == y + p) {
                            if ((yy > ny - 1) || (xx > nx - 1) || (yy < 0)
                                    || (xx < 0)) {
                                continue;
                            } else if (geom.contains(pointGeometries[xx][yy]
                                    .getCentroid())) {
                                SourceBinEntry sbe = new SourceBinEntry();

                                if (returnLatLon) {
                                    sbe.setCoor(pointGeometries[xx][yy]
                                            .getCentroid().getCoordinate());
                                } else {
                                    sbe.setCoor(new Coordinate(xx, yy));
                                    double arealPercent = getArealPercentage(
                                            pointGeometries[xx][yy], geom);
                                    sbe.setArea(arealPercent);
                                    myPoints.add(sbe);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, "Unable to process nest! ", e);
        }

        return myPoints;
    }

    /**
     * point cache for HRAPs
     * 
     * @param extent
     */
    private void setXMRGPointCache(Rectangle extent) {
        // pointCache = new Point[extent.height][extent.width];
        pointGeometries = new Geometry[extent.height][extent.width];

        for (int y = 0; y < extent.height; y++) {
            for (int x = 0; x < extent.width; x++) {
                int xx = getHRAPSubGrid().getExtent().x + x;
                int yy = getHRAPSubGrid().getExtent().y + y;

                Coordinate[] coors = new Coordinate[5];

                coors[0] = getHRAPLatLon(new Coordinate(xx, yy));
                coors[1] = getHRAPLatLon(new Coordinate(xx + 1, yy));
                coors[2] = getHRAPLatLon(new Coordinate(xx + 1, yy + 1));
                coors[3] = getHRAPLatLon(new Coordinate(xx, yy + 1));

                // complete the square
                coors[4] = coors[0];

                LinearRing lr = config.getGeometryFactory().createLinearRing(
                        coors);
                Polygon poly = config.getGeometryFactory().createPolygon(lr,
                        null);

                pointGeometries[y][x] = poly;
            }
        }
    }

    /**
     * point cache for general PDO's
     * 
     * @throws FactoryException
     * @throws TransformException
     */
    private void setPDOPointCache() throws TransformException, FactoryException {
        // pointCache = new Point[imp.getNx()][imp.getNy()];
        pointGeometries = new Geometry[imp.getNx()][imp.getNy()];
        for (int x = 0; x < imp.getNx(); x++) {
            for (int y = 0; y < imp.getNy(); y++) {

                Coordinate[] coors = new Coordinate[5];

                ReferencedCoordinate rc1 = new ReferencedCoordinate(
                        new Coordinate(x, y), imp.getGridGeometry(),
                        Type.GRID_CORNER);
                coors[0] = rc1.asLatLon();

                ReferencedCoordinate rc2 = new ReferencedCoordinate(
                        new Coordinate(x + 1, y), imp.getGridGeometry(),
                        Type.GRID_CORNER);
                coors[1] = rc2.asLatLon();

                ReferencedCoordinate rc3 = new ReferencedCoordinate(
                        new Coordinate(x + 1, y + 1), imp.getGridGeometry(),
                        Type.GRID_CORNER);
                coors[2] = rc3.asLatLon();

                ReferencedCoordinate rc4 = new ReferencedCoordinate(
                        new Coordinate(x, y + 1), imp.getGridGeometry(),
                        Type.GRID_CORNER);
                coors[3] = rc4.asLatLon();
                // complete the square
                coors[4] = coors[0];

                LinearRing lr = config.getGeometryFactory().createLinearRing(
                        coors);
                Polygon poly = config.getGeometryFactory().createPolygon(lr,
                        null);

                pointGeometries[x][y] = poly;

            }
        }
    }

    /**
     * point cache for Grids
     * 
     * @throws FactoryException
     * @throws TransformException
     */
    private void setGridPointCache() throws TransformException,
            FactoryException {
        // pointCache = new Point[getNx()][getNy()];
        pointGeometries = new Geometry[getNx()][getNy()];
        for (int x = 0; x < getNx(); x++) {
            for (int y = 0; y < getNy(); y++) {
                Coordinate[] coors = new Coordinate[5];

                ReferencedCoordinate rc1 = new ReferencedCoordinate(
                        new Coordinate(x, y), getGridGeometry(),
                        Type.GRID_CORNER);
                coors[0] = rc1.asLatLon();

                ReferencedCoordinate rc2 = new ReferencedCoordinate(
                        new Coordinate(x + 1, y), getGridGeometry(),
                        Type.GRID_CORNER);
                coors[1] = rc2.asLatLon();

                ReferencedCoordinate rc3 = new ReferencedCoordinate(
                        new Coordinate(x + 1, y + 1), getGridGeometry(),
                        Type.GRID_CORNER);
                coors[2] = rc3.asLatLon();

                ReferencedCoordinate rc4 = new ReferencedCoordinate(
                        new Coordinate(x, y + 1), getGridGeometry(),
                        Type.GRID_CORNER);
                coors[3] = rc4.asLatLon();
                // complete the square
                coors[4] = coors[0];

                LinearRing lr = config.getGeometryFactory().createLinearRing(
                        coors);
                Polygon poly = config.getGeometryFactory().createPolygon(lr,
                        null);

                pointGeometries[x][y] = poly;
            }
        }
    }

    /**
     * Gets the correct area percent coverage
     * 
     * @param dataPointGeo
     * @param basinGeo
     * @return
     */
    private double getArealPercentage(Geometry dataPointGeo, Geometry basinGeo) {
        double arealPercent = 0.0;

        Geometry intersectGeo = basinGeo.intersection(dataPointGeo);
        arealPercent = intersectGeo.getArea() / dataPointGeo.getArea();

        // System.out.println("Areal Percent: " + arealPercent
        // + " intersect area: " + intersectGeo.getArea()
        // + " point area: " + dataPointGeo.getArea());

        return arealPercent;
    }

    /**
     * Checks the status of the source bin creation
     * 
     * @param sourceId
     * @return
     */
    private boolean checkLockStatus() {
        ClusterTask task = ClusterLockUtils.lookupLock(sourceBinTaskName,
                sourceId);
        if (task.getLockState() != LockState.ALREADY_RUNNING) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * lock on the cluster
     */
    private void lock() {
        ClusterLockUtils.lock(sourceBinTaskName, sourceId, 1800 * 1000, true);
    }

    private void setFFTI() {
        if ((config.fdm.getSettingList() != null)
                && (config.fdm.getSettingList().size() > 0)) {
            for (FFTISettingXML setting : config.fdm.getSettingList()) {
                for (String dispName : setting.getSettingDisplayNames()) {

                    if (dispName.equals(source.getDisplayName())) {
                        if (source.getSourceType().equals(
                                SOURCE_TYPE.QPE.getSourceType())) {
                            fftiSource = setting.getQpeSource();
                            isFFTI = true;
                            fftiAttribute.add(setting.getAttribute().getAttributeName());
                        } else if (source.getSourceType().equals(
                                SOURCE_TYPE.QPF.getSourceType())) {
                            fftiSource = setting.getQpfSource();
                            isFFTI = true;
                            fftiAttribute.add(setting.getAttribute().getAttributeName());
                        } else {
                            fftiSource = setting.getGuidSource();
                            isFFTI = true;
                            fftiAttribute.add(setting.getAttribute().getAttributeName());
                        }
                    }

                    if (source.getSourceType().equals(
                            SOURCE_TYPE.QPE.getSourceType())) {
                        String[] settingKey = dispName.split("-");
                        if (settingKey.length == 2) {
                            if (settingKey[0].equals(siteKey)
                                    && settingKey[1].equals(source
                                            .getDisplayName())) {
                                fftiSource = setting.getQpeSource();
                                isFFTI = true;
                                fftiAttribute.add(setting.getAttribute().getAttributeName());
                            }
                        }
                    }

                    if (source.getSourceType().equals(
                            SOURCE_TYPE.QPF.getSourceType())) {
                        String[] settingKey = dispName.split("-");
                        if (settingKey.length == 2) {
                            if (settingKey[0].equals(siteKey)
                                    && settingKey[1].equals(source
                                            .getDisplayName())) {

                                fftiSource = setting.getQpfSource();
                                isFFTI = true;
                                fftiAttribute.add(setting.getAttribute().getAttributeName());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * is this source an FFTI source
     * 
     * @return
     */
    public boolean isFFTI() {
        return isFFTI;
    }

    /**
     * Gets that FFTI source
     * 
     * @return
     */
    public FFTISourceXML getFFTISource() {
        return fftiSource;
    }
    
    /**
     * Returns the FFTI attributes for this source
     * @return
     */
    public List<String> getAttributes() {
        return fftiAttribute;
    }

    /**
     * composite source ID key
     * 
     * @return
     */
    public String getSourceID() {
        return sourceId;
    }
    
    /**
     * Fire off a semi-useful message for purpose of diagnostics.
     * @param type
     */
    private void fireBadConfigMessage(
            FFMPSourceConfigurationManager.DATA_TYPE type, Exception e) {

        StringBuffer sb = new StringBuffer();
        sb.append(type + " Source: " + source.getSourceName()
                + " has a non-functional configuration! \n");
        sb.append("DataKey: " + dataKey + " SiteKey: " + siteKey + " \n");

        if (type == FFMPSourceConfigurationManager.DATA_TYPE.RADAR) {
            if (radarRec != null) {
                sb.append("Record: " + radarRec.getDataURI() + " \n");
            }
        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.XMRG) {
            if (xmrg != null) {
                sb.append("XMRG File: " + xmrg.getFile().getAbsolutePath()
                        + " \n");
            }
        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.PDO) {
            if (imp != null) {
                sb.append("PDO Record: " + imp.getClass().getName() + " Size: "+ imp.getDataArray().length+ "\n");
            }
        } else if (type == FFMPSourceConfigurationManager.DATA_TYPE.GRID) {
            if (gribRec != null) {
                sb.append("Record: " + gribRec.getDataURI() + " \n");
            }
        }
        // null out the record it is garbage.
        ffmpRec = null;
        statusHandler.handle(Priority.ERROR, sb.toString(), e);
    }

    /**
     * Gets the XMRG data array, checks HRAP/XMRG config for sanity.
     * 
     * @return
     */
    private short[][] getXMRGData() throws Exception {

        String fileName = "MISSING";
        
        if (xmrg.getFile() != null) {
            fileName = xmrg.getFile().getAbsolutePath();
        }
        
        this.extent = getExtents(source.getHrapGridFactor());
        setHRAPSubGrid(extent, source.getHrapGridFactor());
       
        if (xmrg.getHrapExtent() != null) {
            xmrgData = xmrg.getData(extent);
        } else {
            throw new MalformedDataException("The XMRG data is malformed or the file is non-readable. "+fileName);
        }

        return xmrgData;
    }
}
