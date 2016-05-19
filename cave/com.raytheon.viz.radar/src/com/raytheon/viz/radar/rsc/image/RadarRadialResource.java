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
package com.raytheon.viz.radar.rsc.image;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.dataplugin.radar.RadarRecord;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ext.colormap.IColormappedImageExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.awipstools.capabilities.EAVCapability;
import com.raytheon.viz.awipstools.capabilities.RangeRingsOverlayCapability;
import com.raytheon.viz.awipstools.capabilityInterfaces.IRangeableResource;
import com.raytheon.viz.awipstools.common.EstimatedActualVelocity;
import com.raytheon.viz.awipstools.common.IRadialVelocityToolSource;
import com.raytheon.viz.radar.VizRadarRecord;
import com.raytheon.viz.radar.interrogators.IRadarInterrogator;
import com.raytheon.viz.radar.interrogators.RadarRadialInterrogator;
import com.raytheon.viz.radar.rsc.RadarImageResource;
import com.raytheon.viz.radar.rsc.RadarProductFactory;
import com.raytheon.viz.radar.rsc.RadarResourceData;
import com.raytheon.viz.radar.rsc.image.IRadialMeshExtension.RadialMeshData;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * {@link RadarImageResource} that is able to display radial data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jul 29, 2010           mnash       Initial creation
 * May 02, 2013  14587    D. Friedman Implement IRadialVelocityToolSource
 * Jul 31, 2013  2190     mschenke    Convert interrogate string "msl" to
 *                                    Measure and put in as "height"
 * Jun 11, 2014  2061     bsteffen    Move rangeable methods here.
 * Jun 24, 2014  3072     bsteffen    Remove RadarRecord dependency for Radial
 *                                    Mesh
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class RadarRadialResource extends RadarImageResource<MapDescriptor>
        implements IRadialVelocityToolSource, IRangeableResource {

    public static final String HEIGHT_INTERROGATE_ID = "height";

    private static final String EAV_VALUE = "EAC.Value";

    /**
     * @param rrd
     * @param loadProps
     * @throws VizException
     */
    public RadarRadialResource(RadarResourceData rrd, LoadProperties loadProps,
            IRadarInterrogator interrogator) throws VizException {
        super(rrd, loadProps, interrogator);
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        super.paintInternal(target, paintProps);
        RangeRingsOverlayCapability rrcap = getCapability(RangeRingsOverlayCapability.class);
        rrcap.setRangeableResource(this);
        rrcap.paint(target, paintProps);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.radar.rsc.RadarImageResource#toImageData(com.raytheon
     * .uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.ColorMapParameters,
     * com.raytheon.uf.common.dataplugin.radar.RadarRecord, java.awt.Rectangle)
     */
    @Override
    protected IImage createImage(IGraphicsTarget target,
            ColorMapParameters params, RadarRecord record, Rectangle rect)
            throws VizException {
        byte[] table = createConversionTable(params, record);
        return target
                .getExtension(IColormappedImageExtension.class)
                .initializeRaster(
                        new RadarRadialDataRetrievalAdapter(record, table, rect),
                        params);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.radar.rsc.RadarImageResource#project(org.opengis.referencing
     * .crs.CoordinateReferenceSystem)
     */
    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        super.project(crs);
        // TODO dispose just the coverage, not the image.
        for (DrawableImage image : images.values()) {
            if (image != null) {
                image.dispose();
            }
        }
        images.clear();
    }

    @Override
    public String inspect(DataTime dataTime, Map<String, String> dataMap) {
        StringBuilder sb = new StringBuilder(super.inspect(dataTime, dataMap));

        if (dataMap != null && dataMap.containsKey(EAV_VALUE)) {
            sb.append(" ").append(dataMap.get(EAV_VALUE));
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {
        Map<String, Object> dataMap = super.interrogate(coord);
        if (dataMap != null
                && dataMap.containsKey(RadarRadialInterrogator.MSL_HEIGHT_ID)) {
            Object obj = dataMap.get(RadarRadialInterrogator.MSL_HEIGHT_ID);
            // Get MSL object, handles if String, Number or Measure
            if (obj instanceof String) {
                obj = Double.parseDouble(String.valueOf(obj));
            }
            if (obj instanceof Number) {
                obj = Measure.valueOf(((Number) obj).doubleValue(), SI.METER);
            }
            if (obj instanceof Measure) {
                dataMap.put(HEIGHT_INTERROGATE_ID, (Measure<?, ?>) obj);
            }
        }
        return dataMap;
    }

    @Override
    public Map<String, String> interrogate(DataTime dataTime, Coordinate latLon) {
        Map<String, String> dataMap = super.interrogate(dataTime, latLon);

        // add EAV values to dataMap, if necessary
        if (hasCapability(EAVCapability.class)) {
            EAVCapability eavCap = getCapability(EAVCapability.class);
            if (eavCap.isCapabilityActive()) {
                if (dataMap != null) {
                    EstimatedActualVelocity eav = eavCap.getEav();
                    dataMap.put(EAV_VALUE, eav.getEAVValue(latLon,
                            new HashMap<String, Object>(dataMap)));
                }
            }
        }
        return dataMap;
    }

    protected static class RadarRadialDataRetrievalAdapter extends
            RadarImageDataRetrievalAdapter {

        public RadarRadialDataRetrievalAdapter(RadarRecord record,
                byte[] table, Rectangle rect) {
            super(record, table, rect);
        }

        @Override
        public byte[] convertData() {
            byte[] imageData = new byte[record.getNumBins()
                    * record.getNumRadials()];
            int i = 0;
            for (int h = 0; h < record.getNumRadials(); ++h) {
                for (int w = 0; w < record.getNumBins(); ++w) {
                    if (!createCircle(imageData, h, w, i)) {
                        imageData[i] = table[record.getRawIntDataValue(h, w)];
                    }
                    ++i;
                }
            }
            return imageData;
        }

        protected boolean createCircle(byte[] imageData, int h, int w, int i) {
            if (w == 0) {
                imageData[0] = (byte) 0;
                if (record.getRawData() != null) {
                    record.getRawData()[i] = (byte) 0;
                }
                if (record.getRawShortData() != null) {
                    record.getRawShortData()[i] = (byte) 0;
                }
                return true;
            }
            return false;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.radar.rsc.RadarImageResource#buildMesh(com.raytheon.
     * uf.viz.core.IGraphicsTarget, com.raytheon.viz.radar.VizRadarRecord)
     */
    @Override
    public IMesh buildMesh(IGraphicsTarget target, VizRadarRecord radarRecord)
            throws VizException {
        return target.getExtension(IRadialMeshExtension.class).constructMesh(
                new RadialMeshData(radarRecord), descriptor.getGridGeometry());
    }

    @Override
    public boolean isRadialVelocitySource() {
        int productCode = -1;
        try {
            // TODO: This duplicates logic in RadarResourceData.constructResource
            if (radarRecords != null && !radarRecords.isEmpty()) {
                RadarRecord r = radarRecords.values().iterator().next();
                productCode = r.getProductCode();
            } else {
                RequestConstraint productCodeConstraint = getResourceData()
                        .getMetadataMap().get("productCode");
                if (productCodeConstraint.getConstraintType() == ConstraintType.EQUALS)
                    productCode = Integer.parseInt(productCodeConstraint
                            .getConstraintValue());
            }
        } catch (RuntimeException e) {
            // ignore
        }
        return RadarProductFactory.isVelocityProductCode(productCode);
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.raytheon.viz.awipstools.capabilityInterfaces.IRangeableResource#
     * getElevation()
     */
    @Override
    public Measure<?, Length> getElevation() {
        RadarRecord radarRecord = getRadarRecord(displayedDate);

        if (radarRecord != null) {
            return Measure.valueOf(radarRecord.getElevation(), NonSI.FOOT);
        }
        return Measure.valueOf(0.0, NonSI.FOOT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.awipstools.capabilityInterfaces.IRangeableResource#getCenter
     * ()
     */
    @Override
    public Coordinate getCenter() {
        RadarRecord record = getRadarRecord(displayedDate);
        if (record != null) {
            return new Coordinate(record.getLongitude(), record.getLatitude());
        }
        return new Coordinate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.awipstools.capabilityInterfaces.IRangeableResource#getTilt
     * ()
     */
    @Override
    public double getTilt() {
        double tilt = 0.0;
        if (displayedDate != null) {
            tilt = displayedDate.getLevelValue();
        }
        return tilt;
    }
}
