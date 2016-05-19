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
package com.raytheon.viz.aviation.climatology;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;

/**
 * This class displays the Ceiling/Visibility month canvas composite.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 28 FEB 2008  938        lvenable    Initial creation
 * 12 Aug 2013  #2256      lvenable    Moved getCigVisDistImage() to parent class.
 * 
 * </pre>
 * 
 * @author lvenable
 * @version 1.0
 * 
 */
public class CigVisByMonthCanvasComp extends CigVisCanvasComp {
    /**
     * Parent Composite.
     */
    private Composite parent;

    /**
     * Month data.
     */
    private CigVisDistDataManager data;

    /**
     * Number of cell data columns.
     */
    private int numCellColumns = 13;

    /**
     * Graph width offset.
     */
    private int graphWidthOffset = 780 % numCellColumns;

    /**
     * Width of the graph.
     */
    private int graphWidth = 780 - graphWidthOffset;

    /**
     * Height of the graph.
     */
    private int graphHeight = 500;

    /**
     * Graph X coordinate.
     */
    private int graphXCoord = 100;

    /**
     * Graph Y coordinate.
     */
    private int graphYCoord = 50;

    /**
     * Data cell height.
     */
    private int cellHeight = 15;

    /**
     * Data cell width.
     */
    private int cellWidth = graphWidth / numCellColumns;

    /**
     * Cell character X offset.
     */
    private int cellCharXOffset = 15;

    /**
     * Cell character Y offset.
     */
    private int cellCharYOffset = 1;

    /**
     * Cell label width.
     */
    private int cellLabelWidth = 60;

    /**
     * Cell label character offset.
     */
    private int cellLabelCharOffset = 4;

    /**
     * Graph bar offset.
     */
    private int barOffset = 4;

    /**
     * Graph bar width.
     */
    private int barWidth = cellWidth - 8;

    /**
     * Main label above the graph.
     */
    private String mainLabel = "";

    /**
     * MVFR color.
     */
    private Color mvfrColor;

    /**
     * IFR color.
     */
    private Color ifrColor;

    /**
     * LIFR color.
     */
    private Color lifrColor;

    /**
     * VLIFR color.
     */
    private Color vlifrColor;

    /**
     * Starting graph percentage.
     */
    private int maxPercent = 100;

    private float maxPercentInData = 0;

    /**
     * Flag indicating of the widget needs to be drawn.
     */
    private boolean drawLegend = true;

    /**
     * Number of pixels per increment of data.
     */
    double pixelPerInc = (double) graphHeight / (double) maxPercent;

    /**
     * Small font height.
     */
    private int fontHeightSm;

    /**
     * Average width of the large font.
     */
    private int fontAveWidthLrg;

    /**
     * Array of months for the data cells.
     */
    private String[] monthsArray = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * By Month tab composite.
     */
    private CigVisByMonthTabComp tabComp;

    /**
     * Constructor.
     * 
     * @param parent
     *            Parent composite.
     */
    public CigVisByMonthCanvasComp(Composite parent,
            CigVisDistDataManager data, CigVisByMonthTabComp tab) {
        super(parent, SWT.BORDER);

        this.tabComp = tab;
        this.data = data;
        this.parent = parent;

        init();
    }

