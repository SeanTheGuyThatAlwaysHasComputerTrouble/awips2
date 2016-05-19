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
package com.raytheon.viz.mpe.ui.rsc;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;

import org.eclipse.swt.graphics.RGB;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.shef.tables.Colorvalue;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.viz.core.DrawableLine;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelCoverage;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.core.data.IRenderedImageCallback;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.viz.hydrocommon.HydroConstants;
import com.raytheon.viz.mpe.ui.MPEDisplayManager;
import com.raytheon.viz.mpe.ui.MPEFontFactory;
import com.raytheon.viz.mpe.ui.actions.DrawDQCStations;
import com.raytheon.viz.mpe.ui.actions.OtherPrecipOptions;
import com.raytheon.viz.mpe.ui.dialogs.QcPrecipOptionsDialog;
import com.raytheon.viz.mpe.util.DailyQcUtils;
import com.raytheon.viz.mpe.util.DailyQcUtils.Pdata;
import com.raytheon.viz.mpe.util.DailyQcUtils.Station;
import com.raytheon.viz.mpe.util.DailyQcUtils.Stn;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * MPEMultiple point resource.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 24, 2008 1748       snaples     Initial creation
 * Jul 24, 2014 3429       mapeters    Updated deprecated drawLine() calls.
 * Aug 11, 2014 3504       mapeters    Replaced deprecated IODataPreparer
 *                                      instances with IRenderedImageCallback
 *                                      and removed unused modifyStation() method.
 * Nov 26, 2014 16889      snaples     Daily QC does not display SNOTEL data.
 * </pre>
 * 
 * @author snaples
 * @version 1.0
 */

