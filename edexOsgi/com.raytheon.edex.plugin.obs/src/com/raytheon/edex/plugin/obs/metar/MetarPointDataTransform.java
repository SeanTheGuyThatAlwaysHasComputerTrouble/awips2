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
package com.raytheon.edex.plugin.obs.metar;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.edex.plugin.obs.ObsDao;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.obs.metar.MetarRecord;
import com.raytheon.uf.common.dataplugin.obs.metar.util.SkyCover;
import com.raytheon.uf.common.dataplugin.obs.metar.util.WeatherCondition;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataDescription;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.pointdata.spatial.SurfaceObsLocation;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.edex.decodertools.core.DecoderTools;
import com.raytheon.uf.edex.decodertools.time.TimeTools;

/**
 * Provides a transform from MetarRecords to PointDataContainer and vice versa.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 3, 2009            chammack     Initial creation
 * Jun 23, 2009           njensen     Combined present weather
 * Jun 29, 2009     2538  jsanchez    Sorted the sky cover.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class MetarPointDataTransform {

    private static final String ALTIMETER = "altimeter";

    private static final String SEA_LEVEL_PRESS = "seaLevelPress";

    private static final String PRESS_CHANGE3_HOUR = "pressChange3Hour";

    private static final String PRESS_CHANGE_CHAR = "pressChangeChar";

    // ALTIMETER, SEA_LEVEL_PRESS, PRESS_CHANGE3_HOUR, PRESS_CHANGE_CHAR,
    // ------------------
    private static final String PRECIP24_HOUR = "precip24Hour";

    private static final String PRECIP6_HOUR = "precip6Hour";

    private static final String PRECIP3_HOUR = "precip3Hour";

    private static final String PRECIP1_HOUR = "precip1Hour";

    // PRECIP24_HOUR, PRECIP6_HOUR, PRECIP3_HOUR, PRECIP1_HOUR
    // ------------------
    private static final String WIND_GUST = "windGust";

    private static final String WIND_SPEED = "windSpeed";

    private static final String WIND_DIR_STR = "windDirStr";

    // WIND_GUST, WIND_SPEED, WIND_DIR_STR,
    // ------------------
    private static final String PK_WND_SPD = "pkwndSpeed";

    private static final String PK_WND_DIR = "pkwndDir";

    private static final String PK_WND_TIME = "pkwndTime";

    // PK_WND_SPD, PK_WND_DIR, PK_WND_TIME
    // ------------------
    private static final String TEMPERATURE = "temperature";

    private static final String TEMP_FROM_TENTHS = "tempFromTenths";

    private static final String DEWPOINT = "dewpoint";

    private static final String DP_FROM_TENTHS = "dpFromTenths";

    // TEMPERATURE, TEMP_FROM_TENTHS, DEWPOINT, DP_FROM_TENTHS,
    // ------------------
    private static final String MAX_TEMP24_HOUR = "maxTemp24Hour";

    private static final String MIN_TEMP24_HOUR = "minTemp24Hour";

    private static final String MAX_TEMP6_HOUR = "maxTemp6Hour";

    private static final String MIN_TEMP6_HOUR = "minTemp6Hour";

    // MAX_TEMP24_HOUR, MIN_TEMP24_HOUR, MAX_TEMP6_HOUR, MIN_TEMP6_HOUR,
    // ------------------
    private static final String SNOW_DEPTH = "snowDepth";

    private static final String SNOW_WATER = "snowWater";

    private static final String SNOWFALL6_HOUR = "snowfall6Hour";

    private static final String SUNSHINE = "sunshine";

    // SNOW_DEPTH, SNOW_WATER, SNOWFALL6_HOUR, SUNSHINE,
    // ------------------
    private static final String PRES_WEATHER = "presWeather";

    private static final String VISIBILITY = "visibility";

    private static final String VERT_VISIBILITY = "vertVisibility";

    // PRES_WEATHER, VISIBILITY_STR, VISIBILITY, VERT_VISIBILITY,
    // ------------------
    private static final String STATION_NAME = "stationName";

    private static final String STATION_ID = "stationId";

    private static final String AUTO_STATION_TYPE = "autoStationType";

    private static final String REPORT_TYPE = "reportType";

    private static final String TIME_OBS = "timeObs";

    // STATION_NAME, STATION_ID, AUTO_STATION_TYPE, REPORT_TYPE, TIME_OBS,
    // ------------------
    private static final String LONGITUDE = "longitude";

    private static final String LATITUDE = "latitude";

    private static final String ELEVATION = "elevation";

    // LONGITUDE, LATITUDE, ELEVATION,
    // ------------------
    private static final String RAW_METAR = "rawMETAR";

    private static final String CORRECTION = "correction";

    private static final String DATAURI = "dataURI";

    // RAW_METAR, CORRECTION, DATAURI,
    // ------------------
    private static final String SKY_LAYER_BASE = "skyLayerBase";

    private static final String SKY_COVER = "skyCover";

    private static final String SKY_COVER_TYPE = "skyCoverType";

    private static final String SKY_COVER_GENUS = "skyCoverGenus";

    // SKY_LAYER_BASE, SKY_COVER, SKY_COVER_TYPE, SKY_COVER_GENUS,
    // ------------------

    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! It is important to
     * keep this up to date or risk breaking backwards compatibility
     * 
     */
    private static final String[] ALL_PARAMS = { ALTIMETER, SEA_LEVEL_PRESS,
            PRESS_CHANGE3_HOUR, PRESS_CHANGE_CHAR, PRECIP24_HOUR, PRECIP6_HOUR,
            PRECIP3_HOUR, PRECIP1_HOUR, WIND_GUST, WIND_SPEED, WIND_DIR_STR,
            PK_WND_SPD, PK_WND_DIR, PK_WND_TIME, TEMPERATURE, TEMP_FROM_TENTHS,
            DEWPOINT, DP_FROM_TENTHS, MAX_TEMP24_HOUR, MIN_TEMP24_HOUR,
            SNOW_DEPTH, SNOW_WATER, SNOWFALL6_HOUR, SUNSHINE, MAX_TEMP6_HOUR,
            MIN_TEMP6_HOUR, PRES_WEATHER, VISIBILITY, VERT_VISIBILITY,
            STATION_NAME, STATION_ID, AUTO_STATION_TYPE, REPORT_TYPE, TIME_OBS,
            LONGITUDE, LATITUDE, ELEVATION, RAW_METAR, CORRECTION, DATAURI,
            SKY_LAYER_BASE, SKY_COVER, SKY_COVER_TYPE, SKY_COVER_GENUS, };

    public static final String ALL_PARAMS_LIST;
    static {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (String s : ALL_PARAMS) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(s);
        }
        ALL_PARAMS_LIST = sb.toString();
    }

    private ObsDao dao;

    private PointDataDescription description;

    public MetarPointDataTransform() {
        try {
            this.dao = new ObsDao("obs");
            this.description = dao.getPointDataDescription(null);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public PluginDataObject[] toPointData(PluginDataObject[] pdo) {

        if (pdo.length > 0) {
            Map<File, PointDataContainer> pointMap = new HashMap<File, PointDataContainer>();

            for (PluginDataObject p : pdo) {
                if (!(p instanceof MetarRecord))
                    continue;

                File f = this.dao.getFullFilePath(p);
                PointDataContainer pdc = pointMap.get(f);
                if (pdc == null) {
                    pdc = PointDataContainer.build(this.description);
                    pointMap.put(f, pdc);
                }

                MetarRecord mr = (MetarRecord) p;
                PointDataView pdv = buildView(pdc, mr);
                mr.setPointDataView(pdv);
            }
        }
        return pdo;
    }

    private PointDataView buildView(PointDataContainer container,
            MetarRecord record) {
        PointDataView pdv = container.append();

        if (record.getCorrection() != null
                && record.getCorrection().equals("COR")) {
            pdv.setInt(CORRECTION, 1);
        } else {
            pdv.setInt(CORRECTION, 0);
        }

        pdv.setLong(TIME_OBS, record.getDataTime().getRefTime().getTime());
        pdv.setString(AUTO_STATION_TYPE, record.getAutoStationType());
        // TODO: Temporary?
        pdv.setString(RAW_METAR, record.getReport());
        // pdv.setLong("nominalTime", record.getNominalTime())

        pdv.setFloat(VERT_VISIBILITY, record.getVertVisibility());
        // TODO:
        pdv.setFloat(VISIBILITY, record.getVisibility());

        if (record.getSkyCoverage() != null) {
            record.sort(record.getSkyCoverage());
            Iterator<SkyCover> scIterator = record.getSkyCoverage().iterator();
            int i = 0;
            while (scIterator.hasNext()) {
                // TODO: storing duplicate info like this, needs to be resolved
                SkyCover sc = scIterator.next();
                if (sc.getType() != null) {
                    StringBuffer scBuffer = new StringBuffer();
                    scBuffer.append(sc.getType());
                    if (sc.getGenus() != null)
                        scBuffer.append(sc.getGenus());

                    pdv.setString(SKY_COVER, scBuffer.toString(), i);
                    if (sc.getType() != null) {
                        pdv.setString(SKY_COVER_TYPE, sc.getType(), i);
                    }

                    if (sc.getGenus() != null) {
                        pdv.setString(SKY_COVER_GENUS, sc.getGenus(), i);
                    }

                    if (sc.getHeight() != null) {
                        pdv.setFloat(SKY_LAYER_BASE, sc.getHeight(), i);
                    }
                }
                i++;
            }
        }
        // Write this data in "backwards" so that the plot model stuff
        // displays correctly.
        if (record.getWeatherCondition() != null) {
            int i = record.getWeatherCondition().size() - 1;
            for (WeatherCondition wc : record.getWeatherCondition()) {
                pdv.setString(PRES_WEATHER, wc.toCanonicalForm(), i--);
            }
        }

        pdv.setFloat(SEA_LEVEL_PRESS, record.getSeaLevelPress());
        pdv.setFloat(ALTIMETER, record.getAltimeter());
        pdv.setString(PRESS_CHANGE_CHAR, record.getPressChangeChar());
        pdv.setFloat(PRESS_CHANGE3_HOUR, record.getPressChange3Hour());

        pdv.setFloat(TEMPERATURE, record.getTemperature());
        pdv.setFloat(DEWPOINT, record.getDewPoint());
        pdv.setFloat(TEMP_FROM_TENTHS, record.getTempFromTenths());
        pdv.setFloat(DP_FROM_TENTHS, record.getDewPointFromTenths());

        pdv.setFloat(MIN_TEMP6_HOUR, record.getMinTemp6Hour());
        pdv.setFloat(MAX_TEMP6_HOUR, record.getMaxTemp6Hour());

        pdv.setFloat(MIN_TEMP24_HOUR, record.getMinTemp24Hour());
        pdv.setFloat(MAX_TEMP24_HOUR, record.getMaxTemp24Hour());

        pdv.setInt(SNOW_DEPTH, record.getSnowDepth());
        pdv.setFloat(SNOW_WATER, record.getSnowWater());
        pdv.setFloat(SNOWFALL6_HOUR, record.getSnowFall_6Hours());
        pdv.setInt(SUNSHINE, record.getSunshine());

        if (record.getWindDir() != null
                && !record.getWindDir().equalsIgnoreCase("VRB")) {
            pdv.setFloat("windDir", Float.parseFloat(record.getWindDir()));
        }
        // TODO: temporary?
        pdv.setString(WIND_DIR_STR, record.getWindDir());
        pdv.setFloat(WIND_SPEED, record.getWindSpeed());
        pdv.setFloat(WIND_GUST, record.getWindGust());

        // pdv.setFloat("pkwndSpeed",
        pdv.setFloat(PK_WND_SPD, record.getPkWndSpd());
        pdv.setFloat(PK_WND_DIR, record.getPkWndDir());
        Calendar pkTim = record.getPkWndTime();
        if (pkTim != null) {
            pdv.setLong(PK_WND_TIME, pkTim.getTimeInMillis());
        }

        pdv.setFloat(PRECIP1_HOUR, record.getPrecip1Hour());
        pdv.setFloat(PRECIP3_HOUR, record.getPrecip3Hour());
        pdv.setFloat(PRECIP6_HOUR, record.getPrecip6Hour());
        pdv.setFloat(PRECIP24_HOUR, record.getPrecip24Hour());

        return pdv;

    }

    public static MetarRecord toMetarRecord(PointDataView pdv) {
        MetarRecord mr = new MetarRecord();
        mr.setAutoStationType(pdv.getString(AUTO_STATION_TYPE));
        if (pdv.getNumber(CORRECTION).intValue() == 1) {
            mr.setCorrection("COR");
        }

        mr.setDataTime(new DataTime(new Date(pdv.getNumber(TIME_OBS)
                .longValue())));

        SurfaceObsLocation loc = new SurfaceObsLocation(
                pdv.getString(STATION_NAME));
        Double lat = pdv.getNumber(LATITUDE).doubleValue();
        Double lon = pdv.getNumber(LONGITUDE).doubleValue();
        loc.assignLocation(lat, lon);
        loc.setElevation(pdv.getNumber(ELEVATION).intValue());
        mr.setLocation(loc);
        mr.setDataURI(pdv.getString(DATAURI));
        mr.setReport(pdv.getString(RAW_METAR));
        mr.setReportType(pdv.getString(REPORT_TYPE));

        mr.setTemperature(pdv.getNumber(TEMPERATURE).intValue());
        mr.setDewPoint(pdv.getNumber(DEWPOINT).intValue());
        mr.setTempFromTenths(pdv.getNumber(TEMP_FROM_TENTHS).intValue());
        mr.setDewPointFromTenths(pdv.getNumber(DP_FROM_TENTHS).floatValue());

        mr.setMinTemp6Hour(pdv.getNumber(MIN_TEMP6_HOUR).floatValue());
        mr.setMaxTemp6Hour(pdv.getNumber(MAX_TEMP6_HOUR).floatValue());
        mr.setMinTemp24Hour(pdv.getNumber(MIN_TEMP24_HOUR).floatValue());
        mr.setMaxTemp24Hour(pdv.getNumber(MAX_TEMP24_HOUR).floatValue());

        mr.setSnowDepth(pdv.getNumber(SNOW_DEPTH).intValue());
        mr.setSnowWater(pdv.getNumber(SNOW_WATER).floatValue());
        mr.setSnowFall_6Hours(pdv.getNumber(SNOWFALL6_HOUR).floatValue());
        mr.setSunshine(pdv.getNumber(SUNSHINE).intValue());

        mr.setSeaLevelPress(pdv.getNumber(SEA_LEVEL_PRESS).floatValue());
        mr.setAltimeter(pdv.getNumber(ALTIMETER).floatValue());
        double pa = DecoderTools.inToPa(pdv.getNumber(ALTIMETER).doubleValue());
        mr.setAltimeterInPa((float) pa);
        mr.setPressChange3Hour(pdv.getNumber(PRESS_CHANGE3_HOUR).floatValue());
        mr.setPressChangeChar(pdv.getString(PRESS_CHANGE_CHAR));

        mr.setPluginName("obs");
        mr.setPrecip1Hour(pdv.getNumber(PRECIP1_HOUR).floatValue());
        mr.setPrecip3Hour(pdv.getNumber(PRECIP3_HOUR).floatValue());
        mr.setPrecip6Hour(pdv.getNumber(PRECIP6_HOUR).floatValue());
        mr.setPrecip24Hour(pdv.getNumber(PRECIP24_HOUR).floatValue());

        String[] scType = pdv.getStringAllLevels(SKY_COVER_TYPE);
        String[] scGenus = pdv.getStringAllLevels(SKY_COVER_GENUS);
        Number[] levels = pdv.getNumberAllLevels(SKY_LAYER_BASE);
        int i = 0;
        Set<SkyCover> scList = new HashSet<SkyCover>();
        for (String s : scType) {
            if (s != null && !s.equals("")) {

                SkyCover skyCover = new SkyCover();
                skyCover.setType(s);
                skyCover.setGenus(scGenus[i]);

                if ((levels[i].intValue()) != PointDataDescription.FILL_VALUE_INT) {
                    skyCover.setHeight(levels[i].intValue());
                }

                scList.add(skyCover);
            }
            i++;
        }
        mr.setSkyCoverage(scList);

        mr.setVertVisibility(pdv.getNumber(VERT_VISIBILITY).intValue());
        mr.setVisibility(pdv.getNumber(VISIBILITY).floatValue());

        String[] presentWx = pdv.getStringAllLevels(PRES_WEATHER);
        if ((presentWx != null) && (presentWx.length > 0)) {
            int na = 0;
            int nb = presentWx.length - 1;
            while (na < nb) {
                String s = presentWx[na];
                presentWx[na++] = presentWx[nb];
                presentWx[nb--] = s;
            }

            StringBuilder wx = new StringBuilder(presentWx[0]);
            for (i = 1; i < presentWx.length; i++) {
                wx.append(" ");
                wx.append(presentWx[i]);
            }
            List<WeatherCondition> conds = WeatherCondition.parseWeather(wx
                    .toString());

            mr.setWeatherCondition(conds);
            mr.setWeatherKey(WeatherCondition.toCanonicalForm(conds));
        }

        // METAR Wind data
        mr.setWindDir(pdv.getString(WIND_DIR_STR));
        mr.setWindGust(pdv.getNumber(WIND_GUST).intValue());
        mr.setWindSpeed(pdv.getNumber(WIND_SPEED).intValue());

        mr.setPkWndDir(pdv.getNumber(PK_WND_DIR).intValue());
        mr.setPkWndSpd(pdv.getNumber(PK_WND_SPD).intValue());
        long t = pdv.getNumber(PK_WND_TIME).longValue();
        if (t >= 0) {
            mr.setPkWndTime(TimeTools.newCalendar(t));
        }

        return mr;
    }

    public static MetarRecord[] toMetarRecords(PointDataContainer container) {
        List<MetarRecord> records = new ArrayList<MetarRecord>();
        container.setCurrentSz(container.getAllocatedSz());
        for (int i = 0; i < container.getCurrentSz(); i++) {
            PointDataView pdv = container.readRandom(i);
            records.add(toMetarRecord(pdv));
        }
        return records.toArray(new MetarRecord[records.size()]);

    }
}
