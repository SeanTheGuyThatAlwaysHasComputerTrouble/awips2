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
package com.raytheon.viz.hydrocommon.util;

import java.awt.Rectangle;

import org.opengis.metadata.spatial.PixelOrientation;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.ReferencedObject.Type;
import com.raytheon.uf.common.hydro.spatial.HRAP;
import com.raytheon.uf.common.hydro.spatial.HRAPCoordinates;
import com.raytheon.uf.common.hydro.spatial.HRAPSubGrid;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Lat/Lon to HRAP and HRAP to Lat/Lon conversion for HydroBase.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 15, 2009 2772       mpduff      Initial creation
 * Jan 07, 2016 5217       mpduff      Added getGridCellPolygon and HRAPSubGrid processing.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */
public class HrapUtil {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(HrapUtil.class);

    private static final double NMILE_PER_DEG_LAT = 60;

    private static final double PI = 3.14159;

    private static final double RAD_PER_DEG = PI / 180;

    private static final double KM_PER_NMILE = 1.852;

    private static final double KM_PER_MILE = 1.609344;

    private static final double MILES_PER_NMILE = KM_PER_NMILE / KM_PER_MILE;

    private static HRAPSubGrid subGrid;

    /**
     * Convert a lat/lon set to HRAP coordinates.
     * 
     * @param latlon
     *            The latitude/longitude Coordinate
     * @return HRAP Coordinate
     */
    public static Coordinate latLonToHrap(Coordinate latlon) {
        HRAP hrap = HRAP.getInstance();
        Coordinate gridCoord = null;
        try {
            gridCoord = hrap.latLonToGridCoordinate(latlon,
                    PixelOrientation.LOWER_LEFT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gridCoord;
    }

    /**
     * Convert an HRAP coordinate to lat/lon.
     * 
     * @param gridCoord
     *            the HRAP Coordinate
     * @return Coordinate - the lat/lon Coordinate
     */
    public static Coordinate hrapToLatLon(Coordinate gridCoord) {
        HRAP hrap = HRAP.getInstance();
        Coordinate latlon = new Coordinate(0, 0);
        try {
            latlon = hrap.gridCoordinateToLatLon(gridCoord,
                    PixelOrientation.LOWER_LEFT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return latlon;
    }

    /**
     * Get the polygon that covers the HRAP grid cell.
     * 
     * @param gridCoord
     *            The coordinate of the grid cell
     * @return
     */
    public static Polygon getGridCellPolygon(Coordinate gridCoord) {
        Coordinate[] coors = new Coordinate[5];

        coors[0] = getHRAPLatLon(new Coordinate(gridCoord.x, gridCoord.y));
        coors[1] = getHRAPLatLon(new Coordinate(gridCoord.x + 1, gridCoord.y));
        coors[2] = getHRAPLatLon(new Coordinate(gridCoord.x + 1,
                gridCoord.y + 1));
        coors[3] = getHRAPLatLon(new Coordinate(gridCoord.x, gridCoord.y + 1));

        // complete the square
        coors[4] = coors[0];

        return MapUtil.getPolygon(coors);
    }

    /**
     * compute area of an Hrap bin.
     * 
     * Algorithm:
     * 
     * Convert row and column to lat-lon coords Convert row + 1 and column + 1
     * to lat lon coords Get the differences in lat and lon Convert to nautical
     * miles.
     * 
     * get square miles by multiplying the x and y nautical miles by the
     * conversion factor to square miles.
     * 
     * @param row
     *            The row
     * @param col
     *            The column
     * @return The area
     */
    public static double getHrapBinArea(Coordinate coord) {
        double area;

        double latLength;
        double lonLength;

        double nmileXLength;
        double nmileYLength;

        double lat1;
        double lat2;
        double lon1;
        double lon2;

        /* get lat and lon positions */
        Coordinate ll = hrapToLatLon(coord);
        Coordinate ll2 = hrapToLatLon(new Coordinate(coord.x + 1, coord.y + 1));

        lat1 = ll.y;
        lat2 = ll2.y;
        lon1 = ll.x;
        lon2 = ll2.x;

        /* get differences in lat and lon */
        latLength = Math.abs(lat1 - lat2);
        lonLength = Math.abs(lon1 - lon2);

        /* get the nautical miles from lat and lon lengths */
        nmileYLength = NMILE_PER_DEG_LAT * latLength;
        nmileXLength = lonLength * NMILE_PER_DEG_LAT
                * (Math.cos(RAD_PER_DEG * ((lat1 + lat2) / 2.0)));

        /* convert to miles and get the square */
        area = (MILES_PER_NMILE * nmileYLength)
                * (MILES_PER_NMILE * nmileXLength);

        return area;
    }

    /**
     * Get the HRAP subgrid
     * 
     * @param grib
     *            factor
     * @throws Exception
     */
    public static HRAPSubGrid getHrapSubGrid() throws Exception {
        return getHrapSubGrid(1);
    }

    /**
     * Get the HRAP subgrid
     * 
     * @param grib
     *            factor
     * @throws Exception
     */
    public static HRAPSubGrid getHrapSubGrid(int hrapGribFactor)
            throws Exception {
        if (subGrid == null) {
            Rectangle rect = HRAPCoordinates.getHRAPCoordinates();
            subGrid = new HRAPSubGrid(rect, hrapGribFactor);
        }

        return subGrid;
    }

    /**
     * Gets the gribPoint
     * 
     * @param gridPoint
     * @return
     */
    private static Coordinate getHRAPLatLon(Coordinate gridPoint) {
        try {
            ReferencedCoordinate rc = new ReferencedCoordinate(gridPoint,
                    getHrapSubGrid().getHRAP().getGridGeometry(),
                    Type.GRID_CORNER);
            gridPoint = rc.asLatLon();
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Unable translate grid coordinate:  " + gridPoint);
        }
        return gridPoint;
    }
}