public class PointPrecipPlotResource extends
        HydroPointResource<PointPrecipResourceData> implements IMpeResource {

    private static Hashtable<String, Station> dataMap = null;

    private static STRtree strTree = null;

    private static Coordinate selectedCoordinate;

    private MPEFontFactory fontFactory;

    private final DecimalFormat df = new DecimalFormat();

    private final RGB gageColor = new RGB(255, 255, 255);

    private double scaleWidthValue = 0.0;

    private double scaleHeightValue = 0.0;

    private Station gageData = null;

    private static final String[] color_map_a = { "Aquamarine", "OrangeRed",
            "Orange", "Yellow", "VioletRed", "SpringGreen4", "Green3", "Grey",
            "White" };

    private static final String[] color_map_n = { "Grey", "Grey", "Blue",
            "Aquamarine", "LightGreen", "DarkGreen", "Violet", "Purple",
            "Blue", "Blue", "Yellow", "Yellow", "Yellow2", "VioletRed", "Red",
            "White" };

    Pdata pdata[];

    ArrayList<Station> station;
    
    private int time_pos = 0;

    private Hashtable<String, Stn> pdataMap;

    private final DailyQcUtils dqc = DailyQcUtils.getInstance();

    static int prevPcpnDay;

    /**
     * Constructor.
     * 
     * @param name
     *            Resource name
     * @param color
     *            Resource color
     * @param coord
     *            Resource Coordinate
     * @param style
     *            Resource Style
     */
    public PointPrecipPlotResource(PointPrecipResourceData resourceData,
            LoadProperties props) {
        super(resourceData, props);
        pdata = DailyQcUtils.pdata;
        station = DailyQcUtils.precip_stations;
        prevPcpnDay = 0;
        df.setMaximumFractionDigits(2);
        df.setMaximumIntegerDigits(4);
    }

    /**
     * Add a point to this resource.
     * 
     * @param x
     *            The point's x coordinate
     * @param y
     *            The point's y coordinate
     * @param inspectString
     *            String to display when inspection is enabled
     * @param color
     *            The point's color
     */
    public void addPoints() {

        dataMap = new Hashtable<String, Station>();
        pdataMap = new Hashtable<String, Stn>();
        strTree = new STRtree();
        gageData = dqc.new Station();

        if (!DailyQcUtils.precip_stations.isEmpty()) {
            int i = 0;
            for (ListIterator<Station> it = DailyQcUtils.precip_stations.listIterator(); it
                    .hasNext();) {
                gageData = it.next();
                Coordinate xy = new Coordinate();
                xy.x = gageData.lon;
                xy.y = gageData.lat;
                String pm = gageData.parm;
                StringBuilder kv = new StringBuilder(String.valueOf(xy.x));
                kv.append(":");
                kv.append(String.valueOf(xy.y));
                kv.append(":");
                kv.append(pm);
                dataMap.put(kv.toString(), gageData);
                pdataMap.put(kv.toString(),
                        pdata[DailyQcUtils.pcpn_day].stn[i]);

                /* Create a small envelope around the point */
                Coordinate p1 = new Coordinate(xy.x + .02, xy.y + .02);
                Coordinate p2 = new Coordinate(xy.x - .02, xy.y - .02);
                Envelope env = new Envelope(p1, p2);
                ArrayList<Object> data = new ArrayList<Object>();
                data.add(xy);
                data.add("STATION: "
                        + gageData.hb5
                        + " VALUE: "
                        + pdata[DailyQcUtils.pcpn_day].stn[i].frain[time_pos].data);
                strTree.insert(env, data);
                i++;
            }
            prevPcpnDay = DailyQcUtils.pcpn_day;
        }
        // target.setNeedsRefresh(true);
    }

    /**
     * gets the pixel coverage for this image
     * 
     * @return
     */
    private PixelCoverage getPixelCoverage(Coordinate c) {

        double[] centerpixels = descriptor
                .worldToPixel(new double[] { c.x, c.y });
        Coordinate ul = new Coordinate(centerpixels[0] - getScaleWidth(),
                centerpixels[1] - getScaleHeight());
        Coordinate ur = new Coordinate(centerpixels[0] + getScaleWidth(),
                centerpixels[1] - getScaleHeight());
        Coordinate lr = new Coordinate(centerpixels[0] + getScaleWidth(),
                centerpixels[1] + getScaleHeight());
        Coordinate ll = new Coordinate(centerpixels[0] - getScaleWidth(),
                centerpixels[1] + getScaleHeight());

        return new PixelCoverage(ul, ur, lr, ll);
    }

    /**
     * Gets the pixel extent of the rectangle
     * 
     * @param c
     * @return
     */
    private PixelExtent getPixelExtent(Coordinate c) {
        double[] pixels = descriptor.worldToPixel(new double[] { c.x, c.y });
        Coordinate[] coors = new Coordinate[4];
        coors[0] = new Coordinate((pixels[0] - this.getScaleWidth())
                - (this.getScaleWidth() / 2),
                (pixels[1] - this.getScaleHeight())
                        - (this.getScaleHeight() / 2));
        coors[1] = new Coordinate((pixels[0] + this.getScaleWidth())
                - (this.getScaleWidth() / 2),
                (pixels[1] - this.getScaleHeight())
                        - (this.getScaleHeight() / 2));
        coors[2] = new Coordinate((pixels[0] + this.getScaleWidth())
                - (this.getScaleWidth() / 2),
                (pixels[1] + this.getScaleHeight())
                        - (this.getScaleHeight() / 2));
        coors[3] = new Coordinate((pixels[0] - this.getScaleWidth())
                - (this.getScaleWidth() / 2),
                (pixels[1] + this.getScaleHeight())
                        - (this.getScaleHeight() / 2));
        return new PixelExtent(coors);
    }

    /**
     * Draws the plot information
     * 
     * @param c
     * @param gageData
     * @throws VizException
     */
    private void drawPlotInfo(Coordinate c, String key, Station station,
            IGraphicsTarget target, PaintProperties paintProps, RGB color,
            IFont font) throws VizException {

        if ((MPEDisplayManager.getCurrent().isQpf() == true)
                && (DailyQcUtils.points_flag == 1)) {
            int type = DailyQcUtils.plot_view;
            int i = 0;
            int m = 0;
            int dcmode = OtherPrecipOptions.dcmode;
            int tcmode = OtherPrecipOptions.tcmode;
            int dmvalue = dqc.dmvalue;
            int tsmax = DailyQcUtils.tsmax;
            boolean frzlvl_flag = dqc.frzlvl_flag;
            int gage_char[] = DailyQcUtils.gage_char;
            int find_station_flag = dqc.find_station_flag;
            String mbuf = "";
            String tbuf = "";
            String val = "";

            if (MPEDisplayManager.pcpn_time_step == 0) {
                time_pos = DailyQcUtils.pcpn_time;
            } else {
                time_pos = 4;
            }

            if (type == 0) {
                return;
            }

            double[] centerpixels = descriptor.worldToPixel(new double[] { c.x,
                    c.y });
            color = RGBColors.getRGBColor(color_map_n[15]);
            if ((DailyQcUtils.points_flag == 1)
                    && (QcPrecipOptionsDialog.isOpen == true)
                    && (MPEDisplayManager.getCurrent().isQpf() == true)) {
            } else {
                return;
            }
            if ((station.elev >= 0)
                    && (station.elev < DailyQcUtils.elevation_filter_value)) {
                return;
            }

            if ((tcmode == 1) && (pdataMap.get(key).tcons == 1)) {
                return;
            }

            if ((tcmode == 0) && (pdataMap.get(key).tcons == -1)) {
                return;
            }

            if ((dcmode == 0) && (pdataMap.get(key).scons[time_pos] == -1)) {
                return;
            }

            if ((dcmode == 1) && (pdataMap.get(key).scons[time_pos] == 1)) {
                return;
            }

            if ((station.tip == 0) && (gage_char[0] == -1)) {
                return;
            }

            if ((station.tip == 1) && (gage_char[1] == -1)) {
                return;
            }

            for (m = 0; m < tsmax; m++) {
                if (station.parm.substring(3, 5)
                        .equalsIgnoreCase(DailyQcUtils.ts[m].abr)
                        && (DailyQcUtils.dflag[m + 1] == 1)) {
                    break;
                }
            }

            if (m == tsmax) {
                return;
            }

            for (m = 0; m < 9; m++) {

                if ((m == pdataMap.get(key).frain[time_pos].qual)
                        && (DailyQcUtils.qflag[m] == 1)) {
                    break;
                } else if ((m == 7) && (DailyQcUtils.qflag[7] == 1)
                        && (pdataMap.get(key).frain[time_pos].data == -99)
                        && (pdataMap.get(key).frain[time_pos].qual == -99)) {
                    break;
                }

            }

            if (m == 9) {
                return;
            }

            /* locate station in data stream */
            if (((type == 4) || (type == 5))
                    && (pdata[DailyQcUtils.pcpn_day].used[time_pos] == 0)
                    && (pdata[DailyQcUtils.pcpn_day].level == 0)) {
                return;
            }
            if (((type == 4) || (type == 5))
                    && (pdataMap.get(key).frain[time_pos].data < QcPrecipOptionsDialog
                            .getPointFilterValue())
                    && (pdataMap.get(key).frain[time_pos].data != -99)
                    && (pdataMap.get(key).frain[time_pos].qual != -99)) {
                return;
            }

            if (((type == 4) || (type == 5))
                    && (pdataMap.get(key).frain[time_pos].data > QcPrecipOptionsDialog
                            .getPointFilterReverseValue())
                    && (pdataMap.get(key).frain[time_pos].data < 20.00)) {
                return;
            }

            final RGB circleColor = color;
            IImage image = target
                    .initializeRaster(new IRenderedImageCallback() {
                        @Override
                        public RenderedImage getImage() throws VizException {
                            return drawMPECircle(circleColor);
                        }
                    });

            Coordinate idCoor = new Coordinate(centerpixels[0]
                    + (this.getScaleWidth() / 3), centerpixels[1]
                    - this.getScaleHeight());

            target.drawRaster(image, getPixelCoverage(c), paintProps);

            tbuf = "";
            if (type == 1) {
                tbuf = station.hb5;
            }

            else if (type == 2) {
                tbuf = station.parm.substring(3, 5);
            }

            else if (type == 3) {
                tbuf = station.name;

            } else if (type == 4) {
                if ((pdata[DailyQcUtils.pcpn_day].used[time_pos] == 0)
                        && (pdata[DailyQcUtils.pcpn_day].level == 0)) {
                    return;

                }
                if (pdataMap.get(key).frain[time_pos].data == -2) {
                    return;

                }

                /* if point data is missing, use character 'm' */

                mbuf = "";
                if ((pdataMap.get(key).frain[time_pos].data == -99)
                        && (pdataMap.get(key).frain[time_pos].qual == -99)) {
                    mbuf = "m";

                } else {
                    mbuf = String.format("%5.2f",
                            pdataMap.get(key).frain[time_pos].data);

                }
                tbuf = mbuf;

            } else if (type == 5) {
                if ((pdata[DailyQcUtils.pcpn_day].used[time_pos] == 0)
                        && (pdata[DailyQcUtils.pcpn_day].level == 0)) {
                    return;

                }
                if (pdataMap.get(key).frain[time_pos].data == -2) {
                    return;

                }
                mbuf = String.format("%5.2f",
                        pdataMap.get(key).frain[time_pos].stddev);
                tbuf = mbuf;

            }
            if (m == 9) {
                m = 7;

            }

            /* XSetForeground(display,gc,amap[m]); */
            color = RGBColors.getRGBColor(color_map_a[m]);

            // length = strlen(tbuf);
            // text_width = XTextWidth(info_font[4], tbuf, length);

            int xadd = station.xadd;
            int yadd = station.yadd;
            // int xc = 0;
            // int yc = 0;
            IExtent screenExtent = paintProps.getView().getExtent();
            double scale = (screenExtent.getHeight() / paintProps
                    .getCanvasBounds().height);
            DrawableString dstr = new DrawableString("0",
                    new RGB(255, 255, 255));
            dstr.font = font;
            double textHeight = target.getStringsBounds(dstr).getHeight()
                    * scale;
            // double padding = .5 * scale;
            // int text_width = (int) (tbuf.length() * .75);
            Coordinate valCoor = new Coordinate(centerpixels[0]
                    + (this.getScaleWidth() / 3), centerpixels[1]
                    - this.getScaleHeight());
            // if (xadd < 0) {
            // xc = (int) (valCoor.x - text_width);
            // } else {
            // xc = (int) (valCoor.x + 1);
            // }

            // if (yadd < 0) {
            // yc = (int) valCoor.y;
            // } else {
            // yc = (int) valCoor.y + 1;
            // }

            dstr.setText(tbuf, color);
            dstr.horizontalAlignment = HorizontalAlignment.LEFT;
            dstr.verticalAlignment = VerticalAlignment.TOP;
            // orig code
            // dstr.setCoordinates(xc + (.75 * padding) + (text_width * scale),
            // yc
            // + (temp + .75) * textSpace);
            final int textWidthPadding = 9;
            final double verticalSpacingFactor = 1.5;
            double textWidth = target.getStringsBounds(dstr).getWidth();
            double scaledTextWidth = (textWidth + textWidthPadding) * scale;
            double textX = valCoor.x + (xadd * scaledTextWidth);
            double textY = valCoor.y
                    + ((yadd) * verticalSpacingFactor * textHeight)
                    + (textHeight / 2);

            dstr.setCoordinates(textX, textY);
            target.drawStrings(dstr);
            /* XDrawString(display,pix,gc,xc,yc,tbuf,length); */
            // mDrawText(M_EXPOSE, map_number, xc, yc, tbuf);
            if (i == find_station_flag) {
                find_station_flag = -1;
                /* XDrawLine(display,pix,gc,xc,yc,xc+text_width,yc); */
                // mDrawLine(M_EXPOSE, map_number, xc, yc, xc + text_width, yc);
            }

            if (pdataMap.get(key).snoflag[time_pos] != 0) {
                /* XSetForeground(display,gc,amap[1]); */
                color = RGBColors.getRGBColor(color_map_a[1]);

                /* XDrawLine(display,pix,gc,xc,yc,xc+text_width,yc); */
                // mDrawLine(M_EXPOSE, map_number, xc, yc, xc + text_width, yc);
            }

            if (pdataMap.get(key).sflag[time_pos] == 1) {
                /* XSetForeground(display,gc,amap[0]); */
                color = RGBColors.getRGBColor(color_map_a[0]);
                /* XDrawLine(display,pix,gc,xc,yc,xc+text_width,yc); */
                // mDrawLine(M_EXPOSE, map_number, xc, yc, xc + text_width, yc);
            }

            int mm = 0;
            if ((frzlvl_flag == true)
                    && (station.tip == 0)
                    && ((pdataMap.get(key).frain[time_pos].estimate > .005) || (pdataMap
                            .get(key).frain[time_pos].data > .005))) {
                if (time_pos == 4) {
                    for (mm = 0; mm < 4; mm++) {

                        if ((pdataMap.get(key).frzlvl[mm] < -98)
                                || (station.elev < 0)) {
                            continue;
                        }

                        if ((pdataMap.get(key).frzlvl[mm] - dmvalue) < station.elev) {
                            break;
                        }

                    }

                    if (mm == 4) {
                        return;
                    }

                }

                else {

                    if ((pdataMap.get(key).frzlvl[time_pos] < -98)
                            || (station.elev < 0)) {
                        return;
                    }

                    if ((pdataMap.get(key).frzlvl[time_pos] - dmvalue) >= station.elev) {
                        return;
                    }

                }

                /* XSetForeground(display,gc,amap[1]); */
                color = RGBColors.getRGBColor(color_map_a[1]);

                /* XDrawLine(display,pix,gc,xc,yc,xc+text_width,yc); */
                // mDrawLine(M_EXPOSE, map_number, xc, yc, xc + text_width, yc);
                Coordinate stageCoor = new Coordinate(centerpixels[0]
                        + (this.getScaleWidth() / 3), centerpixels[1]
                        + this.getScaleHeight());
                val = df.format(pdataMap.get(key).frain[time_pos].data);
                val = "m";
                // draw the value
                dstr.setText(val, gageColor);
                dstr.setCoordinates(stageCoor.x, stageCoor.y);
                dstr.horizontalAlignment = HorizontalAlignment.LEFT;
                target.drawStrings(dstr);

                // draw the ID
                final BufferedImage gage2 = drawMPECircle(color);
                IImage image2 = target
                        .initializeRaster(new IRenderedImageCallback() {
                            @Override
                            public RenderedImage getImage() throws VizException {
                                return gage2;
                            }
                        });

                idCoor = new Coordinate(centerpixels[0]
                        + (this.getScaleWidth() / 3), centerpixels[1]
                        - this.getScaleHeight());

                dstr.setCoordinates(idCoor.x, idCoor.y);
                dstr.setText(gageData.hb5, gageColor);
                dstr.horizontalAlignment = HorizontalAlignment.LEFT;
                target.drawStrings(dstr);
                target.drawRaster(image2, getPixelCoverage(c), paintProps);
            }
        } else {
            return;
        }
    }

    /**
     * Get the contrasting color to specified color
     * 
     * @param RGB
     * 
     * @return RGB contrast color
     */
    public static RGB getContrast(RGB rgb) {
        RGB xc;
        int r = rgb.red;
        int g = rgb.green;
        int b = rgb.blue;

        // If color is near black set the contrast to white
        if (((r + g + b) / 3) == 0) {
            xc = new RGB(255, 255, 255);
        } else {

            if (rgb.green <= 127) {
                g = 255;
            } else {
                g = 10;
            }

            if (rgb.blue <= 127) {
                b = 255;
            } else {
                b = 10;
            }
            xc = new RGB(r, g, b);
        }
        return xc;
    }

    /**
     * Set the width scalar
     * 
     * @param props
     * @return
     */
    private void setScaleWidth(PaintProperties props) {
        double screenToWorldWidthRatio = props.getCanvasBounds().width
                / props.getView().getExtent().getWidth();
        scaleWidthValue = (IMAGE_WIDTH / 2.0) / screenToWorldWidthRatio;
    }

    /**
     * get the scale width value
     * 
     * @return
     */
    private double getScaleWidth() {
        return scaleWidthValue;
    }

    /**
     * Get the scalar height
     * 
     * @return
     */
    private double getScaleHeight() {
        return scaleHeightValue;
    }

    /**
     * Set the height scalar
     * 
     * @param props
     * @return
     */
    private void setScaleHeight(PaintProperties props) {
        double screenToWorldHeightRatio = props.getCanvasBounds().height
                / props.getView().getExtent().getHeight();
        scaleHeightValue = (IMAGE_HEIGHT / 2.0) / screenToWorldHeightRatio;
    }

    /**
     * Paint method called to display this resource.
     * 
     * @param target
     *            The IGraphicsTarget
     * @param paintProps
     *            The Paint Properties
     * @throws VizException
     */
    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        MPEDisplayManager displayMgr = getResourceData().getMPEDisplayManager();
        // Fonts are shared and cached, no need to init or dispose
        IFont font = fontFactory.getMPEFont(MPEDisplayManager.getFontId());

        if ((DailyQcUtils.points_flag == 1) && (displayMgr.isQpf() == true)) {
            Iterator<String> iter = dataMap.keySet().iterator();

            while (iter.hasNext()) {

                if (displayMgr.isQpf() == true) {
                    String key = iter.next();
                    Coordinate c = new Coordinate();
                    String[] spKey = key.split(":", 5);
                    c.x = Double.parseDouble(spKey[0]);
                    c.y = Double.parseDouble(spKey[1]);
                    double[] pixel = descriptor.worldToPixel(new double[] {
                            c.x, c.y });

                    if (paintProps.getView().getExtent().contains(pixel)) {
                        setScaleWidth(paintProps);
                        setScaleHeight(paintProps);

                        RGB color = new RGB(255, 255, 255);
                        drawPlotInfo(c, key, dataMap.get(key), target,
                                paintProps, color, font);

                        if (getSelectedCoordinate() != null) {
                            Envelope env = new Envelope(getSelectedCoordinate());
                            List<?> elements = strTree.query(env);
                            if (elements.size() > 0) {
                                Iterator<?> iter2 = elements.iterator();
                                /* Take the first one in the list */
                                if (iter2.hasNext()) {
                                    /* element 0 = Coordinate, 1 = inspectString */
                                    ArrayList<?> data = (ArrayList<?>) iter2
                                            .next();
                                    PixelExtent pe = this
                                            .getPixelExtent((Coordinate) data
                                                    .get(0));
                                    target.drawRect(pe,
                                            HydroConstants.SQUARE_COLOR, 2, 1);
                                }
                            }
                        }
                    }
                } else {
                    return;
                }
            }
            target.clearClippingPlane();
            drawQCLegend(target, paintProps, font);
            target.setupClippingPlane(paintProps.getClippingPane());
        }
    }

    private void drawQCLegend(IGraphicsTarget target,
            PaintProperties paintProps, IFont font) throws VizException {
        // TODO this screen location code is borrowed from MPELegendResource...
        // should it be put into a shared class, possibly a paint
        // properties method?

        IExtent screenExtent = paintProps.getView().getExtent();
        double scale = (screenExtent.getHeight() / paintProps.getCanvasBounds().height);
        DrawableString string = new DrawableString("0");
        string.font = font;
        double textHeight = target.getStringsBounds(string).getHeight() * scale;
        double padding = 3.2 * scale;
        double textSpace = textHeight + padding;
        double cmapHeight = textHeight * 1.25;
        double legendHeight = cmapHeight + (2.0 * textSpace) + (2.0 * padding);
        double y1 = screenExtent.getMinY() + legendHeight;
        double x1 = screenExtent.getMinX() + padding;
        RGB color = null;
        String label = "";
        int[] funct = dqc.funct;
        DrawableLine[] lines = new DrawableLine[typename.length];
        for (int i = 0; i < typename.length; i++) {
            color = RGBColors.getRGBColor(color_map_a[funct[i]]);
            lines[i] = new DrawableLine();
            lines[i].setCoordinates(x1, y1 + (i * textSpace));
            lines[i].addPoint(x1 + (2.5 * padding), y1 + (i * textSpace));
            lines[i].basics.color = color;
            lines[i].width = 35;
            label = typename[i];
            color = RGBColors.getRGBColor(color_map_n[15]);
            double xLoc = x1 + (4 * padding);
            double yLoc = y1 + ((i + .45) * textSpace);
            string.setText(label, color);
            string.setCoordinates(xLoc, yLoc);
            string.horizontalAlignment = HorizontalAlignment.LEFT;
            string.verticalAlignment = VerticalAlignment.BOTTOM;
            target.drawStrings(string);
        }
        target.drawLine(lines);

    }

    /**
     * Set the selected coordinate
     * 
     * @param selectedCoordinate
     */
    public static void setSelectedCoordinate(Coordinate selCoordinate) {
        selectedCoordinate = selCoordinate;
    }

    /**
     * Selected coordinate
     * 
     * @return
     */
    public static Coordinate getSelectedCoordinate() {
        return selectedCoordinate;
    }

    /**
     * Inspect method called when moused over while inspect is enabled
     * 
     * @param coord
     *            The coordinate of the inspection
     */
    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        Envelope env = new Envelope();
        try {
            env = new Envelope(coord.asLatLon());
        } catch (TransformException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<?> elements = strTree.query(env);
        if (elements.size() > 0) {
            Iterator<?> iter = elements.iterator();
            while (iter.hasNext()) {
                ArrayList<?> list = (ArrayList<?>) iter.next();
                if (list.get(1) instanceof String) {
                    return (String) list.get(1);
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Interrogate method called when user clicks on a location
     * 
     * @param coord
     *            The coordinates of the mouse click
     */
    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate rcoord)
            throws VizException {

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.map.rsc.PointResource#init(com.raytheon.uf.viz
     * .core.IGraphicsTarget)
     */
    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        fontFactory = new MPEFontFactory(target, this);
        /* Retrieve the precip colormap. */
        List<Colorvalue> colorSet = getResourceData().getColorSet();
        ColorMap colorMap = new ColorMap(colorSet.size());
        colorMap.setName("24hGRID_PRECIP");
        DataMappingPreferences dmPref = new DataMappingPreferences();
        int i = 0;
        for (Colorvalue cv : colorSet) {
            RGB rgb = RGBColors.getRGBColor(cv.getColorname().getColorName());
            colorMap.setColor(i, new Color(rgb.red / 255f, rgb.green / 255f,
                    rgb.blue / 255f));

            DataMappingEntry entry = new DataMappingEntry();
            entry.setPixelValue((double) i);
            entry.setDisplayValue(cv.getId().getThresholdValue());
            dmPref.addEntry(entry);

            i++;
        }

        dmPref.getEntries().get(0).setLabel("");
        dmPref.getEntries().get(1).setLabel("");

        ColorMapCapability cmc = getCapability(ColorMapCapability.class);
        ColorMapParameters parameters = cmc.getColorMapParameters();
        if (parameters == null) {
            parameters = new ColorMapParameters();
            cmc.setColorMapParameters(parameters);
        }
        parameters.setColorMap(colorMap);
        parameters.setDataMapping(dmPref);

        Unit<?> displayUnit = NonSI.INCH;
        Unit<?> dataUnit = NonSI.INCH;
        parameters.setDataUnit(dataUnit);
        parameters.setDisplayUnit(displayUnit);
        parameters.setImageUnit(dmPref.getImageUnit(displayUnit));
        parameters.setFormatString("0.00");

        parameters.setColorMapMax(parameters.getColorMap().getSize() - 1);
        parameters.setColorMapMin(0);
        parameters.setDataMax(parameters.getColorMap().getSize() - 1);
        parameters.setDataMin(0);
        addPoints();
    }

    public int getSize() {
        return dataMap.size();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.rsc.IVizResource#getName()
     */
    @Override
    public String getName() {
        if (DrawDQCStations.qcmode == "") {
            return "No Data Available";
        }

        return DrawDQCStations.qcmode;
    }

    /**
     * draw the station circle
     * 
     * @param image
     * @return
     */
    private static BufferedImage drawMPECircle(RGB color) {
        // make circle in center
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        Rectangle2D.Double rect = new Rectangle2D.Double(0, 0,
                image.getWidth(), image.getHeight());
        g.fill(rect);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        // always red
        g.setColor(convertR(color));
        g.fillOval(0, 0, IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);

        return image;
    }

    /**
     * convert RGB to Color
     * 
     * @param color
     * @return
     */
    public static java.awt.Color convertR(RGB color) {
        int blue = color.blue;
        int green = color.green;
        int red = color.red;

        return new java.awt.Color(red, green, blue);
    }

    /**
     * convert Color to RGB
     * 
     * @param color
     * @return
     */
    public static RGB convertC(Color color) {
        int blue = (int) (color.getBlue() * 255f);
        int green = (int) (color.getGreen() * 255f);
        int red = (int) (color.getRed() * 255f);

        return new RGB(red, green, blue);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.rsc.IVizResource#dispose()
     */
    @Override
    protected void disposeInternal() {
        fontFactory.dispose();
    }
}
