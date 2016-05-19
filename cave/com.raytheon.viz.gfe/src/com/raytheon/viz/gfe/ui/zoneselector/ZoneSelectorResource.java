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
package com.raytheon.viz.gfe.ui.zoneselector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.RGB;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.dataplugin.gfe.db.objects.GridLocation;
import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.util.WorldWrapCorrector;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.maps.rsc.AbstractDbMapResourceData.ColumnDefinition;
import com.raytheon.uf.viz.core.maps.rsc.DbMapResource;
import com.raytheon.uf.viz.core.maps.rsc.DbMapResourceData;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.LabelableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ShadeableCapability;
import com.raytheon.viz.core.rsc.jts.JTSCompiler;
import com.raytheon.viz.core.rsc.jts.JTSCompiler.JTSGeometryData;
import com.raytheon.viz.core.rsc.jts.JTSCompiler.PointStyle;
import com.raytheon.viz.gfe.Activator;
import com.raytheon.viz.gfe.rsc.GFEFonts;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Zone Selector Resource
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2011            randerso    Initial creation
 * Apr 10, 2013     #1854  randerso    Fix for compatibility with PostGIS 2.0
 * May 30, 2013     #2028  randerso    Fixed date line issue with map display
 * Jul 31, 2013     #2239  randerso    Fixed scaling of maps that cross the date line
 * Jan 07, 2014     #2662  randerso    Fixed limitZones (subDomainUGCs) support
 * Feb 18, 2014     #2819  randerso    Removed unnecessary clones of geometries
 * Aug 13, 2014     #3492  mapeters    Updated deprecated createWireframeShape() calls.
 * Aug 14, 2014     #3523  mapeters    Updated deprecated {@link DrawableString#textStyle} 
 *                                     assignments.
 * Aug 21, 2014     #3459  randerso    Restructured Map resource class hierarchy
 * Sep 04, 2014     #3365  ccody       Changes for removing Data_Delivery dependencies
 * Apr 06, 2015     #17340 randerso    Eliminated clipping to GFE domain, code cleanup
 * Jul 13, 2015      4500  rjpeter     Fix SQL Injection concerns.
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public class ZoneSelectorResource extends DbMapResource {
    private static final String EDIT_AREA = "editarea";

    private static final RGB NO_ZONE_COLOR;
    static {
        String s = Activator.getDefault().getPreferenceStore()
                .getString("ZoneCombiner_noZoneColor");
        if (s.isEmpty()) {
            s = "black";
        }
        NO_ZONE_COLOR = RGBColors.getRGBColor(s);
    }

    private static GeometryFactory gf = new GeometryFactory();

    private static PreparedGeometryFactory pgf = new PreparedGeometryFactory();

    public String myWfo;

    class MapQueryJob extends Job {

        private static final int QUEUE_LIMIT = 1;

        class Request {
            IGraphicsTarget target;

            IMapDescriptor descriptor;

            ZoneSelectorResource rsc;

            String query;

            Request(IGraphicsTarget target, IMapDescriptor descriptor,
                    ZoneSelectorResource rsc, String query) {
                this.target = target;
                this.descriptor = descriptor;
                this.rsc = rsc;
                this.query = query;
            }
        }

        public class Result {
            public IWireframeShape outlineShape;

            public IWireframeShape wfoShape;

            public List<LabelNode> labels;

            public IShadedShape[] shapeList;

            public boolean failed;

            public Throwable cause;

            public String query;

            private Result(String query) {
                this.query = query;
                failed = true;
            }
        }

        private final ArrayBlockingQueue<Request> requestQueue = new ArrayBlockingQueue<Request>(
                QUEUE_LIMIT);

        private final ArrayBlockingQueue<Result> resultQueue = new ArrayBlockingQueue<Result>(
                QUEUE_LIMIT);

        private boolean canceled;

        public MapQueryJob() {
            super("Retrieving map...");
        }

        public void request(IGraphicsTarget target, IMapDescriptor descriptor,
                ZoneSelectorResource rsc, String query) {
            if (requestQueue.size() == QUEUE_LIMIT) {
                requestQueue.poll();
            }
            requestQueue.add(new Request(target, descriptor, rsc, query));

            this.cancel();
            this.schedule();
        }

        public Result getLatestResult() {
            return resultQueue.poll();
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
         * IProgressMonitor)
         */
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Request req = requestQueue.poll();
            while (req != null) {
                Result result = new Result(req.query);
                try {

                    // System.out.println(req.query);
                    // long t0 = System.currentTimeMillis();
                    QueryResult mappedResult = DirectDbQuery
                            .executeMappedQuery(req.query, "maps",
                                    QueryLanguage.SQL);

                    // long t1 = System.currentTimeMillis();
                    // System.out.println("Maps DB query took: " + (t1 - t0)
                    // + "ms");

                    List<LabelNode> newLabels = new ArrayList<LabelNode>();

                    Map<String, Geometry> resultingGeoms = new HashMap<String, Geometry>(
                            mappedResult.getResultCount());
                    List<Geometry> wfoGeoms = new ArrayList<Geometry>();

                    int numPoints = 0;
                    int wfoPoints = 0;
                    List<String> limitZones = req.rsc.getLimitZones();
                    WKBReader wkbReader = new WKBReader();
                    for (int i = 0; i < mappedResult.getResultCount(); i++) {
                        if (canceled) {
                            canceled = false;
                            result = null;
                            // System.out.println("MapQueryJob Canceled.");
                            return Status.CANCEL_STATUS;
                        }
                        Geometry g = null;
                        Object obj = mappedResult.getRowColumnValue(i, 0);
                        if (obj instanceof byte[]) {
                            byte[] wkb = (byte[]) obj;
                            g = wkbReader.read(wkb);
                        } else {
                            statusHandler.handle(Priority.ERROR,
                                    "Expected byte[] received "
                                            + obj.getClass().getName() + ": "
                                            + obj.toString() + "\n  query=\""
                                            + req.query + "\"");
                        }

                        if (g != null) {
                            String zoneName = (String) mappedResult
                                    .getRowColumnValue(i, "editarea");
                            if (zoneName == null) {
                                continue;
                            }

                            if ((limitZones != null)
                                    && !limitZones.contains(zoneName)) {
                                continue;
                            }

                            Geometry existingGeom = resultingGeoms
                                    .get(zoneName);
                            if (existingGeom != null) {
                                // continue;
                                numPoints -= existingGeom.getNumPoints();
                                g = mergeGeometry(g, existingGeom);
                            }
                            numPoints += g.getNumPoints();
                            resultingGeoms.put(zoneName, g);

                            if (myWfo != null) {
                                String wfo = (String) mappedResult
                                        .getRowColumnValue(i, "wfo");

                                if (myWfo.equals(wfo)) {
                                    if (existingGeom != null) {
                                        wfoPoints -= existingGeom
                                                .getNumPoints();
                                    }
                                    wfoPoints += g.getNumPoints();
                                    wfoGeoms.add(g);
                                }
                            }

                            ZoneInfo info = req.rsc.getZoneInfo(zoneName);
                            info.setGeometry(g);
                            g.setUserData(zoneName);

                            int numGeometries = g.getNumGeometries();
                            List<Geometry> gList = new ArrayList<Geometry>(
                                    numGeometries);
                            for (int polyNum = 0; polyNum < numGeometries; polyNum++) {
                                Geometry poly = g.getGeometryN(polyNum);
                                gList.add(poly);
                            }
                            // Sort polygons in g so biggest comes first.
                            Collections.sort(gList, new Comparator<Geometry>() {
                                @Override
                                public int compare(Geometry g1, Geometry g2) {
                                    return (int) Math.signum(g2.getEnvelope()
                                            .getArea()
                                            - g1.getEnvelope().getArea());
                                }
                            });

                            for (Geometry poly : gList) {
                                Point point = poly.getInteriorPoint();
                                if (point.getCoordinate() != null) {
                                    LabelNode node = new LabelNode(zoneName,
                                            point, req.target, req.rsc.font);
                                    newLabels.add(node);
                                }
                            }
                        }
                    }

                    IWireframeShape newOutlineShape = req.target
                            .createWireframeShape(false, req.descriptor);
                    newOutlineShape.allocate(numPoints);

                    JTSCompiler outlineCompiler = new JTSCompiler(null,
                            newOutlineShape, req.descriptor);

                    int i = 0;
                    result.shapeList = new IShadedShape[resultingGeoms.size()];
                    for (Geometry g : resultingGeoms.values()) {
                        String zoneName = (String) g.getUserData();
                        ZoneInfo info = req.rsc.getZoneInfo(zoneName);

                        try {
                            outlineCompiler.handle(g);
                        } catch (VizException e) {
                            statusHandler.handle(Priority.PROBLEM,
                                    "Error reprojecting map outline", e);
                        }

                        IShadedShape newShadedShape = computeShape(req.target,
                                req.descriptor, g, info.getColor());
                        info.setShapeIndex(i);
                        result.shapeList[i++] = newShadedShape;
                    }

                    newOutlineShape.compile();

                    result.outlineShape = newOutlineShape;
                    result.labels = newLabels;

                    if (wfoGeoms.size() > 0) {
                        IWireframeShape newWfoShape = req.target
                                .createWireframeShape(false, req.descriptor);
                        newWfoShape.allocate(wfoPoints);

                        JTSCompiler wfoCompiler = new JTSCompiler(null,
                                newWfoShape, req.descriptor);

                        for (Geometry g : wfoGeoms) {
                            try {
                                wfoCompiler.handle(g);
                            } catch (VizException e) {
                                statusHandler.handle(Priority.PROBLEM,
                                        "Error reprojecting map outline", e);
                            }
                        }
                        newWfoShape.compile();
                        result.wfoShape = newWfoShape;
                    }

                    result.failed = false;

                    // long t2 = System.currentTimeMillis();
                    // System.out.println("Wireframe construction took: "
                    // + (t2 - t1) + "ms");
                    // System.out.println("Total map retrieval took: " + (t2 -
                    // t0)
                    // + "ms");
                } catch (Throwable e) {
                    result.cause = e;
                } finally {
                    if (result != null) {
                        if (resultQueue.size() == QUEUE_LIMIT) {
                            resultQueue.poll();
                        }
                        resultQueue.add(result);
                        req.rsc.issueRefresh();
                    }
                }

                req = requestQueue.poll();
            }

            return Status.OK_STATUS;
        }

        /**
         * @param g1
         * @param g2
         * @return
         */
        private Geometry mergeGeometry(Geometry g1, Geometry g2) {
            int numPolys = g1.getNumGeometries() + g2.getNumGeometries();

            Polygon[] polys = new Polygon[numPolys];
            int i = 0;
            for (int n = 0; n < g1.getNumGeometries(); n++) {
                polys[i++] = (Polygon) g1.getGeometryN(n);
            }
            for (int n = 0; n < g2.getNumGeometries(); n++) {
                polys[i++] = (Polygon) g2.getGeometryN(n);
            }

            return gf.createMultiPolygon(polys);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.core.runtime.jobs.Job#canceling()
         */
        @Override
        protected void canceling() {
            super.canceling();

            this.canceled = true;
        }
    }

    private static class ZoneInfo {

        private String groupLabel;

        private RGB color;

        private PreparedGeometry prepGeom;

        private int shapeIndex;

        public ZoneInfo(RGB color) {
            this.color = color;
            this.groupLabel = "";
            this.shapeIndex = -1;
        }

        /**
         * @return the groupLabel
         */
        public String getGroupLabel() {
            return groupLabel;
        }

        /**
         * @param groupLabel
         *            the groupLabel to set
         */
        public void setGroupLabel(String groupLabel) {
            this.groupLabel = groupLabel;
        }

        /**
         * @return the color
         */
        public RGB getColor() {
            return color;
        }

        /**
         * @param color
         *            the color to set
         */
        public void setColor(RGB color) {
            this.color = color;
        }

        /**
         * @return the geometry
         */
        public Geometry getGeometry() {
            Geometry g = null;
            if (prepGeom != null) {
                g = prepGeom.getGeometry();
            }
            return g;
        }

        /**
         * @param geometry
         *            the geometry to set
         */
        public void setGeometry(Geometry geometry) {
            this.prepGeom = pgf.create(geometry);
        }

        /**
         * @return the shapeIndex
         */
        public int getShapeIndex() {
            return shapeIndex;
        }

        /**
         * @param shapeIndex
         *            the shapeIndex to set
         */
        public void setShapeIndex(int shapeIndex) {
            this.shapeIndex = shapeIndex;
        }

        public boolean contains(Point p) {
            if (prepGeom != null) {
                return this.prepGeom.contains(p);
            }
            return false;
        }
    }

    private class LabelTuple {
        public double x;

        public double y;

        public String group;

        public String zone;

        public LabelTuple(double x, double y, String group, String zone) {
            this.x = x;
            this.y = y;
            this.group = group;
            this.zone = zone;
        }
    }

    private final MapQueryJob queryJob;

    private final Map<String, ZoneInfo> zoneData;

    private List<String> limitZones;

    private RGB defaultFillColor;

    private RGB outlineColor;

    private RGB wfoOutlineColor;

    private IWireframeShape wfoShape;

    private IShadedShape shapeList[];

    private final GeometryFactory geomFactory;

    private IGraphicsTarget target;

    private boolean labelZones;

    private boolean labelZoneGroups;

    private Envelope boundingEnvelope;

    private final GridLocation gloc;

    private final WorldWrapCorrector worldWrapCorrector;

    /**
     * @param data
     * @param loadProperties
     * @param gloc
     * @param limitZones
     */
    public ZoneSelectorResource(DbMapResourceData data,
            LoadProperties loadProperties, GridLocation gloc,
            List<String> limitZones) {
        super(data, loadProperties);
        this.zoneData = new HashMap<String, ZoneInfo>();
        this.geomFactory = new GeometryFactory();
        this.queryJob = new MapQueryJob();
        this.defaultFillColor = NO_ZONE_COLOR;
        this.outlineColor = RGBColors.getRGBColor("white");
        this.wfoOutlineColor = RGBColors.getRGBColor("yellow");
        this.gloc = gloc;
        this.limitZones = limitZones;

        GeneralEnvelope env = new GeneralEnvelope(MapUtil.LATLON_PROJECTION);
        env.setEnvelope(-180.0, -90.0, 180.0, 90.0);

        GridGeometry2D latLonGridGeometry = new GridGeometry2D(
                new GeneralGridEnvelope(new int[] { 0, 0 }, new int[] { 360,
                        180 }, false), env);
        this.worldWrapCorrector = new WorldWrapCorrector(latLonGridGeometry);
    }

    private ZoneInfo getZoneInfo(String zoneName) {
        ZoneInfo zoneInfo = this.zoneData.get(zoneName);
        if (zoneInfo == null) {
            zoneInfo = new ZoneInfo(this.defaultFillColor);
            this.zoneData.put(zoneName, zoneInfo);
        }
        return zoneInfo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.maps.rsc.DbMapResource#disposeInternal()
     */
    @Override
    protected void disposeInternal() {
        super.disposeInternal();

        if (this.wfoShape != null) {
            this.wfoShape.dispose();
        }

        if (this.shapeList != null) {
            for (IShadedShape shape : this.shapeList) {
                shape.dispose();
            }
        }
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);
        getCapabilities().removeCapability(ShadeableCapability.class);
        getCapabilities().removeCapability(LabelableCapability.class);
    }

    /**
     * @param zoneName
     * @param groupLabel
     */
    public void setZoneGroupLabel(String zoneName, String groupLabel) {
        ZoneInfo info = zoneData.get(zoneName);
        if (info != null) {
            info.setGroupLabel(groupLabel);
        }
    }

    /**
     * @param zoneName
     * @param color
     */
    public void setZone(String zoneName, RGB color) {
        ZoneInfo info = zoneData.get(zoneName);
        if (info != null) {
            info.setColor(color);

            int index = info.getShapeIndex();
            if ((this.target != null) && (index >= 0)
                    && (index < shapeList.length)) {
                shapeList[index].dispose();
                shapeList[index] = computeShape(this.target, this.descriptor,
                        info.getGeometry(), color);
            }
            issueRefresh();
        }
    }

    /**
     * @param myWfo
     *            the myWfo to set
     */
    public void setMyWfo(String myWfo) {
        this.myWfo = myWfo;
    }

    /**
     * @param defaultFillColor
     *            the defaultFillColor to set
     */
    public void setDefaultFillColor(RGB defaultFillColor) {
        this.defaultFillColor = defaultFillColor;
    }

    /**
     * @param outlineColor
     *            the outlineColor to set
     */
    public void setOutlineColor(RGB outlineColor) {
        this.outlineColor = outlineColor;
    }

    /**
     * @param wfoOutlineColor
     *            the wfoOutlineColor to set
     */
    public void setWfoOutlineColor(RGB wfoOutlineColor) {
        this.wfoOutlineColor = wfoOutlineColor;
    }

    @Override
    protected void paintInternal(IGraphicsTarget aTarget,
            PaintProperties paintProps) throws VizException {
        this.target = aTarget;

        if (font == null) {
            font = GFEFonts.getFont(aTarget, 2);
        }

        PixelExtent screenExtent = (PixelExtent) paintProps.getView()
                .getExtent();

        double simpLev = getSimpLev(paintProps);
        if ((simpLev < lastSimpLev)
                || (lastExtent == null)
                || !lastExtent.getEnvelope().contains(
                        clipToProjExtent(screenExtent).getEnvelope())) {
            if (!paintProps.isZooming()) {
                PixelExtent clippedExtent = clipToProjExtent(screenExtent);
                String query = buildQuery(clippedExtent, simpLev);
                queryJob.request(aTarget, descriptor, this, query);
                lastExtent = clippedExtent;
                lastSimpLev = simpLev;
            }
        }

        MapQueryJob.Result result = queryJob.getLatestResult();
        if (result != null) {
            if (result.failed) {
                lastExtent = null; // force to re-query when re-enabled
                throw new VizException("Error processing map query request: "
                        + result.query, result.cause);
            }
            if (outlineShape != null) {
                outlineShape.dispose();
            }

            if (wfoShape != null) {
                wfoShape.dispose();
            }

            if (shapeList != null) {
                for (IShadedShape shape : shapeList) {
                    shape.dispose();
                }
            }
            outlineShape = result.outlineShape;
            wfoShape = result.wfoShape;
            labels = result.labels;
            shapeList = result.shapeList;
        }

        // draw the shapes
        if (shapeList != null /* && shadedShape.isDrawable() */) {
            aTarget.drawShadedShapes(paintProps.getAlpha(), 1.0f, shapeList);
        }

        if ((outlineShape != null) && outlineShape.isDrawable()
                && getCapability(OutlineCapability.class).isOutlineOn()) {
            aTarget.drawWireframeShape(outlineShape, this.outlineColor,
                    getCapability(OutlineCapability.class).getOutlineWidth(),
                    getCapability(OutlineCapability.class).getLineStyle());
        } else if ((outlineShape == null)
                && getCapability(OutlineCapability.class).isOutlineOn()) {
            issueRefresh();
        }

        if ((wfoShape != null) && wfoShape.isDrawable()
                && getCapability(OutlineCapability.class).isOutlineOn()) {
            aTarget.drawWireframeShape(wfoShape, this.wfoOutlineColor,
                    getCapability(OutlineCapability.class).getOutlineWidth(),
                    getCapability(OutlineCapability.class).getLineStyle());
        } else if ((wfoShape == null)
                && getCapability(OutlineCapability.class).isOutlineOn()) {
            issueRefresh();
        }

        if ((labels != null) && (this.labelZones || this.labelZoneGroups)) {
            double worldToScreenRatio = paintProps.getView().getExtent()
                    .getWidth()
                    / paintProps.getCanvasBounds().width;

            IExtent extent = paintProps.getView().getExtent();
            List<DrawableString> strings = new ArrayList<DrawableString>(
                    labels.size());
            List<LabelTuple> alreadyDrawn = new ArrayList<ZoneSelectorResource.LabelTuple>(
                    labels.size());
            for (LabelNode node : labels) {
                if (extent.contains(node.getLocation())) {

                    String zone = node.getLabel();
                    String group = getZoneInfo(node.getLabel()).getGroupLabel();
                    double x = node.getLocation()[0];
                    double y = node.getLocation()[1];
                    double minDistance = 9999;
                    for (LabelTuple tuple : alreadyDrawn) {
                        if (!tuple.zone.equals(zone)
                                || !tuple.group.equals(group)) {
                            continue;
                        }
                        double distance = Math.abs(tuple.x - x)
                                + Math.abs(tuple.y - y);
                        minDistance = Math.min(distance, minDistance);
                    }
                    if (minDistance > (100 * worldToScreenRatio)) {
                        String[] text = new String[] { "", "" };
                        if (this.labelZones) {
                            text[0] = zone;
                        }
                        if (this.labelZoneGroups) {
                            text[1] = group;
                        }
                        DrawableString ds = new DrawableString(text,
                                RGBColors.getRGBColor("white"));
                        ds.setCoordinates(node.getLocation()[0],
                                node.getLocation()[1]);
                        ds.font = font;
                        ds.horizontalAlignment = HorizontalAlignment.CENTER;
                        ds.verticalAlignment = VerticalAlignment.MIDDLE;
                        ds.addTextStyle(TextStyle.DROP_SHADOW);
                        strings.add(ds);

                        alreadyDrawn.add(new LabelTuple(x, y, group, zone));
                    }
                }
            }

            aTarget.drawStrings(strings);
        }
    }

    protected String buildQuery(PixelExtent extent, double simpLev) {

        DecimalFormat df = new DecimalFormat("0.######");
        String suffix = "_"
                + StringUtils.replaceChars(df.format(simpLev), '.', '_');

        String geometryField = resourceData.getGeomField() + suffix;

        // get the geometry field
        StringBuilder query = new StringBuilder("SELECT AsBinary(");
        query.append(geometryField);
        query.append(") as ");
        query.append(geometryField);

        // add any additional columns
        if (resourceData.getColumns() != null) {
            for (ColumnDefinition column : resourceData.getColumns()) {
                query.append(", ");
                query.append(column);
            }
        }

        // add the geometry table
        query.append(" FROM ");
        query.append(resourceData.getTable());

        // add any constraints
        String[] constraints = resourceData.getConstraints();
        if ((constraints != null) && (constraints.length > 0)) {
            query.append(" WHERE ").append(
                    StringUtils.join(constraints, " AND "));
        }

        query.append(';');

        return query.toString();
    }

    /**
     * returns the zones containing the selected coordinate
     * 
     * @param c
     *            the coordinate
     * @return the zones
     */
    public List<String> getSelectedZones(Coordinate c) {
        List<String> zones = new ArrayList<String>();

        Point p = this.geomFactory.createPoint(c);
        for (Entry<String, ZoneInfo> entry : this.zoneData.entrySet()) {
            if (entry.getValue().contains(p)) {
                zones.add(entry.getKey());
            }
        }
        return zones;
    }

    private IShadedShape computeShape(IGraphicsTarget target,
            IMapDescriptor descriptor, Geometry g, RGB color) {

        IShadedShape newShadedShape = target.createShadedShape(false,
                new GeneralGridGeometry(descriptor.getGridGeometry()), true);
        // new GeneralGridGeometry(descriptor.getGridGeometry()));
        JTSCompiler shapeCompiler = new JTSCompiler(newShadedShape, null,
                descriptor);
        JTSGeometryData geomData = shapeCompiler.createGeometryData();
        geomData.setWorldWrapCorrect(true);
        geomData.setPointStyle(PointStyle.CROSS);

        try {
            geomData.setGeometryColor(color);
            shapeCompiler.handle(g, geomData);
        } catch (VizException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error computing shaded shape", e);
        }
        newShadedShape.compile();

        return newShadedShape;
    }

    /**
     * @return the limitZones
     */
    public List<String> getLimitZones() {
        return limitZones;
    }

    /**
     * @param limitZones
     *            the limitZones to set
     */
    public void setLimitZones(List<String> limitZones) {
        this.limitZones = limitZones;
        issueRefresh();
    }

    /**
     * @param labelZones
     */
    public void setLabelZones(boolean labelZones) {
        this.labelZones = labelZones;
        issueRefresh();
    }

    /**
     * @param labelZoneGroups
     */
    public void setLabelZoneGroups(boolean labelZoneGroups) {
        this.labelZoneGroups = labelZoneGroups;
        issueRefresh();
    }

    /**
     * 
     */
    public List<String> getZoneNames() {
        if (zoneData.isEmpty()) {
            try {
                StringBuilder query = new StringBuilder("SELECT ");

                // add any additional columns
                if (resourceData.getColumns() != null) {
                    int count = 0;

                    for (ColumnDefinition column : resourceData.getColumns()) {
                        if (count > 0) {
                            query.append(", ");
                        }

                        query.append(column);
                        count++;
                    }
                }

                query.append(" FROM ");
                // add the geometry table
                query.append(resourceData.getTable());

                // add any constraints
                String[] constraints = resourceData.getConstraints();
                if ((constraints != null) && (constraints.length > 0)) {
                    query.append(" WHERE ").append(
                            StringUtils.join(constraints, " AND "));
                }

                query.append(';');

                QueryResult mappedResult = DirectDbQuery.executeMappedQuery(
                        query.toString(), "maps", QueryLanguage.SQL);
                if (mappedResult.getColumnNames().containsKey("editarea")) {

                    for (int i = 0; i < mappedResult.getResultCount(); i++) {
                        String zoneName = (String) mappedResult
                                .getRowColumnValue(i, "editarea");
                        getZoneInfo(zoneName);
                    }
                }
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error retrieving zone names", e);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error computing boudning envelope", e);
            }
        }
        List<String> zoneNames = new ArrayList<String>(zoneData.keySet());
        return zoneNames;
    }

    protected String getGeospatialConstraint(String geometryField, Envelope env) {
        StringBuilder constraint = new StringBuilder();

        Geometry g1 = buildBoundingGeometry(gloc);
        if (env != null) {
            g1 = g1.intersection(MapUtil.createGeometry(env));
        }

        constraint.append("ST_Intersects(");
        constraint.append(geometryField);
        constraint.append(", ST_GeomFromText('");
        constraint.append(g1.toText());
        constraint.append("',4326))");

        return constraint.toString();
    }

    /**
     * Get the bounding envelope of all overlapping geometry in CRS coordinates
     * 
     * @return the envelope
     */
    public Envelope getBoundingEnvelope() {
        if (this.boundingEnvelope == null) {
            try {
                this.boundingEnvelope = new Envelope();
                StringBuilder query = new StringBuilder("SELECT ");

                query.append("asBinary(ST_Envelope(");
                query.append(resourceData.getGeomField());
                query.append(")) as extent");

                // add editarea column
                if (resourceData.getColumns() != null) {
                    for (ColumnDefinition column : resourceData.getColumns()) {
                        if (column.getName().equalsIgnoreCase("editarea")) {
                            query.append(", ");
                            query.append(column);
                        }
                    }
                }

                // add the geometry table
                query.append(" FROM ");
                query.append(resourceData.getTable());

                // add any constraints
                String[] constraints = resourceData.getConstraints();
                if ((constraints != null) && (constraints.length > 0)) {
                    query.append(" WHERE ").append(
                            StringUtils.join(constraints, " AND "));
                }

                query.append(';');

                QueryResult mappedResult = DirectDbQuery.executeMappedQuery(
                        query.toString(), "maps", QueryLanguage.SQL);

                WKBReader wkbReader = new WKBReader();
                for (int i = 0; i < mappedResult.getResultCount(); i++) {
                    String zoneName = (String) mappedResult.getRowColumnValue(
                            i, 1);

                    if ((this.limitZones != null)
                            && !this.limitZones.contains(zoneName)) {
                        continue;
                    }

                    byte[] b = (byte[]) mappedResult.getRowColumnValue(i, 0);
                    if (b != null) {
                        Geometry geom = wkbReader.read(b);

                        // world wrap correct the geometry and then
                        // get the envelope of each geometry in the collection
                        geom = this.worldWrapCorrector.correct(geom);

                        for (int n = 0; n < geom.getNumGeometries(); n++) {
                            Geometry g = geom.getGeometryN(n);

                            Envelope env = g.getEnvelopeInternal();

                            ReferencedEnvelope llEnv = new ReferencedEnvelope(
                                    env, MapUtil.LATLON_PROJECTION);
                            ReferencedEnvelope projEnv = llEnv.transform(
                                    gloc.getCrs(), true);

                            this.boundingEnvelope.expandToInclude(projEnv);
                        }
                    }
                }

            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error retrieving bounding envelope", e);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error computing bounding envelope", e);
            }
        }
        return this.boundingEnvelope;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.maps.rsc.DbMapResource#getLevels()
     */
    @Override
    protected double[] getLevels() {
        double[] d = super.getLevels();
        // d = new double[] { d[d.length - 1] };
        return d;
    }

    private Geometry buildBoundingGeometry(GridLocation gloc) {

        try {
            Coordinate ll = MapUtil.gridCoordinateToNative(
                    new Coordinate(0, 0), PixelOrientation.LOWER_LEFT, gloc);
            Coordinate ur = MapUtil.gridCoordinateToNative(
                    new Coordinate(gloc.getNx(), gloc.getNy()),
                    PixelOrientation.LOWER_LEFT, gloc);

            MathTransform latLonToCRS = MapUtil.getTransformFromLatLon(gloc
                    .getCrs());

            Coordinate pole = null;
            double[] output = new double[2];
            try {
                latLonToCRS.transform(new double[] { 0, 90 }, 0, output, 0, 1);
                Coordinate northPole = new Coordinate(output[0], output[1]);

                if ((northPole.x >= ll.x) && (northPole.x <= ur.x)
                        && (northPole.y >= ll.y) && (northPole.y <= ur.y)) {
                    pole = northPole;

                }
            } catch (TransformException e) {
                // north pole not defined in CRS
            }

            if (pole == null) {
                try {
                    latLonToCRS.transform(new double[] { 0, -90 }, 0, output,
                            0, 1);
                    Coordinate southPole = new Coordinate(output[0], output[1]);
                    if ((southPole.x >= ll.x) && (southPole.x <= ur.x)
                            && (southPole.y >= ll.y) && (southPole.y <= ur.y)) {
                        pole = southPole;
                    }
                } catch (TransformException e) {
                    // south pole not defined in CRS
                }
            }

            // compute delta = min cell dimension in meters
            Coordinate cellSize = gloc.gridCellSize();
            double delta = Math.min(cellSize.x, cellSize.y) * 1000;

            Geometry poly;
            if (pole == null) {
                poly = polygonFromGloc(gloc, delta, ll, ur);
            } else {
                // if pole is in the domain split the domain into four quadrants
                // with corners at the pole
                Coordinate[][] quadrant = new Coordinate[4][2];
                quadrant[0][0] = ll;
                quadrant[0][1] = pole;

                quadrant[1][0] = new Coordinate(ll.x, pole.y);
                quadrant[1][1] = new Coordinate(pole.x, ur.y);

                quadrant[2][0] = pole;
                quadrant[2][1] = ur;

                quadrant[3][0] = new Coordinate(pole.x, ll.y);
                quadrant[3][1] = new Coordinate(ur.x, pole.y);

                List<Polygon> polygons = new ArrayList<Polygon>(4);
                for (Coordinate[] q : quadrant) {
                    if ((q[1].x > q[0].x) && (q[1].y > q[0].y)) {
                        polygons.add(polygonFromGloc(gloc, delta, q[0], q[1]));
                    }
                }

                GeometryFactory gf = new GeometryFactory();
                poly = gf.createMultiPolygon(polygons
                        .toArray(new Polygon[polygons.size()]));
            }

            MathTransform crsToLatLon = MapUtil.getTransformToLatLon(gloc
                    .getCrs());
            poly = JTS.transform(poly, crsToLatLon);

            // correct for world wrap
            poly = this.worldWrapCorrector.correct(poly);

            return poly;
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error computing bounding geometry", e);
        }
        return null;
    }

    private Polygon polygonFromGloc(GridLocation gridLoc, double delta,
            Coordinate ll, Coordinate ur) {

        double width = ur.x - ll.x;
        double height = ur.y - ll.y;

        int nx = (int) Math.abs(Math.ceil(width / delta));
        int ny = (int) Math.abs(Math.ceil(height / delta));

        double dx = width / nx;
        double dy = height / ny;

        Coordinate[] coordinates = new Coordinate[(2 * (nx + ny)) + 1];
        int i = 0;
        for (int x = 0; x < nx; x++) {
            coordinates[i++] = new Coordinate((x * dx) + ll.x, ll.y);
        }
        for (int y = 0; y < ny; y++) {
            coordinates[i++] = new Coordinate(ur.x, (y * dy) + ll.y);
        }
        for (int x = nx; x > 0; x--) {
            coordinates[i++] = new Coordinate((x * dx) + ll.x, ur.y);
        }
        for (int y = ny; y > 0; y--) {
            coordinates[i++] = new Coordinate(ll.x, (y * dy) + ll.y);
        }
        coordinates[i++] = coordinates[0];

        GeometryFactory gf = new GeometryFactory();
        LinearRing shell = gf.createLinearRing(coordinates);
        Polygon poly = gf.createPolygon(shell, null);
        return poly;
    }
}