    /**
     * Initialize method.
     */
    private void init() {
        mvfrColor = new Color(parent.getDisplay(), 0, 255, 0);
        ifrColor = new Color(parent.getDisplay(), 255, 255, 0);
        lifrColor = new Color(parent.getDisplay(), 255, 0, 0);
        vlifrColor = new Color(parent.getDisplay(), 171, 0, 255);

        this.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                mvfrColor.dispose();
                ifrColor.dispose();
                lifrColor.dispose();
                vlifrColor.dispose();
            }
        });
    }

    /**
     * Set the month data.
     * 
     * @param monthData
     *            The Month data array.
     */
    public void setCigVisData(CigVisDistDataManager data) {
        this.data = data;
    }

    /**
     * Set the graph scale percent.
     * 
     * @param po
     *            Percent occurrence.
     */
    public void setMaxPercentOccurrence(int po) {
        maxPercent = po;
    }

    /**
     * Redraw the canvas.
     */
    public void redrawCanvas() {
        this.redrawCigVisCanvas();
    }

    /**
     * Draw the graph legend based on the flag passed in.
     * 
     * @param flag
     *            True draws the legend, false does not.
     */
    public void drawLegend(boolean flag) {
        drawLegend = flag;
        this.redrawCigVisCanvas();
    }

    private String hourStr(int startHour, int endHour) {
        int numHours = endHour - startHour;

        if (numHours < 1) {
            numHours += 24;
        }

        if (numHours == 1) {
            return Integer.toString(startHour);
        } else {
            return String.format("%d-%d", startHour, endHour);
        }
    }

    /**
     * Draw the graph on the canvas.
     * 
     * @param gc
     *            Graphical context.
     */
    @Override
    public void drawCanvas(GC gc) {
        int startYear = data.getStartYear();
        int endYear = data.getEndYear();
        String site = data.getSite();

        CigVisDistDataManager.Element element = tabComp.getDialog()
                .getSelectedElement();
        int startHour = tabComp.getStartHour();
        int endHour = tabComp.getEndHour();
        mainLabel = String.format("%s %s %sZ (%d-%d)", site,
                elements[element.ordinal()], hourStr(startHour, endHour),
                startYear, endYear);

        pixelPerInc = (double) graphHeight / (double) maxPercent;

        // ----------------------------
        // Get drawing values
        // ----------------------------
        Font smallFont = getSmallFont();
        Font largeFont = getLargeFont();

        gc.setFont(smallFont);
        fontHeightSm = gc.getFontMetrics().getHeight();

        gc.setFont(largeFont);
        fontAveWidthLrg = gc.getFontMetrics().getAverageCharWidth();

        // -------------------------------
        // Fill in the canvas background
        // -------------------------------
        gc.setBackground(parent.getDisplay().getSystemColor(
                SWT.COLOR_WIDGET_BACKGROUND));
        gc.fillRectangle(0, 0, getCanvasWidth(), getCanvasHeight());

        // -----------------------------------------------
        // Draw the graph area.
        // -----------------------------------------------
        gc.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gc.fillRectangle(graphXCoord, graphYCoord, graphWidth, graphHeight);

        gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        gc.drawRectangle(graphXCoord, graphYCoord, graphWidth, graphHeight);

        // ----------------------------------------------
        // Draw values grid
        // ----------------------------------------------

        drawValuesGrid(gc);

        // -------------------------------------------------------
        // Draw the main graph label located above the chart
        // -------------------------------------------------------
        int mainLabelXCoord = graphWidth / 2 + graphXCoord - mainLabel.length()
                / 2 * fontAveWidthLrg;
        gc.drawString(mainLabel, mainLabelXCoord, 10, true);

        // ----------------------------------------------
        // Draw the Months in the cells
        // ----------------------------------------------
        gc.setFont(getSmallFont());

        // Draw the Months
        for (int x = 0; x < numCellColumns - 1; ++x) {
            gc.drawString(String.format("%3S", monthsArray[x]), graphXCoord
                    + cellWidth * x + cellCharXOffset, graphYCoord
                    + graphHeight + cellCharYOffset, true);
        }

        gc.drawString("Annual", graphXCoord + cellWidth * (numCellColumns - 1)
                + cellCharXOffset - 4, graphYCoord + graphHeight
                + cellCharYOffset, true);

        // ----------------------------------------------
        // Draw the cell label colors and text
        // ----------------------------------------------

        // MVFR cell label
        gc.setBackground(mvfrColor);
        gc.fillRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight, cellLabelWidth, cellHeight);

        gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        gc.drawRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight, cellLabelWidth, cellHeight);

        // IFR cell label
        gc.setBackground(ifrColor);
        gc.fillRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight * 2, cellLabelWidth, cellHeight);

        gc.drawRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight * 2, cellLabelWidth, cellHeight);

        // LIFR cell label
        gc.setBackground(lifrColor);
        gc.fillRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight * 3, cellLabelWidth, cellHeight);

        gc.drawRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight * 3, cellLabelWidth, cellHeight);

        // VLIFR cell label
        gc.setBackground(vlifrColor);
        gc.fillRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight * 4, cellLabelWidth, cellHeight);

        gc.drawRectangle(graphXCoord - cellLabelWidth, graphYCoord
                + graphHeight + cellHeight * 4, cellLabelWidth, cellHeight);

        // Add the label text
        gc.setForeground(getTextColor(mvfrColor.getRGB()));
        gc.drawString("MVFR", graphXCoord - cellLabelWidth
                + cellLabelCharOffset, graphYCoord + graphHeight + cellHeight
                + cellCharYOffset, true);

        gc.setForeground(getTextColor(ifrColor.getRGB()));
        gc.drawString("IFR",
                graphXCoord - cellLabelWidth + cellLabelCharOffset, graphYCoord
                        + graphHeight + cellHeight * 2 + cellCharYOffset, true);

        gc.setForeground(getTextColor(lifrColor.getRGB()));
        gc.drawString("LIFR", graphXCoord - cellLabelWidth
                + cellLabelCharOffset, graphYCoord + graphHeight + cellHeight
                * 3 + cellCharYOffset, true);

        gc.setForeground(getTextColor(vlifrColor.getRGB()));
        gc.drawString("VLIFR", graphXCoord - cellLabelWidth
                + cellLabelCharOffset, graphYCoord + graphHeight + cellHeight
                * 4 + cellCharYOffset, true);

        // ------------------------------------------------------------
        // Draw the bars and the data values in the cells
        // ------------------------------------------------------------

        drawGraphBarsAndDataCellValues(gc, startHour, endHour);

        // ------------------------------------------------------------
        // Draw the Percent labels and the dash lines across the graph
        // ------------------------------------------------------------
        int maxPercentLbl = maxPercent;
        gc.setLineStyle(SWT.LINE_DOT);

        int offset = 10;

        if (maxPercent <= 10) {
            offset = 1;
        } else if (maxPercent <= 20) {
            offset = 2;
        } else if (maxPercent <= 50) {
            offset = 5;
        }

        for (int x = 0; x <= maxPercentLbl; x += offset) {
            int yCoord = (int) Math.round(graphYCoord + graphHeight
                    - pixelPerInc * x);

            if (yCoord >= graphYCoord) {
                gc.drawString(String.format("%3d", x), graphXCoord - 25, yCoord
                        - fontHeightSm / 2, true);
                gc.drawLine(graphXCoord, yCoord, graphXCoord + graphWidth,
                        yCoord);
            }
        }

        if (maxPercentLbl % 10 != 0) {
            int yCoord = (int) Math.round(graphYCoord + graphHeight
                    - pixelPerInc * maxPercentLbl);
            gc.drawString(String.format("%3d", maxPercentLbl),
                    graphXCoord - 25, yCoord - fontHeightSm / 2, true);
            gc.drawLine(graphXCoord, yCoord, graphXCoord + graphWidth, yCoord);
        }

        gc.setLineStyle(SWT.LINE_SOLID);

        // ------------------------------------------------
        // Draw the legend
        // ------------------------------------------------

        if (drawLegend == true) {
            drawGraphLegend(gc, graphXCoord, graphYCoord, graphWidth,
                    mvfrColor, ifrColor, lifrColor, vlifrColor);
        }

        // ----------------------------------------------
        // Draw the Percent Occurrence label
        // ----------------------------------------------
        gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        gc.setFont(getMediumFont());
        Transform t = new Transform(gc.getDevice());
        t.translate(35, 375); // new origin
        t.rotate(-90f);
        gc.setTransform(t);
        gc.drawText("Percent Occurrence", 0, 0, true);
        t.dispose();
    }

    /**
     * This method draws all of the grid cells where the data will be located.
     * 
     * @param gc
     *            Graphical context.
     */
    private void drawValuesGrid(GC gc) {
        // Draw white background
        gc.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        gc.fillRectangle(graphXCoord, graphYCoord + graphHeight, graphWidth,
                cellHeight * 5);

        // Draw the black outline
        gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
        gc.drawRectangle(graphXCoord, graphYCoord + graphHeight, graphWidth,
                cellHeight * 5);

        // Draw the horizontal grid lines
        for (int x = 1; x < 5; ++x) {
            gc.drawLine(graphXCoord,
                    graphYCoord + graphHeight + cellHeight * x, graphXCoord
                            + graphWidth, graphYCoord + graphHeight
                            + cellHeight * x);
        }

        // Draw the vertical grid lines
        for (int x = 0; x < numCellColumns; ++x) {
            gc.drawLine(graphXCoord + cellWidth * x, graphYCoord + graphHeight,
                    graphXCoord + cellWidth * x, graphYCoord + graphHeight
                            + cellHeight * 5);
        }
    }

    /**
     * Draw the bar graphs and the data in the grid cells.
     * 
     * @param gc
     *            Graphical context.
     */
    private void drawGraphBarsAndDataCellValues(GC gc, int startHour,
            int endHour) {
        int hour = startHour;
        int numHours = endHour - startHour;

        if (numHours < 1) {
            numHours += 24;
        }

        CigVisDistDataManager.Element element = tabComp.getDialog()
                .getSelectedElement();
        float[][] dataArray = data.getDataByMonth(element, hour, numHours);
        float[] vfrArray = dataArray[4];
        float[] mvfrArray = dataArray[3];
        float[] ifrArray = dataArray[2];
        float[] lifrArray = dataArray[1];
        float[] vlifrArray = dataArray[0];

        for (int i = 0; i < vfrArray.length; i++) {
            float t = vfrArray[i] + mvfrArray[i] + ifrArray[i] + lifrArray[i]
                    + vlifrArray[i];
            vfrArray[i] = (vfrArray[i] / t) * 100;
            mvfrArray[i] = (mvfrArray[i] / t) * 100;
            ifrArray[i] = (ifrArray[i] / t) * 100;
            lifrArray[i] = (lifrArray[i] / t) * 100;
            vlifrArray[i] = (vlifrArray[i] / t) * 100;

            maxPercentInData = Math.max((100 - vfrArray[i]), maxPercentInData);
            if (Float.isNaN(maxPercentInData)) {
                maxPercentInData = 0;
            }
        }

        int yStartPos = graphYCoord + graphHeight;

        gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));

        boolean done = false;
        int barLengthOffset = 0;

        //
        // Draw the data for each month
        //
        for (int x = 0; x < (mvfrArray.length - 1); ++x) {
            done = false;
            barLengthOffset = 0;

            // ------
            // VLIFR
            // ------

            // Draw the VLIFR label
            gc.drawString(String.format("%02d", Math.round(vlifrArray[x])),
                    graphXCoord + cellWidth * x + cellCharXOffset, graphYCoord
                            + graphHeight + cellHeight * 4 + cellCharYOffset,
                    true);

            // Draw the VLIFR bar
            if (vlifrArray[x] != 0) {
                gc.setBackground(vlifrColor);
                yStartPos = yStartPos
                        - (int) Math.round(vlifrArray[x] * pixelPerInc);

                if (yStartPos < graphYCoord) {
                    barLengthOffset = graphYCoord - yStartPos;
                    yStartPos = graphYCoord;
                }

                gc.fillRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(vlifrArray[x] * pixelPerInc)
                                - barLengthOffset);

                gc.drawRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(vlifrArray[x] * pixelPerInc)
                                - barLengthOffset);

                if (yStartPos == graphYCoord) {
                    yStartPos = graphYCoord + graphHeight;
                    done = true;
                }
            }

            // ------
            // LIFR
            // ------

            // Draw the LIFR label
            gc.drawString(String.format("%02d", Math.round(lifrArray[x])),
                    graphXCoord + cellWidth * x + cellCharXOffset, graphYCoord
                            + graphHeight + cellHeight * 3 + cellCharYOffset,
                    true);

            // Draw the LIFR bar
            if (lifrArray[x] != 0 && done == false) {
                gc.setBackground(lifrColor);
                yStartPos = yStartPos
                        - (int) Math.round(lifrArray[x] * pixelPerInc);

                if (yStartPos < graphYCoord) {
                    barLengthOffset = graphYCoord - yStartPos;
                    yStartPos = graphYCoord;
                }

                gc.fillRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(lifrArray[x] * pixelPerInc)
                                - barLengthOffset);

                gc.drawRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(lifrArray[x] * pixelPerInc)
                                - barLengthOffset);

                if (yStartPos == graphYCoord) {
                    yStartPos = graphYCoord + graphHeight;
                    done = true;
                    ;
                }
            }

            // ------
            // IFR
            // ------

            // Draw the IFR label
            gc.drawString(String.format("%02d", Math.round(ifrArray[x])),
                    graphXCoord + cellWidth * x + cellCharXOffset, graphYCoord
                            + graphHeight + cellHeight * 2 + cellCharYOffset,
                    true);

            // Draw the IFR bar
            if (ifrArray[x] != 0 && done == false) {
                gc.setBackground(ifrColor);
                yStartPos = yStartPos
                        - (int) Math.round(ifrArray[x] * pixelPerInc);

                if (yStartPos < graphYCoord) {
                    barLengthOffset = graphYCoord - yStartPos;
                    yStartPos = graphYCoord;
                }

                gc.fillRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(ifrArray[x] * pixelPerInc)
                                - barLengthOffset);

                gc.drawRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(ifrArray[x] * pixelPerInc)
                                - barLengthOffset);

                if (yStartPos == graphYCoord) {
                    yStartPos = graphYCoord + graphHeight;
                    done = true;
                    ;
                }
            }

            // ------
            // MVFR
            // ------

            // Draw the MVFR label
            gc.drawString(String.format("%02d", Math.round(mvfrArray[x])),
                    graphXCoord + cellWidth * x + cellCharXOffset, graphYCoord
                            + graphHeight + cellHeight + cellCharYOffset, true);

            // Draw the MVFR bar
            if (mvfrArray[x] != 0 && done == false) {
                gc.setBackground(mvfrColor);
                yStartPos = yStartPos
                        - (int) Math.round(mvfrArray[x] * pixelPerInc);

                if (yStartPos < graphYCoord) {
                    barLengthOffset = graphYCoord - yStartPos;
                    yStartPos = graphYCoord;
                }

                gc.fillRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(mvfrArray[x] * pixelPerInc)
                                - barLengthOffset);

                gc.drawRectangle(cellWidth * x + graphXCoord + barOffset,
                        yStartPos, barWidth,
                        (int) Math.round(mvfrArray[x] * pixelPerInc)
                                - barLengthOffset);

                if (yStartPos == graphYCoord) {
                    yStartPos = graphYCoord + graphHeight;
                    done = true;
                    ;
                }
            }

            // Reset the Y coordinate position
            yStartPos = graphYCoord + graphHeight;
        }

        // -----------------------------------
        // Draw the annual data
        // -----------------------------------
        done = false;
        barLengthOffset = 0;
        int arrayLength = (mvfrArray.length - 1);

        float mvfrAnnual = mvfrArray[12];
        float ifrAnnual = ifrArray[12];
        float lifrAnnual = lifrArray[12];
        float vlifrAnnual = vlifrArray[12];

        // ------
        // VLIFR
        // ------
        // Draw the VLIFR label
        gc.drawString(String.format("%02d", Math.round(vlifrAnnual)),
                graphXCoord + cellWidth * arrayLength + cellCharXOffset,
                graphYCoord + graphHeight + cellHeight * 4 + cellCharYOffset,
                true);

        // Draw the VLIFR bar
        if (vlifrAnnual != 0) {
            gc.setBackground(vlifrColor);
            yStartPos = yStartPos - (int) Math.round(vlifrAnnual * pixelPerInc);

            if (yStartPos < graphYCoord) {
                barLengthOffset = graphYCoord - yStartPos;
                yStartPos = graphYCoord;
            }

            gc.fillRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(vlifrAnnual * pixelPerInc)
                            - barLengthOffset);

            gc.drawRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(vlifrAnnual * pixelPerInc)
                            - barLengthOffset);

            if (yStartPos == graphYCoord) {
                yStartPos = graphYCoord + graphHeight;
                done = true;
            }
        }

        // ------
        // LIFR
        // ------

        // Draw the LIFR label
        gc.drawString(String.format("%02d", Math.round(lifrAnnual)),
                graphXCoord + cellWidth * arrayLength + cellCharXOffset,
                graphYCoord + graphHeight + cellHeight * 3 + cellCharYOffset,
                true);

        // Draw the LIFR bar
        if (lifrAnnual != 0 && done == false) {
            gc.setBackground(lifrColor);
            yStartPos = yStartPos - (int) Math.round(lifrAnnual * pixelPerInc);

            if (yStartPos < graphYCoord) {
                barLengthOffset = graphYCoord - yStartPos;
                yStartPos = graphYCoord;
            }

            gc.fillRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(lifrAnnual * pixelPerInc)
                            - barLengthOffset);

            gc.drawRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(lifrAnnual * pixelPerInc)
                            - barLengthOffset);

            if (yStartPos == graphYCoord) {
                yStartPos = graphYCoord + graphHeight;
                done = true;
                ;
            }
        }

        // ------
        // IFR
        // ------

        // Draw the IFR label
        gc.drawString(String.format("%02d", Math.round(ifrAnnual)), graphXCoord
                + cellWidth * arrayLength + cellCharXOffset, graphYCoord
                + graphHeight + cellHeight * 2 + cellCharYOffset, true);

        // Draw the IFR bar
        if (ifrAnnual != 0 && done == false) {
            gc.setBackground(ifrColor);
            yStartPos = yStartPos - (int) Math.round(ifrAnnual * pixelPerInc);

            if (yStartPos < graphYCoord) {
                barLengthOffset = graphYCoord - yStartPos;
                yStartPos = graphYCoord;
            }

            gc.fillRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(ifrAnnual * pixelPerInc) - barLengthOffset);

            gc.drawRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(ifrAnnual * pixelPerInc) - barLengthOffset);

            if (yStartPos == graphYCoord) {
                yStartPos = graphYCoord + graphHeight;
                done = true;
                ;
            }
        }

        // ------
        // MVFR
        // ------

        // Draw the MVFR label
        gc.drawString(String.format("%02d", Math.round(mvfrAnnual)),
                graphXCoord + cellWidth * arrayLength + cellCharXOffset,
                graphYCoord + graphHeight + cellHeight + cellCharYOffset, true);

        // Draw the MVFR bar
        if (mvfrAnnual != 0 && done == false) {
            gc.setBackground(mvfrColor);
            yStartPos = yStartPos - (int) Math.round(mvfrAnnual * pixelPerInc);

            if (yStartPos < graphYCoord) {
                barLengthOffset = graphYCoord - yStartPos;
                yStartPos = graphYCoord;
            }

            gc.fillRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(mvfrAnnual * pixelPerInc)
                            - barLengthOffset);

            gc.drawRectangle(cellWidth * arrayLength + graphXCoord + barOffset,
                    yStartPos, barWidth,
                    (int) Math.round(mvfrAnnual * pixelPerInc)
                            - barLengthOffset);

            if (yStartPos == graphYCoord) {
                yStartPos = graphYCoord + graphHeight;
                done = true;
                ;
            }
        }

        // Reset the Y coordinate position
        yStartPos = graphYCoord + graphHeight;
    }

    @Override
    public float getMaxPercentInData() {
        return maxPercentInData;
    }
}
