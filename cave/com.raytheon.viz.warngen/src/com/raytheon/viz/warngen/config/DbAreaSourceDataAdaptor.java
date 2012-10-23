package com.raytheon.viz.warngen.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.measure.converter.UnitConverter;

import org.geotools.referencing.GeodeticCalculator;

import com.raytheon.uf.common.dataplugin.warning.config.PathcastConfiguration;
import com.raytheon.uf.common.dataplugin.warning.config.PointSourceConfiguration;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.geospatial.SpatialQueryResult;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.warngen.PreferenceUtil;
import com.raytheon.viz.warngen.gis.ClosestPoint;
import com.raytheon.viz.warngen.gis.GisUtil;
import com.raytheon.viz.warngen.gis.GisUtil.Direction;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * 
 * SOFTWARE HISTORY
 * 
 * Date Ticket# Engineer Description ------------ ---------- -----------
 * --------------------------
 * 
 * @author jsanchez Sep 25, 2012 #15425 Qinglu Lin Updated createClosestPoint().
 * 
 */
public class DbAreaSourceDataAdaptor extends AbstractDbSourceDataAdaptor {

    private static final String useDirectionField = "usedirs";

    private static final String suppressedDirectionsField = "supdirs";

    private static final String cwaField = "cwa";

    public DbAreaSourceDataAdaptor(PathcastConfiguration pathcastConfiguration,
            UnitConverter distanceToMeters, Geometry searchArea,
            String localizedSite) throws VizException {
        super(pathcastConfiguration, distanceToMeters, searchArea,
                localizedSite);
    }

    public DbAreaSourceDataAdaptor(
            PointSourceConfiguration pointSourceConfiguration,
            Geometry searchArea, String localizedSite) throws VizException {
        super(pointSourceConfiguration, searchArea, localizedSite);
    }

    /**
     * 
     */
    @Override
    protected Set<String> createSpatialQueryField(String pointField,
            String[] sortBy) {
        Set<String> ptFields = new HashSet<String>();
        ptFields.add(pointField);
        ptFields.add(useDirectionField);
        ptFields.add(suppressedDirectionsField);

        List<String> fields = null;
        if (sortBy != null && sortBy.length > 0) {
            fields = Arrays.asList(sortBy);
        } else {
            fields = new ArrayList<String>(0);
        }

        // Sort fields don't exist in the db.
        for (String field : fields) {
            if (undatabasedSortableFields.contains(field.toUpperCase()) == false) {
                ptFields.add(field.toUpperCase());
            }
        }

        return ptFields;
    }

    /**
     * Creates a closest point object.
     */
    @Override
    protected ClosestPoint createClosestPoint(String pointField,
            Set<String> ptFields, SpatialQueryResult ptRslt) {
        Map<String, Object> attributes = ptRslt.attributes;

        String name = String.valueOf(attributes.get(pointField));
        Coordinate point = ptRslt.geometry.getCoordinate();
        int population = getPopulation(ptFields, attributes);
        int warngenlev = getWangenlev(ptFields, attributes);
        List<String> partOfArea = getPartOfArea(ptFields, attributes,
                ptRslt.geometry);
        int gid = getGid(ptFields, attributes);

        return new ClosestPoint(name, point, population, warngenlev,
                partOfArea, gid);
    }

    /**
     * Processes the filter to set the localized site.
     */
    @Override
    protected Map<String, RequestConstraint> processFilterSubstitution(
            Map<String, RequestConstraint> filter) {
        if (filter != null) {
            // Process substitutes for filter
            for (RequestConstraint rc : filter.values()) {
                rc.setConstraintValue(PreferenceUtil.substitute(
                        rc.getConstraintValue(), localizedSite));
            }
        }

        if (filter == null) {
            filter = new HashMap<String, RequestConstraint>();
        }

        filter.put(cwaField, new RequestConstraint(localizedSite));

        return filter;
    }

