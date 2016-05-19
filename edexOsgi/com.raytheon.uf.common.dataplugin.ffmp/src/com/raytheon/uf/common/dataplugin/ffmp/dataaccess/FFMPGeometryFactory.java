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
package com.raytheon.uf.common.dataplugin.ffmp.dataaccess;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.unit.Unit;

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.impl.AbstractDataPluginFactory;
import com.raytheon.uf.common.dataaccess.impl.DefaultGeometryData;
import com.raytheon.uf.common.dataaccess.util.DatabaseQueryUtil;
import com.raytheon.uf.common.dataaccess.util.DatabaseQueryUtil.QUERY_MODE;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPBasin;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPBasinData;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPGuidanceBasin;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPRecord;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPTemplates;
import com.raytheon.uf.common.dataplugin.ffmp.FFMPTemplates.MODE;
import com.raytheon.uf.common.dataplugin.ffmp.HucLevelGeometriesFactory;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.monitor.config.FFMPRunConfigurationManager;
import com.raytheon.uf.common.monitor.config.FFMPSourceConfigurationManager;
import com.raytheon.uf.common.monitor.xml.DomainXML;
import com.raytheon.uf.common.monitor.xml.SourceXML;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A data factory for retrieving FFMP data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 24, 2013   1552     mpduff      Initial creation
 * Apr 16, 2013 1912       bsteffen    Initial bulk hdf5 access for ffmp
 * Jul 15, 2013 2184       dhladky     Remove all HUC's for storage except ALL
 * Aug,20, 2013 2250       mnash       Change some methods that were not working in all cases
 * Jan,14, 2014 2667       mnash       Remove getGridData method
 * May 1, 2014  3099       bkowal      No longer use an empty pfaf list when the
 *                                     data request locationNames list is empty.
 * Jun 24, 2014 3170       mnash       Get the accumulated time if multiple times are requested
 * Jul 14, 2014 3184       njensen     Overrode getAvailableLevels()
 * Jul 30, 2014 3184       njensen     Overrode required and optional identifiers
 * Feb 27, 2015 4180       mapeters    Overrode getAvailableParameters().
 * Jun 15, 2015 4560       ccody       Added support for configurable rate/accumulation calculation for getGeometryData
 * Jul 16, 2015 4658       dhladky     Expiration times fixed.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class FFMPGeometryFactory extends AbstractDataPluginFactory {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(FFMPGeometryFactory.class);

    /** Site key constant */
    public static final String SITE_KEY = "siteKey";

    /** Data key constant */
    public static final String DATA_KEY = "dataKey";

    /** wfo constant */
    public static final String WFO = "wfo";

    /** plugin constant */
    public static final String PLUGIN_NAME = "ffmp";

    /** huc constant */
    public static final String HUC = "huc";

    /** source name constant */
    public static final String SOURCE_NAME = "sourceName";

    /** FFMP Templates object */
    private FFMPTemplates templates;

    /**
     * Constructor.
     */
    public FFMPGeometryFactory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IGeometryData[] getGeometryData(IDataRequest request,
            DbQueryResponse dbQueryResponse) {
        List<Map<String, Object>> results = dbQueryResponse.getResults();
        Map<Long, DefaultGeometryData> cache = new HashMap<Long, DefaultGeometryData>();
        FFMPRecord record = null;
        Date start = new Date(Long.MAX_VALUE);
        Date end = new Date(0);

        for (Map<String, Object> map : results) {
            for (Map.Entry<String, Object> es : map.entrySet()) {
                FFMPRecord rec = (FFMPRecord) es.getValue();
                /*
                 * Adding all of the basin data to a single record so that we
                 * can get the accumulated values from that record
                 */
                if (record == null) {
                    record = rec;
                }
                // building a time range of the earliest FFMP time (based on
                // each record) to the latest FFMP time (based on each record)
                if (start.after(rec.getDataTime().getRefTime())) {
                    start = rec.getDataTime().getRefTime();
                }
                if (end.before(rec.getDataTime().getRefTime())) {
                    end = rec.getDataTime().getRefTime();
                }

                try {
                    rec.retrieveMapFromDataStore(templates);
                } catch (Exception e) {
                    throw new DataRetrievalException(
                            "Failed to retrieve the IDataRecord for PluginDataObject: "
                                    + rec.toString(), e);
                }

                /*
                 * loop over each pfaf id in the current record (rec) we are
                 * iterating. Add that basin data to the record that we are
                 * keeping around (record) to use to get the accumulated value.
                 */
                for (Long pfaf : rec.getBasinData().getPfafIds()) {
                    // setValue is a misnomer here, it is actually an add
                    record.getBasinData()
                            .get(pfaf)
                            .setValue(rec.getDataTime().getRefTime(),
                                    rec.getBasinData().get(pfaf).getValue());
                }

            }
        }
        /*
         * now that we have all the basin data in a single record (record), we
         * can use the methods on the FFMPRecord class to get the accumulated
         * value in the case of a non-guidance basin
         */
        try {
            cache = makeGeometryData(record, request, cache, start, end);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to create the geoemtry data from the records.", e);
        }
        return cache.values().toArray(
                new DefaultGeometryData[cache.values().size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, RequestConstraint> buildConstraintsFromRequest(
            IDataRequest request) {
        Map<String, RequestConstraint> map = new HashMap<String, RequestConstraint>();
        Map<String, Object> identifiers = request.getIdentifiers();
        String siteKey = (String) identifiers.get(SITE_KEY);
        for (Map.Entry<String, Object> entry : request.getIdentifiers()
                .entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            if (!key.equals(HUC)) {
                RequestConstraint rc = new RequestConstraint(value);
                map.put(key, rc);
            }
        }

        RequestConstraint parameterConstraint = new RequestConstraint();
        parameterConstraint.setConstraintValueList(request.getParameters());
        parameterConstraint.setConstraintType(ConstraintType.IN);
        map.put(SOURCE_NAME, parameterConstraint);

        String domain = (String) request.getIdentifiers().get(WFO);
        DomainXML domainXml = FFMPRunConfigurationManager.getInstance()
                .getDomain(domain);

        templates = FFMPTemplates.getInstance(domainXml, siteKey, MODE.EDEX);

        return map;
    }

    /**
     * Create the IGeometryData objects.
     * 
     * @param rec
     *            The FFMPRecord
     * @param cache
     * @param huc
     *            The HUC level
     * @param siteKey
     *            The siteKey
     * @param cwa
     *            The CWA
     * @param dataKey
     *            The dataKey
     * @throws Exception
     */
    private Map<Long, DefaultGeometryData> makeGeometryData(FFMPRecord rec,
            IDataRequest request, Map<Long, DefaultGeometryData> cache,
            Date start, Date end) throws Exception {
        String huc = (String) request.getIdentifiers().get(HUC);
        String dataKey = (String) request.getIdentifiers().get(DATA_KEY);
        String siteKey = (String) request.getIdentifiers().get(SITE_KEY);
        String cwa = (String) request.getIdentifiers().get(WFO);

        if (dataKey == null) {
            dataKey = siteKey;
        }

        FFMPBasinData basinData = rec.getBasinData();

        Map<Long, FFMPBasin> basinDataMap = basinData.getBasins();

        HucLevelGeometriesFactory geomFactory = HucLevelGeometriesFactory
                .getInstance();
        Map<Long, Geometry> geomMap = geomFactory.getGeometries(templates,
                siteKey, cwa, huc);

        FFMPSourceConfigurationManager srcConfigMan = FFMPSourceConfigurationManager
                .getInstance();
        SourceXML sourceXml = srcConfigMan.getSource(rec.getSourceName());
        String rateOrAccum = sourceXml.getRateOrAccum(siteKey);
        boolean isRate = false;
        if ((rateOrAccum != null) && (rateOrAccum.isEmpty() == false)
                && (rateOrAccum.compareToIgnoreCase("RATE") == 0)) {
            isRate = true;
        }
        DefaultGeometryData data = null;

        String[] locationNames = request.getLocationNames();

        List<Long> pfafList = null;
        if (locationNames != null && locationNames.length > 0) {
            pfafList = convertLocations(locationNames);
        }

        for (Long pfaf : geomMap.keySet()) {
            if (pfafList == null || pfafList.contains(pfaf)) {
                if (cache.containsKey(pfaf)) {
                    data = cache.get(pfaf);
                } else {
                    data = new DefaultGeometryData();
                    Map<String, Object> attrs = new HashMap<String, Object>();
                    attrs.put(DATA_KEY, dataKey);
                    attrs.put(SITE_KEY, siteKey);
                    attrs.put(WFO, cwa);
                    attrs.put(HUC, huc);
                    data.setAttributes(attrs);
                    data.setLocationName(String.valueOf(pfaf));
                    data.setGeometry(geomMap.get(pfaf));
                    data.setDataTime(new DataTime(start.getTime(),
                            new TimeRange(start, end)));
                    cache.put(pfaf, data);
                }

                FFMPBasin basin = basinDataMap.get(pfaf);
                Float value = null;

                if (basin == null) {
                    continue;
                }

                if (basin instanceof FFMPGuidanceBasin) {
                    value = ((FFMPGuidanceBasin) basin).getValue(
                            rec.getSourceName(),
                            sourceXml.getExpirationMinutes(rec.getSiteKey())
                                    * TimeUtil.MILLIS_PER_MINUTE);
                } else {
                    value = basin.getAccumValue(start, end,
                            sourceXml.getExpirationMinutes(rec.getSiteKey())
                                    * TimeUtil.MILLIS_PER_MINUTE, isRate);
                }
                String parameter = rec.getSourceName();
                String unitStr = sourceXml.getUnit();

                Unit<?> unit = null;
                if (unitStr.equals(SourceXML.UNIT_TXT)) {
                    unit = Unit.valueOf("in");
                }

                if (unit != null) {
                    data.addData(parameter, value, unit);
                } else {
                    data.addData(parameter, value);
                }
            }
        }

        return cache;
    }

    /**
     * Convert list of PFAF strings to list of PFAF longs
     * 
     * @param locationNames
     * @return
     */
    private List<Long> convertLocations(String[] locationNames) {
        List<Long> pfafList = new ArrayList<Long>();
        for (String s : locationNames) {
            try {
                pfafList.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error parsing pfaf id: " + s, e);
            }
        }

        return pfafList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAvailableLocationNames(IDataRequest request) {
        List<String> pfafList = new ArrayList<String>();
        String domain = (String) request.getIdentifiers().get("wfo");
        String sql = "select pfaf_id from mapdata.ffmp_basins where cwa = '"
                + domain + "';";

        List<Object[]> results = DatabaseQueryUtil.executeDatabaseQuery(
                QUERY_MODE.MODE_SQLQUERY, sql, "metadata", PLUGIN_NAME);

        for (Object[] oa : results) {
            pfafList.add((String) oa[0]);
        }

        return pfafList.toArray(new String[0]);
    }

    @Override
    public String[] getAvailableParameters(IDataRequest request) {
        StringBuilder sqlQuery = new StringBuilder("select distinct ")
                .append(SOURCE_NAME).append(" from ").append(PLUGIN_NAME);

        String keyWord = " where ";
        for (Map.Entry<String, Object> entry : request.getIdentifiers()
                .entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            sqlQuery.append(keyWord).append(key).append(" = '").append(value)
                    .append("'");
            keyWord = " and ";
        }
        sqlQuery.append(";");

        List<Object[]> results = DatabaseQueryUtil.executeDatabaseQuery(
                QUERY_MODE.MODE_SQLQUERY, sqlQuery.toString(), "metadata",
                PLUGIN_NAME);

        List<String> params = new ArrayList<>(results.size());
        for (Object[] r : results) {
            params.add((String) r[0]);
        }
        return params.toArray(new String[0]);
    }

    @Override
    public Level[] getAvailableLevels(IDataRequest request) {
        throw new IncompatibleRequestException(request.getDatatype()
                + " data does not support the concept of levels");
    }

    @Override
    public String[] getRequiredIdentifiers() {
        return new String[] { SITE_KEY, WFO, HUC };
    }

    @Override
    public String[] getOptionalIdentifiers() {
        return new String[] { DATA_KEY };
    }
}
