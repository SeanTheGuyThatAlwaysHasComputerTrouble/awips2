package com.raytheon.uf.common.dataplugin.ldadhydro;

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

import java.util.Calendar;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Index;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.dataplugin.persist.PersistablePluginDataObject;
import com.raytheon.uf.common.geospatial.ISpatialEnabled;
import com.raytheon.uf.common.pointdata.IPointData;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.pointdata.spatial.SurfaceObsLocation;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Record implementation for ldadhydro plugin.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 *                     
 * Date          Ticket#     Engineer    Description
 * -----------  ----------  ----------- --------------------------
 * Sep 30, 2009             vkorolev    Initial creation
 * Apr 04, 2013 1846        bkowal      Added an index on refTime and
 *                                      forecastTime
 * Apr 12, 2013 1857        bgonzale    Added SequenceGenerator annotation.
 * May 07, 2013 1869        bsteffen    Remove dataURI column from
 *                                      PluginDataObject.
 * Aug 30, 2013 2298        rjpeter     Make getPluginName abstract
 * Oct 15, 2013 2361        njensen     Remove XML annotations and IDecoderGettable
 * 
 * </pre>
 * 
 * @author vkorolev
 * @version 1
 */

@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "ldadhydroseq")
@Table(name = "ldadhydro", uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) })
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@org.hibernate.annotations.Table(appliesTo = "ldadhydro", indexes = { @Index(name = "ldadhydro_refTimeIndex", columnNames = {
        "refTime", "forecastTime" }) })