    /**
     * Determines the part of area impacted if the userDirectionField is set to
     * true.
     * 
     * @param ptFields
     * @param attributes
     * @param geom
     * @return
     */
    private List<String> getPartOfArea(Set<String> ptFields,
            Map<String, Object> attributes, Geometry geom) {
        List<String> partOfArea = null;

        boolean userDirections = Boolean.valueOf(String.valueOf(attributes
                .get(useDirectionField)));
        if (userDirections) {
            PreparedGeometry prepGeom = PreparedGeometryFactory.prepare(geom);
            if (prepGeom.intersects(searchArea) && !prepGeom.within(searchArea)) {
                Geometry intersection = searchArea.intersection(geom);
                partOfArea = GisUtil.asStringList(calculateLocationPortion(
                        geom, intersection, gc));

                if (attributes.get(suppressedDirectionsField) != null) {
                    String suppressedDirections = String.valueOf(
                            attributes.get(suppressedDirectionsField))
                            .toLowerCase();
                    // supdirs can be 'nse', for example
                    // TODO create an enum constructor for Directions
                    for (int i = 0; i < suppressedDirections.length(); i++) {
                        switch (suppressedDirections.charAt(i)) {
                        case 'n':
                            partOfArea.remove(Direction.NORTH.toString());
                            break;
                        case 's':
                            partOfArea.remove(Direction.SOUTH.toString());
                            break;
                        case 'e':
                            partOfArea.remove(Direction.EAST.toString());
                            break;
                        case 'w':
                            partOfArea.remove(Direction.WEST.toString());
                            break;
                        }
                    }
                }
            }
        }

        if (partOfArea != null && !partOfArea.isEmpty()) {
            return partOfArea;
        }

        return null;
    }

    /**
     * Helper class to store cardinal ranges
     * 
     * @author jsanchez
     * 
     */
    private static class CardinalRange {
        public EnumSet<Direction> directions;

        public double lowRange;

        public double highRange;

        public CardinalRange(EnumSet<Direction> directions, double lowRange,
                double highRange) {
            this.directions = directions;
            this.lowRange = lowRange;
            this.highRange = highRange;
        }
    }

    private static CardinalRange[] ranges = new CardinalRange[] {
            new CardinalRange(EnumSet.of(Direction.NORTH), 0, 22.5),
            new CardinalRange(EnumSet.of(Direction.NORTH, Direction.EAST),
                    22.5, 67.5),
            new CardinalRange(EnumSet.of(Direction.EAST), 67.5, 112.5),
            new CardinalRange(EnumSet.of(Direction.SOUTH, Direction.EAST),
                    112.5, 157.5),
            new CardinalRange(EnumSet.of(Direction.SOUTH), 157.5, 202.5),
            new CardinalRange(EnumSet.of(Direction.SOUTH, Direction.WEST),
                    202.5, 247.5),
            new CardinalRange(EnumSet.of(Direction.WEST), 247.5, 292.5),
            new CardinalRange(EnumSet.of(Direction.NORTH, Direction.WEST),
                    292.5, 337.5),
            new CardinalRange(EnumSet.of(Direction.NORTH), 337.5, 360) };

    /**
     * Calculates the cardinal directions of a location.
     * 
     * @param geom
     * @param intersection
     * @param gc
     * @return
     */
    private static EnumSet<Direction> calculateLocationPortion(Geometry geom,
            Geometry intersection, GeodeticCalculator gc) {
        EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
        Coordinate geomCentroid = geom.convexHull().getCentroid()
                .getCoordinate();
        Coordinate intersectCentroid = intersection.convexHull().getCentroid()
                .getCoordinate();

        gc.setStartingGeographicPoint(geomCentroid.x, geomCentroid.y);
        gc.setDestinationGeographicPoint(intersectCentroid.x,
                intersectCentroid.y);

        Envelope envelope = geom.getEnvelopeInternal();
        double centerThresholdX = envelope.getWidth() * 0.10;
        double centerThresholdY = envelope.getHeight() * 0.10;
        double distanceX = Math.abs(intersectCentroid.x - geomCentroid.x);
        double distanceY = Math.abs(intersectCentroid.y - geomCentroid.y);

        if (distanceX > centerThresholdX || distanceY > centerThresholdY) {
            // Convert azimuth from -180/180 to 0/360
            double degrees = gc.getAzimuth();
            if (degrees < 0) {
                degrees += 360;
            }

            for (CardinalRange range : ranges) {
                if (degrees > range.lowRange && degrees <= range.highRange) {
                    directions = range.directions;
                    break;
                }
            }
        }

        return directions;
    }
}
