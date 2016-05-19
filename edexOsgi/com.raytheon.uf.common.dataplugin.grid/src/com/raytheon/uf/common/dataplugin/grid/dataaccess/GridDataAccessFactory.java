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
package com.raytheon.uf.common.dataplugin.grid.dataaccess;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.measure.unit.Unit;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.EnvelopeProjectionException;
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.grid.IGridData;
import com.raytheon.uf.common.dataaccess.impl.AbstractGridDataPluginFactory;
import com.raytheon.uf.common.dataaccess.impl.DefaultGeometryData;
import com.raytheon.uf.common.dataaccess.impl.DefaultGridData;
import com.raytheon.uf.common.dataaccess.util.PDOUtil;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.grid.GridConstants;
import com.raytheon.uf.common.dataplugin.grid.GridRecord;
import com.raytheon.uf.common.dataplugin.grid.dataquery.GridQueryAssembler;
import com.raytheon.uf.common.dataplugin.grid.mapping.DatasetIdMapper;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.mapping.LevelMapper;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.util.SubGridGeometryCalculator;
import com.raytheon.uf.common.gridcoverage.GridCoverage;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.parameter.Parameter;
import com.raytheon.uf.common.parameter.mapping.ParameterMapper;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.util.mapping.Mapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Data access factory for accessing data from the Grid plugin as grid types.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jan 17, 2013           bsteffen    Initial creation
 * Feb 14, 2013  1614     bsteffen    Refactor data access framework to use
 *                                    single request.
 * Feb 04, 2014  2672     bsteffen    Enable requesting subgrids.
 * Jul 30, 2014  3184     njensen     Renamed valid identifiers to optional
 * Sep 09, 2014  3356     njensen     Remove CommunicationException
 * Oct 16, 2014  3598     nabowle     Accept level identifiers.
 * Oct 21, 2014  3755     nabowle     Add getAvailable levels and parameters.
 * Feb 13, 2015  4124     mapeters    Inherits IDataFactory.
 * Feb 23, 2015  4016     bsteffen    Add support for geometry requests.
 * Feb 27, 2015  4179     mapeters    Use AbstractDataPluginFactory.getAvailableValues().
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class GridDataAccessFactory extends AbstractGridDataPluginFactory {

    private static final String NAMESPACE = "namespace";

    private static final String[] VALID_IDENTIFIERS = {
            GridConstants.DATASET_ID, GridConstants.SECONDARY_ID,
            GridConstants.ENSEMBLE_ID, NAMESPACE,
            GridConstants.MASTER_LEVEL_NAME, GridConstants.LEVEL_ONE,
            GridConstants.LEVEL_TWO };

    @Override
    public String[] getOptionalIdentifiers() {
        return VALID_IDENTIFIERS;
    }

    @Override
    protected Map<String, RequestConstraint> buildConstraintsFromRequest(
            IDataRequest request) {
        Map<String, RequestConstraint> result = new HashMap<String, RequestConstraint>();

        Map<String, Object> identifiers = request.getIdentifiers();
        try {
            GridQueryAssembler assembler = new GridQueryAssembler();
            if (identifiers != null && identifiers.containsKey(NAMESPACE)) {
                assembler.setNamespace(identifiers.get(NAMESPACE).toString());
            }
            if (request.getParameters() != null) {
                for (String parameter : request.getParameters()) {
                    assembler.setParameterAbbreviation(parameter);
                    mergeConstraintMaps(assembler.getConstraintMap(), result);
                }
                // clear fields so it doesn't merge the last one again.
                assembler.setParameterAbbreviation(null);
            }

            if (request.getLevels() != null) {
                checkForLevelConflict(request.getLevels(),
                        request.getIdentifiers());

                for (Level level : request.getLevels()) {
                    assembler.setMasterLevelName(level.getMasterLevel()
                            .getName());
                    assembler.setLevelUnits(level.getMasterLevel()
                            .getUnitString());
                    assembler.setLevelOneValue(level.getLevelonevalue());
                    assembler.setLevelTwoValue(level.getLeveltwovalue());
                    // TODO Theoretically merging these could end badly if there
                    // are multiple master levels or if some levels have
                    // leveltwo and others don't. But for now pretend that never
                    // happens since it probably won't.
                    mergeConstraintMaps(assembler.getConstraintMap(), result);
                }
                // clear fields so it doesn't merge the last one again.
                assembler.setMasterLevelName(null);
                assembler.setLevelUnits(null);
                assembler.setLevelOneValue(null);
                assembler.setLevelTwoValue(null);
            }

            if (request.getLocationNames() != null) {
                for (String loc : request.getLocationNames()) {
                    assembler.setDatasetId(loc);
                    mergeConstraintMaps(assembler.getConstraintMap(), result);
                }
                // clear fields so it doesn't merge the last one again.
                assembler.setDatasetId(null);

            }

            if (identifiers != null) {
                if (identifiers.containsKey(GridConstants.DATASET_ID)) {
                    assembler.setDatasetId(identifiers.get(
                            GridConstants.DATASET_ID).toString());
                }
                if (identifiers.containsKey(GridConstants.ENSEMBLE_ID)) {
                    assembler.setEnsembleId(identifiers.get(
                            GridConstants.ENSEMBLE_ID).toString());
                }
                if (identifiers.containsKey(GridConstants.SECONDARY_ID)) {
                    assembler.setSecondaryId(identifiers.get(
                            GridConstants.SECONDARY_ID).toString());
                }
                if (identifiers.containsKey(GridConstants.MASTER_LEVEL_NAME)) {
                    assembler.setMasterLevelName(identifiers.get(
                            GridConstants.MASTER_LEVEL_NAME).toString());
                }
                if (identifiers.containsKey(GridConstants.LEVEL_ONE)) {
                    assembler.setLevelOneValue((Double) identifiers
                            .get(GridConstants.LEVEL_ONE));
                }
                if (identifiers.containsKey(GridConstants.LEVEL_TWO)) {
                    assembler.setLevelTwoValue((Double) identifiers
                            .get(GridConstants.LEVEL_TWO));
                }
            }
            mergeConstraintMaps(assembler.getConstraintMap(), result);
        } catch (CommunicationException e) {
            throw new DataRetrievalException(e);
        }
        return result;
    }

    /**
     * Check for possible level conflicts.
     * 
     * @param levels
     *            The request levels. Assumed to not be null.
     * @param identifiers
     *            The request identifiers.
     * @throws DataRetrievalException
     *             if levels is not empty and at least one of the
     *             {@link GridConstants#MASTER_LEVEL_NAME},
     *             {@link GridConstants#LEVEL_ONE}, or
     *             {@link GridConstants#LEVEL_TWO} identifiers is specified.
     */
    private void checkForLevelConflict(Level[] levels,
            Map<String, Object> identifiers) {
        if (levels.length > 0
                && identifiers != null
                && (identifiers.containsKey(GridConstants.MASTER_LEVEL_NAME)
                        || identifiers.containsKey(GridConstants.LEVEL_ONE) || identifiers
                            .containsKey(GridConstants.LEVEL_TWO))) {
            throw new DataRetrievalException(
                    "Conflict between the request levels and request "
                            + "identifiers. Please set the levels either as"
                            + " identifiers or as levels, not both.");
        }
    }

    /**
     * Copy all constraints from source to target. If target already contains a
     * constraint for a key then merge the values into target.
     * 
     * @param target
     * @param source
     */
    private void mergeConstraintMaps(Map<String, RequestConstraint> source,
            Map<String, RequestConstraint> target) {
        for (Entry<String, RequestConstraint> sourceEntry : source.entrySet()) {
            String key = sourceEntry.getKey();
            RequestConstraint sourceConstraint = sourceEntry.getValue();
            RequestConstraint targetConstraint = target.get(sourceEntry
                    .getKey());
            if (targetConstraint == null) {
                target.put(key, sourceConstraint);
            } else if (!sourceConstraint.equals(targetConstraint)) {
                targetConstraint.setConstraintType(ConstraintType.IN);
                // TODO we don't necessarily want to always add. This could
                // result in something like IN MB,FHAG,MB,MB,MB, but we also
                // don't want to parse the in list all the time.
                targetConstraint.addToConstraintValueList(sourceConstraint
                        .getConstraintValue());
            }
        }
    }

    @Override
    protected IGridData constructGridDataResponse(IDataRequest request,
            PluginDataObject pdo, GridGeometry2D gridGeometry,
            DataSource dataSource) {
        if (pdo instanceof GridRecord == false) {
            throw new DataRetrievalException(this.getClass().getSimpleName()
                    + " cannot handle " + pdo.getClass().getSimpleName());
        }
        GridRecord gridRecord = (GridRecord) pdo;

        String parameter = gridRecord.getParameter().getAbbreviation();
        String datasetId = gridRecord.getDatasetId();

        Level level = gridRecord.getLevel();
        Map<String, Object> identifiers = request.getIdentifiers();
        if (identifiers != null && identifiers.containsKey(NAMESPACE)) {
            // perform reverse mappings so the parameters and levels that are
            // returned match exactly what was requested.
            String namespace = request.getIdentifiers().get(NAMESPACE)
                    .toString();
            List<String> requestParameters = Arrays.asList(request
                    .getParameters());
            parameter = reverseResolveMapping(ParameterMapper.getInstance(),
                    parameter, namespace, requestParameters);

            if (identifiers.containsKey(GridConstants.DATASET_ID)) {
                List<String> requestedDatasets = Arrays.asList(identifiers.get(
                        GridConstants.DATASET_ID).toString());
                datasetId = reverseResolveMapping(
                        DatasetIdMapper.getInstance(), datasetId, namespace,
                        requestedDatasets);
            }
            for (Level requestLevel : request.getLevels()) {
                double levelone = requestLevel.getLevelonevalue();
                double leveltwo = requestLevel.getLeveltwovalue();
                String master = requestLevel.getMasterLevel().getName();
                Unit<?> unit = requestLevel.getMasterLevel().getUnit();

                // instead of doing reverse mapping just do a forward
                // mapping on everything they requested and compare to what
                // they got.
                Set<Level> levels = LevelMapper.getInstance().lookupLevels(
                        master, namespace, levelone, leveltwo, unit);
                for (Level l : levels) {
                    if (level.equals(l)) {
                        level = requestLevel;
                        break;
                    }
                }

                if (level == requestLevel) {
                    // we found one.
                    break;
                }
            }
        }

        DefaultGridData defaultGridData = new DefaultGridData(dataSource,
                gridGeometry);
        defaultGridData.setDataTime(pdo.getDataTime());
        defaultGridData.setParameter(parameter);
        defaultGridData.setLevel(level);
        defaultGridData.setLocationName(datasetId);
        defaultGridData.setUnit(gridRecord.getParameter().getUnit());
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(GridConstants.DATASET_ID, datasetId);
        attributes.put(GridConstants.SECONDARY_ID, gridRecord.getSecondaryId());
        attributes.put(GridConstants.ENSEMBLE_ID, gridRecord.getEnsembleId());

        defaultGridData.setAttributes(attributes);
        return defaultGridData;
    }

    private String reverseResolveMapping(Mapper mapper, String base,
            String namespace, Collection<String> requested) {
        // attempt to find a valid mapping that they requested.
        for (String alias : mapper.lookupAliases(base, namespace)) {
            if (requested.contains(alias)) {
                return alias;
            }
        }
        return base;
    }

    @Override
    public String[] getAvailableLocationNames(IDataRequest request) {
        return getAvailableValues(request, GridConstants.DATASET_ID,
                String.class);
    }

    /**
     * Get the available levels.
     */
    @Override
    public Level[] getAvailableLevels(IDataRequest request) {
        return getAvailableValues(request, GridConstants.LEVEL, Level.class);
    }

    /**
     * Get the available parameter abbreviations.
     */
    @Override
    public String[] getAvailableParameters(IDataRequest request) {
        return getAvailableValues(request,
                GridConstants.PARAMETER_ABBREVIATION, String.class);
    }

    /**
     * Return geometry data for grid records. Each grid cell is treated as a
     * single Point Geometry. An envelope must be provided.
     */
    @Override
    protected IGeometryData[] getGeometryData(IDataRequest request,
            DbQueryResponse dbQueryResponse) {
        if (request.getEnvelope() == null) {
            throw new IncompatibleRequestException(
                    "Requests for Grid data as a geometry must provide a bounding envelope.");
        }
        Map<GridGeometryKey, Set<GridRecord>> sortedRecords = new HashMap<>();
        for (GridRecord record : dbQueryResponse
                .getEntityObjects(GridRecord.class)) {
            GridGeometryKey key = new GridGeometryKey(record);
            Set<GridRecord> records = sortedRecords.get(key);
            if (records == null) {
                records = new HashSet<>();
                sortedRecords.put(key, records);
            }
            records.add(record);
        }
        List<IGeometryData> result = new ArrayList<>(
                dbQueryResponse.getNumResults());
        for (Entry<GridGeometryKey, Set<GridRecord>> entry : sortedRecords
                .entrySet()) {
            result.addAll(getGeometryData(request, entry.getKey(),
                    entry.getValue()));

        }
        return result.toArray(new IGeometryData[0]);
    }

    /**
     * Get geometry data for many related records. The result will be a single
     * {@link IGeometryData} for each grid cell in the request envelope.
     * Multiple records will be represented as seperate paramters within each
     * IGeometryData.
     */
    private Collection<IGeometryData> getGeometryData(IDataRequest request,
            GridGeometryKey key, Set<GridRecord> records) {
        ReferencedEnvelope requestEnv = new ReferencedEnvelope(
                request.getEnvelope(), DefaultGeographicCRS.WGS84);
        Point point = findRequestPoint(key.getGridCoverage(),
                request.getEnvelope());
        if (point != null) {
            return Collections.singleton(getGeometryData(point, key, records));
        } else {
            SubGridGeometryCalculator subGrid = calculateSubGrid(requestEnv,
                    key.getGridCoverage().getGridGeometry());
            if (subGrid != null && !subGrid.isEmpty()) {
                return getGeometryData(subGrid, key, records);
            } else {
                return Collections.emptyList();

            }
        }
    }

    /**
     * Get geometry data for a single point.
     */
    private IGeometryData getGeometryData(Point point, GridGeometryKey key,
            Set<GridRecord> records) {
        DefaultGeometryData data = key.toGeometryData();
        DirectPosition2D llPoint = findResponsePoint(key.getGridCoverage(),
                point.x, point.y);
        data.setGeometry(new GeometryFactory().createPoint(new Coordinate(
                llPoint.x, llPoint.y)));
        data.setLocationName(data.getLocationName() + "-" + point.x + ","
                + point.y);
        Request request = Request.buildPointRequest(point);
        populateGeometryData(records, request,
                new DefaultGeometryData[] { data });
        return data;
    }

    /**
     * Get geometry data for multiple points within a subgrid that has been
     * calculated from a request envelope.
     */
    private Collection<IGeometryData> getGeometryData(
            SubGridGeometryCalculator subGrid, GridGeometryKey key,
            Set<GridRecord> records) {
        GridEnvelope2D gridRange = subGrid.getSubGridGeometry2D()
                .getGridRange2D();
        DefaultGeometryData[] data = new DefaultGeometryData[(int) (gridRange
                .getWidth() * gridRange.getHeight())];
        GeometryFactory geometryFactory = new GeometryFactory();
        int index = 0;
        for (int y = (int) gridRange.getMinY(); y < gridRange.getMaxY(); y += 1) {
            for (int x = (int) gridRange.getMinX(); x < gridRange.getMaxX(); x += 1) {
                data[index] = key.toGeometryData();
                DirectPosition2D llPoint = findResponsePoint(
                        key.getGridCoverage(), x, y);
                data[index].setGeometry(geometryFactory
                        .createPoint(new Coordinate(llPoint.x, llPoint.y)));
                data[index].setLocationName(data[index].getLocationName() + "-"
                        + x + "," + y);
                index += 1;
            }
        }
        Request request = Request.buildSlab(subGrid.getGridRangeLow(true),
                subGrid.getGridRangeHigh(false));
        populateGeometryData(records, request, data);
        return Arrays.<IGeometryData> asList(data);
    }

    /**
     * Populate one or more DefaultGeometryData by requesting data for the
     * specified request for each grid record. Each GridRecord is added to each
     * DefaultGeometryData as a separate parameter. The order of the
     * DefaultGeometryDatas must be the same as the order of the data returned
     * from the provided request.
     */
    private static void populateGeometryData(Set<GridRecord> records,
            Request request, DefaultGeometryData[] data) {
        for (GridRecord record : records) {
            try {
                Parameter parameter = record.getParameter();
                IDataRecord dataRecord = PDOUtil.getDataRecord(record, "Data",
                        request);
                if (dataRecord instanceof FloatDataRecord) {
                    float[] rawArray = ((FloatDataRecord) dataRecord)
                            .getFloatData();
                    if (rawArray.length != data.length) {
                        throw new DataRetrievalException(
                                "Unexpected response of size "
                                        + rawArray.length
                                        + " when expected size is "
                                        + data.length + " for record " + record);
                    }
                    for (int i = 0; i < data.length; i += 1) {
                        data[i].addData(parameter.getAbbreviation(),
                                rawArray[i], parameter.getUnit());
                    }
                } else {
                    String type = dataRecord == null ? "null" : dataRecord
                            .getClass().getSimpleName();
                    throw new DataRetrievalException("Unexpected record type("
                            + type + ") for " + record);
                }
            } catch (Exception e) {
                throw new DataRetrievalException(
                        "Failed to retrieve the IDataRecord for GridRecord: "
                                + record.toString(), e);
            }
        }
    }

    /**
     * After a grid point has been requested, this will determine the Geometry
     * for that point.
     */
    private DirectPosition2D findResponsePoint(GridCoverage coverage, int x,
            int y) {
        try {
            MathTransform grid2crs = coverage.getGridGeometry().getGridToCRS();
            MathTransform crs2ll = CRS.findMathTransform(coverage.getCrs(),
                    DefaultGeographicCRS.WGS84, true);
            DirectPosition2D point = new DirectPosition2D(x, y);
            grid2crs.transform(point, point);
            crs2ll.transform(point, point);
            return point;
        } catch (TransformException | FactoryException e) {
            throw new EnvelopeProjectionException(
                    "Error determining point from envelope: ", e);
        }
    }

    /**
     * Find a single point(grid cell) to request. This will return a result only
     * if the envelope is so small that it is entirely within a single grid cell
     * and it overlaps the area of the coverage. It tests for this by converting
     * the upper left and lower right corners of the envelope into grid space
     * and returning the point only if both convert to the same point.
     */
    private Point findRequestPoint(GridCoverage coverage, Envelope envelope) {
        try {
            MathTransform ll2crs = CRS.findMathTransform(
                    DefaultGeographicCRS.WGS84, coverage.getCrs(), true);
            MathTransform crs2grid = coverage.getGridGeometry()
                    .getCRSToGrid2D();
            double[] testPoints = { envelope.getMinX(), envelope.getMinY(),
                    envelope.getMaxX(), envelope.getMaxY() };
            ll2crs.transform(testPoints, 0, testPoints, 0, 2);
            crs2grid.transform(testPoints, 0, testPoints, 0, 2);
            int minX = (int) Math.round(testPoints[0]);
            int minY = (int) Math.round(testPoints[1]);
            int maxX = (int) Math.round(testPoints[2]);
            int maxY = (int) Math.round(testPoints[3]);
            GridEnvelope2D gridRange = coverage.getGridGeometry()
                    .getGridRange2D();
            if (minX == maxX && minY == maxY && gridRange.contains(minX, minY)) {
                return new Point(minX, minY);
            } else {
                return null;
            }
        } catch (TransformException | FactoryException e) {
            throw new EnvelopeProjectionException(
                    "Error determining point from envelope: ", e);
        }
    }

    /**
     * A class for organizing {@link GridRecord}s into groups which can be
     * combined into a single {@link IGeometryData} object.
     */
    private static final class GridGeometryKey {

        private final Level level;

        private final DataTime dataTime;

        private final String datasetId;

        private final GridCoverage coverage;

        private final int hashCode;

        public GridGeometryKey(GridRecord record) {
            this.level = record.getLevel();
            this.dataTime = record.getDataTime();
            this.datasetId = record.getDatasetId();
            this.coverage = record.getLocation();
            final int prime = 31;
            int hashCode = 1;
            hashCode = prime * hashCode
                    + ((coverage == null) ? 0 : coverage.hashCode());
            hashCode = prime * hashCode
                    + ((dataTime == null) ? 0 : dataTime.hashCode());
            hashCode = prime * hashCode
                    + ((datasetId == null) ? 0 : datasetId.hashCode());
            hashCode = prime * hashCode
                    + ((level == null) ? 0 : level.hashCode());
            this.hashCode = hashCode;
        }

        public DefaultGeometryData toGeometryData() {
            DefaultGeometryData geometryData = new DefaultGeometryData();
            geometryData.setLevel(level);
            geometryData.setDataTime(dataTime);
            geometryData.setLocationName(datasetId);
            return geometryData;
        }

        public GridCoverage getGridCoverage() {
            return coverage;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GridGeometryKey other = (GridGeometryKey) obj;
            if (coverage == null) {
                if (other.coverage != null)
                    return false;
            } else if (!coverage.equals(other.coverage))
                return false;
            if (dataTime == null) {
                if (other.dataTime != null)
                    return false;
            } else if (!dataTime.equals(other.dataTime))
                return false;
            if (datasetId == null) {
                if (other.datasetId != null)
                    return false;
            } else if (!datasetId.equals(other.datasetId))
                return false;
            if (level == null) {
                if (other.level != null)
                    return false;
            } else if (!level.equals(other.level))
                return false;
            return true;
        }

    }

}