@DynamicSerialize
public class HydroLdadRecord extends PersistablePluginDataObject implements
        ISpatialEnabled, IPointData, IPersistable {

    private static final long serialVersionUID = 1L;

    public static final String OBS_TEXT = "text";

    public static final Unit<Length> LENGTH_UNIT = SI.METER;

    public static final Unit<Temperature> TEMPERATURE_UNIT = SI.KELVIN;

    public static final Unit<Velocity> WIND_SPEED_UNIT = SI.METERS_PER_SECOND;

    public static final Unit<Angle> WIND_DIR_UNIT = NonSI.DEGREE_ANGLE;

    public static final Unit<Pressure> PRESSURE_UNIT = SI.PASCAL;

    public static final Unit<Angle> LOCATION_UNIT = NonSI.DEGREE_ANGLE;

    // Time of the observation.
    @DataURI(position = 2)
    @Column
    @DynamicSerializeElement
    private Calendar observationTime;

    // numeric WMO identification number
    @Column
    @DynamicSerializeElement
    private long numericWMOid;

    // latitude, longitude, elevation, stationId="RALC2"
    @Embedded
    @DataURI(position = 3, embedded = true)
    @DynamicSerializeElement
    private SurfaceObsLocation location;

    // Data Provider station Id
    @Column
    @DynamicSerializeElement
    private String providerId; // * "110" "FA6026DA"

    @Column
    @DynamicSerializeElement
    private String stationName; // * "Ralston_Res"

    // Handbook Id (AFOS id or SHEF id)
    @Column
    @DynamicSerializeElement
    private String handbook5Id;

    // Home WFO Id for the LDAD data
    @Column
    @DynamicSerializeElement
    private String homeWFO;

    // LDAD hydro station type.
    @Column
    @DynamicSerializeElement
    private String stationType;

    // LDAD hydro data provider
    @DataURI(position = 1)
    @Column
    @DynamicSerializeElement
    private String dataProvider;

    // time data was processed by the provider
    @Column
    @DynamicSerializeElement
    private double reportTime; // * 1.247436157E9

    // * time data was received
    @Column
    @DynamicSerializeElement
    private Double receivedTime; // seconds since 1-1-1970

    // Below surface
    @Column
    @DynamicSerializeElement
    private Float belowSurface; // meter

    // River stage
    @Column
    @DynamicSerializeElement
    private Float riverStage; // meter

    // Pool elevation
    @Column
    @DynamicSerializeElement
    private Float poolElevation; // meter

    // Tail water stage
    @Column
    @DynamicSerializeElement
    private Float tailwaterStage; // meter

    // River velocity
    @Column
    @DynamicSerializeElement
    private Float riverVelocity; // kph

    // River inflow
    @Column
    @DynamicSerializeElement
    private Float riverInflow; // meter^3 / sec

    // River flow
    @Column
    @DynamicSerializeElement
    private Float riverFlow; // meter^3 / sec

    // Computed outflow
    @Column
    @DynamicSerializeElement
    private Float computedOutflow; // meter^3 / sec

    // Water temperature
    @Column
    @DynamicSerializeElement
    private Float waterTemperature; // kelvin

    // Battery voltage
    @Column
    @DynamicSerializeElement
    private Float voltageBattery; // volt

    // Water conductance
    @Column
    @DynamicSerializeElement
    private Float waterConductance; // umhos/cm

    // Water oxygen
    @Column
    @DynamicSerializeElement
    private Float waterOxygen; // mg/l

    // Water PH
    @Column
    @DynamicSerializeElement
    private Float waterPH; // pH

    // Relative humidity
    @Column
    @DynamicSerializeElement
    private Float relHumidity;

    // River stage & flow - time of last change (ALERT)
    @Column
    @DynamicSerializeElement
    private Double riverReportChangeTime; // seconds since 1970-1-1 00:00:00.0

    // Observation air temperature in degrees Kelvin.
    @Column
    @DynamicSerializeElement
    private Float temperature;

    // Observation dewpoint temperature in degrees Kelvin.
    @Column
    @DynamicSerializeElement
    private Float dewpoint;

    // Observation wind direction in angular degrees.
    @Column
    @DynamicSerializeElement
    private Float windDir;

    // Observation wind speed in meters per second.
    @Column
    @DynamicSerializeElement
    private Float windSpeed;

    // Wind speed peak
    @Column
    @DynamicSerializeElement
    private Float windSpeedPeak;

    // Observation wind gust in meters per second.
    @Column
    @DynamicSerializeElement
    private Double windGust;

    // precip accumulation with an unknown time period in mm.
    @Column
    @DynamicSerializeElement
    private Float precipAccum; // mm

    // 5 minute precip accumulation
    @Column
    @DynamicSerializeElement
    private Float precip5min; // mm

    // 1 hour precip accumulation
    @Column
    @DynamicSerializeElement
    private Float precip1hr; // mm

    // 3 hour precip accumulation
    @Column
    @DynamicSerializeElement
    private Float precip3hr; // float precip3hr mm

    // 6 hour precip accumulation
    @Column
    @DynamicSerializeElement
    private Float precip6hr; // float precip6hr mm

    // 12 hour precip accumulation mm
    @Column
    @DynamicSerializeElement
    private Float precip12hr;

    // 18 hour precip accumulation mm
    @Column
    @DynamicSerializeElement
    private Float precip18hr;

    // 24 hour precip accumulation
    @Column
    @DynamicSerializeElement
    private Float precip24hr; // mm

    // Raw text LDAD hydro report
    @Column
    @DynamicSerializeElement
    private String rawMessage;

    private PointDataView pointDataView;

    /**
     * 
     */
    public HydroLdadRecord() {
    }

    /**
     * Constructor for DataURI construction through base class. This is used by
     * the notification service.
     * 
     * @param uri
     *            A data uri applicable to this class.
     */
    public HydroLdadRecord(String uri) {
        super(uri);
    }

    /**
     * Get this observation's geometry.
     * 
     * @return The geometry for this observation.
     */
    public Geometry getGeometry() {
        return location.getGeometry();
    }

    /**
     * Get the geometry latitude.
     * 
     * @return The geometry latitude.
     */
    public double getLatitude() {
        return location.getLatitude();
    }

    /**
     * Get the geometry longitude.
     * 
     * @return The geometry longitude.
     */
    public double getLongitude() {
        return location.getLongitude();
    }

    /**
     * Get the station identifier for this observation.
     * 
     * @return the stationId
     */
    public String getStationId() {
        return location.getStationId();
    }

    /**
     * Get the elevation, in meters, of the observing platform or location.
     * 
     * @return The observation elevation, in meters.
     */
    public Integer getElevation() {
        return location.getElevation();
    }

    /**
     * @return the location
     */
    public SurfaceObsLocation getLocation() {
        return location;
    }

    /**
     * @param location
     *            the location to set
     */
    public void setLocation(SurfaceObsLocation location) {
        this.location = location;
    }

    /**
     * @return the timeObs
     */
    public Calendar getObservationTime() {
        return observationTime;
    }

    /**
     * @param observationTime
     *            the observationTime to set
     */
    public void setObservationTime(Calendar timeObs) {
        this.observationTime = timeObs;
    }

    /**
     * @return the windSpeed
     */
    public Float getWindSpeed() {
        return windSpeed;
    }

    /**
     * @param windSpeed
     *            the windSpeed to set
     */
    public void setWindSpeed(Float windSpeed) {
        this.windSpeed = windSpeed;
    }

    /**
     * @return the windGust
     */
    public Double getWindGust() {
        return windGust;
    }

    /**
     * @param windGust
     *            the windGust to set
     */
    public void setWindGust(Double windGust) {
        this.windGust = windGust;
    }

    /**
     * 
     */
    public void setSpatialObject(SurfaceObsLocation loc) {
        location = loc;
    }

    /**
     * 
     */
    @Override
    public SurfaceObsLocation getSpatialObject() {
        return location;
    }

    /**
    /**
     * @param providerId
     *            the providerId to set
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * @return the providerId
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * @param stationName
     *            the stationName to set
     */
    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    /**
     * @return the stationName
     */
    public String getStationName() {
        return stationName;
    }

    /**
     * @param handbook5Id
     *            the handbook5Id to set
     */
    public void setHandbook5Id(String handbook5Id) {
        this.handbook5Id = handbook5Id;
    }

    /**
     * @return the handbook5Id
     */
    public String getHandbook5Id() {
        return handbook5Id;
    }

    /**
     * @param homeWFO
     *            the homeWFO to set
     */
    public void setHomeWFO(String homeWFO) {
        this.homeWFO = homeWFO;
    }

    /**
     * @return the homeWFO
     */
    public String getHomeWFO() {
        return homeWFO;
    }

    /**
     * @param stationType
     *            the stationType to set
     */
    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    /**
     * @return the stationType
     */
    public String getStationType() {
        return stationType;
    }

    /**
     * @param dataProvider
     *            the dataProvider to set
     */
    public void setDataProvider(String dataProvider) {
        this.dataProvider = dataProvider;
    }

    /**
     * @return the dataProvider
     */
    public String getDataProvider() {
        return dataProvider;
    }

    /**
     * @param receivedTime
     *            the receivedTime to set
     */
    public void setReceivedTime(Double receivedTime) {
        this.receivedTime = receivedTime;
    }

    /**
     * @return the receivedTime
     */
    public Double getReceivedTime() {
        return receivedTime;
    }

    /**
     * @param belowSurface
     *            the belowSurface to set
     */
    public void setBelowSurface(Float belowSurface) {
        this.belowSurface = belowSurface;
    }

    /**
     * @return the belowSurface
     */
    public Float getBelowSurface() {
        return belowSurface;
    }

    /**
     * @param riverStage
     *            the riverStage to set
     */
    public void setRiverStage(Float riverStage) {
        this.riverStage = riverStage;
    }

    /**
     * @return the riverStage
     */
    public Float getRiverStage() {
        return riverStage;
    }

    /**
     * @param poolElevation
     *            the poolElevation to set
     */
    public void setPoolElevation(Float poolElevation) {
        this.poolElevation = poolElevation;
    }

    /**
     * @return the poolElevation
     */
    public Float getPoolElevation() {
        return poolElevation;
    }

    /**
     * @param tailwaterStage
     *            the tailwaterStage to set
     */
    public void setTailwaterStage(Float tailwaterStage) {
        this.tailwaterStage = tailwaterStage;
    }

    /**
     * @return the tailwaterStage
     */
    public Float getTailwaterStage() {
        return tailwaterStage;
    }

    /**
     * @param riverVelocity
     *            the riverVelocity to set
     */
    public void setRiverVelocity(Float riverVelocity) {
        this.riverVelocity = riverVelocity;
    }

    /**
     * @return the riverVelocity
     */
    public Float getRiverVelocity() {
        return riverVelocity;
    }

    /**
     * @param riverInflow
     *            the riverInflow to set
     */
    public void setRiverInflow(Float riverInflow) {
        this.riverInflow = riverInflow;
    }

    /**
     * @return the riverInflow
     */
    public Float getRiverInflow() {
        return riverInflow;
    }

    /**
     * @param riverFlow
     *            the riverFlow to set
     */
    public void setRiverFlow(Float riverFlow) {
        this.riverFlow = riverFlow;
    }

    /**
     * @return the riverFlow
     */
    public Float getRiverFlow() {
        return riverFlow;
    }

    /**
     * @param computedOutflow
     *            the computedOutflow to set
     */
    public void setComputedOutflow(Float computedOutflow) {
        this.computedOutflow = computedOutflow;
    }

    /**
     * @return the computedOutflow
     */
    public Float getComputedOutflow() {
        return computedOutflow;
    }

    /**
     * @param waterTemperature
     *            the waterTemperature to set
     */
    public void setWaterTemperature(Float waterTemperature) {
        this.waterTemperature = waterTemperature;
    }

    /**
     * @return the waterTemperature
     */
    public Float getWaterTemperature() {
        return waterTemperature;
    }

    /**
     * @param voltageBattery
     *            the voltageBattery to set
     */
    public void setVoltageBattery(Float voltageBattery) {
        this.voltageBattery = voltageBattery;
    }

    /**
     * @return the voltageBattery
     */
    public Float getVoltageBattery() {
        return voltageBattery;
    }

    /**
     * @param waterConductance
     *            the waterConductance to set
     */
    public void setWaterConductance(Float waterConductance) {
        this.waterConductance = waterConductance;
    }

    /**
     * @return the waterConductance
     */
    public Float getWaterConductance() {
        return waterConductance;
    }

    /**
     * @param waterOxygen
     *            the waterOxygen to set
     */
    public void setWaterOxygen(Float waterOxygen) {
        this.waterOxygen = waterOxygen;
    }

    /**
     * @return the waterOxygen
     */
    public Float getWaterOxygen() {
        return waterOxygen;
    }

    /**
     * @param waterPH
     *            the waterPH to set
     */
    public void setWaterPH(Float waterPH) {
        this.waterPH = waterPH;
    }

    /**
     * @return the waterPH
     */
    public Float getWaterPH() {
        return waterPH;
    }

    /**
     * @param riverReportChangeTime
     *            the riverReportChangeTime to set
     */
    public void setRiverReportChangeTime(Double riverReportChangeTime) {
        this.riverReportChangeTime = riverReportChangeTime;
    }

    /**
     * @return the riverReportChangeTime
     */
    public Double getRiverReportChangeTime() {
        return riverReportChangeTime;
    }

    /**
     * @param precip12hr
     *            the precip12hr to set
     */
    public void setPrecip12hr(Float precip12hr) {
        this.precip12hr = precip12hr;
    }

    /**
     * @return the precip12hr
     */
    public Float getPrecip12hr() {
        return precip12hr;
    }

    /**
     * @param precip18hr
     *            the precip18hr to set
     */
    public void setPrecip18hr(Float precip18hr) {
        this.precip18hr = precip18hr;
    }

    /**
     * @return the precip18hr
     */
    public Float getPrecip18hr() {
        return precip18hr;
    }

    /**
     * @param temperature
     *            the temperature to set
     */
    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    /**
     * @return the temperature
     */
    public Float getTemperature() {
        return temperature;
    }

    /**
     * @param dewpoint
     *            the dewpoint to set
     */
    public void setDewpoint(Float dewpoint) {
        this.dewpoint = dewpoint;
    }

    /**
     * @return the dewpoint
     */
    public Float getDewpoint() {
        return dewpoint;
    }

    /**
     * @param windDir
     *            the windDir to set
     */
    public void setWindDir(Float windDir) {
        this.windDir = windDir;
    }

    /**
     * @return the windDir
     */
    public Float getWindDir() {
        return windDir;
    }

    /**
     * @param windSpeedPeak
     *            the windSpeedPeak to set
     */
    public void setWindSpeedPeak(Float windSpeedPeak) {
        this.windSpeedPeak = windSpeedPeak;
    }

    /**
     * @return the windSpeedPeak
     */
    public Float getWindSpeedPeak() {
        return windSpeedPeak;
    }

    /**
     * @param precipAccum
     *            the precipAccum to set
     */
    public void setPrecipAccum(Float precipAccum) {
        this.precipAccum = precipAccum;
    }

    /**
     * @return the precipAccum
     */
    public Float getPrecipAccum() {
        return precipAccum;
    }

    /**
     * @param precip5min
     *            the precip5min to set
     */
    public void setPrecip5min(Float precip5min) {
        this.precip5min = precip5min;
    }

    /**
     * @return the precip5min
     */
    public Float getPrecip5min() {
        return precip5min;
    }

    /**
     * @param precip1hr
     *            the precip1hr to set
     */
    public void setPrecip1hr(Float precip1hr) {
        this.precip1hr = precip1hr;
    }

    /**
     * @return the precip1hr
     */
    public Float getPrecip1hr() {
        return precip1hr;
    }

    /**
     * @param precip3hr
     *            the precip3hr to set
     */
    public void setPrecip3hr(Float precip3hr) {
        this.precip3hr = precip3hr;
    }

    /**
     * @return the precip3hr
     */
    public Float getPrecip3hr() {
        return precip3hr;
    }

    /**
     * @param precip6hr
     *            the precip6hr to set
     */
    public void setPrecip6hr(Float precip6hr) {
        this.precip6hr = precip6hr;
    }

    /**
     * @return the precip6hr
     */
    public Float getPrecip6hr() {
        return precip6hr;
    }

    /**
     * @param precip24hr
     *            the precip24hr to set
     */
    public void setPrecip24hr(Float precip24hr) {
        this.precip24hr = precip24hr;
    }

    /**
     * @return the precip24hr
     */
    public Float getPrecip24hr() {
        return precip24hr;
    }

    /**
     * @param rawMessage
     *            the rawMessage to set
     */
    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    /**
     * @return the rawMessage
     */
    public String getRawMessage() {
        return rawMessage;
    }

    /**
     * @param relHumidity
     *            the relHumidity to set
     */
    public void setRelHumidity(Float relHumidity) {
        this.relHumidity = relHumidity;
    }

    /**
     * @return the relHumidity
     */
    public Float getRelHumidity() {
        return relHumidity;
    }

    /**
     * @param numericWMOid
     *            the numericWMOid to set
     */
    public void setNumericWMOid(long numericWMOid) {
        this.numericWMOid = numericWMOid;
    }

    /**
     * @return the numericWMOid
     */
    public long getNumericWMOid() {
        return numericWMOid;
    }

    /**
     * @param l
     *            the reportTime to set
     */
    public void setReportTime(long l) {
        this.reportTime = l;
    }

    /**
     * @return the reportTime
     */
    public Double getReportTime() {
        return reportTime;
    }

    @Override
    public void setPointDataView(PointDataView pointDataView) {
        this.pointDataView = pointDataView;

    }

    /**
     * @return the pointDataView
     */
    @Override
    public PointDataView getPointDataView() {
        return this.pointDataView;
    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

    @Override
    public String getPluginName() {
        return "ldadhydro";
    }
}
