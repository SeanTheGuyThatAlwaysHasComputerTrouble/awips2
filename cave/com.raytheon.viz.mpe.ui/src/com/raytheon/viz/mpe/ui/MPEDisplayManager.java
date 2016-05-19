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
package com.raytheon.viz.mpe.ui;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RadioState;
import org.eclipse.ui.handlers.RegistryToggleState;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;
import com.raytheon.uf.common.dataplugin.shef.tables.Colorvalue;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.mpe.util.XmrgFile;
import com.raytheon.uf.common.ohd.AppsDefaults;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceList.RemoveListener;
import com.raytheon.viz.hydrocommon.util.MPEColors;
import com.raytheon.viz.hydrocommon.whfslib.colorthreshold.GetColorValues;
import com.raytheon.viz.hydrocommon.whfslib.colorthreshold.NamedColorUseSet;
import com.raytheon.viz.mpe.MPECommandConstants;
import com.raytheon.viz.mpe.MPEDateFormatter;
import com.raytheon.viz.mpe.core.MPEDataManager;
import com.raytheon.viz.mpe.core.MPEDataManager.MPERadarLoc;
import com.raytheon.viz.mpe.ui.dialogs.hourlyradar.RadarDataManager;
import com.raytheon.viz.mpe.ui.displays.MPEMapRenderableDisplay;
import com.raytheon.viz.mpe.ui.rsc.MPEFieldResource;
import com.raytheon.viz.mpe.ui.rsc.MPEFieldResourceData;
import com.raytheon.viz.mpe.ui.rsc.MPEFieldResourceData.ArealDisplay;
import com.raytheon.viz.mpe.ui.rsc.MPEGageResource;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;

/**
 * 
 * Display manager class for MPE renderable displays
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 18, 2012            mschenke     Initial creation
 * Mar 14, 2013   1457     mpduff       Reset the gages on the resource.
 * Apr 18, 2013   1920     mpduff       Added updateGages method to reload the gage data, 
 *                                      fix formatting of legend for Base field Height.
 * Jul 02, 2013   2160     mpduff       Initialize newly displayed resources.
 * Feb 02, 2014  16201     snaples      Added saved data flag support
 * Feb 04, 2014   16410    lbousaidi    changed the first letter of the month to lower case.
 * Feb 19, 2014   2628     mpduff       Change cast from short to int when creating color bar.
 * Jun 30, 2014  17457     snaples      Added default case to switch in getXmrgfile.
 * Jul 8, 2015   16790     snaples      Updated setCurrentEditDate to refresh resources when dateMap is stale.
 * Jul 29, 2015  17471     snaples      Updated editTime to ensure that it always references "GMT" timezone.
 * Sep 29, 2015  16790     snaples      Fixed issue with date not following the CAVE time when changed, and fixed time matching issue.
 * Dec 02, 2015  18104     snaples      Fixed issue of not unzooming when using Pan/Zoom tools.
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */
public class MPEDisplayManager {

    /** pcpn time step */
    public static int pcpn_time_step = -1;

    /** QPF Flag **/
    private boolean qpf = false;

    /** Freezing Flag **/
    private boolean zflag = false;

    /** MaxMin Flag **/
    private boolean maxmin = false;

    /** Group Edit Flag **/
    private boolean groupedt = false;

    private int dqcDays = Integer.parseInt(AppsDefaults.getInstance().getToken(
            "mpe_dqc_num_days"));

    /** Radar Type for Radar Coverage Map Legend **/
    public static enum AvailableRadarGridType
    {
        DUAL_POL, SINGLE_POL, SINGLE_AND_DUAL_POL, MISSING
    };
    
    /**
     * @return the qpf
     */
    public boolean isQpf() {
        return qpf;
    }

    /**
     * @param qpf
     *            the qpf to set
     */
    public void setQpf(boolean qpf) {
        this.qpf = qpf;
    }

    /**
     * @return the groupedt
     */
    public boolean isGroupedt() {
        return groupedt;
    }

    /**
     * @param groupedt
     *            the groupedt to set
     */
    public void setGroupedt(boolean groupedt) {
        this.groupedt = groupedt;
    }

    /**
     * @return the zflag
     */
    public boolean isZflag() {
        return zflag;
    }

    /**
     * @param zflag
     *            the zflag to set
     */
    public void setZflag(boolean zflag) {
        this.zflag = zflag;
    }

    /**
     * @return the maxmin
     */
    public boolean isMaxmin() {
        return maxmin;
    }

    /**
     * @param maxmin
     *            the maxmin to set
     */
    public void setMaxmin(boolean maxmin) {
        this.maxmin = maxmin;
    }

    /**
     * @return the dqcDays
     */
    public int getDqcDays() {
        return dqcDays;
    }

    /**
     * @param dqcDays
     *            the dqcDays to set
     */
    public void setDqcDays(int qcDays) {
        dqcDays = qcDays;
        MPEDataManager.getInstance().setDQCDays(dqcDays);
    }

    private AbstractVizResource<?, ?> displayedResource;

    private boolean savedData = true;

    public void setDisplayedResource(AbstractVizResource<?, ?> rsc) {
        displayedResource = rsc;
        if (displayedFieldResource != null
                && displayedFieldResource != displayedResource) {
            display.getDescriptor().getResourceList()
                    .removeRsc(displayedFieldResource);
        } else if (displayedResource instanceof MPEFieldResource) {
            displayedFieldResource = (MPEFieldResource) displayedResource;
        }
    }

    /**
     * @return the dataSaved
     */
    public boolean isDataSaved() {
        return savedData;
    }

    public void setSavedData(boolean saved) {
        savedData = saved;
    }

    /**
     * TODO: Delete
     * 
     * @return the pColorSetGroup
     */
    public List<NamedColorUseSet> getColorSetGroup() {
        return pColorSetGroup;
    }

    public static enum DisplayMode {
        Image, Contour
    };

    public static enum GageDisplay {
        Ids, Values, Triangles
    };

    public static enum GageColor {
        Solid, Contrast, ByQC, ByValue
    }

    public static enum GageMissingOptions {
        MissingNone, MissingReported, MissingAll
    }

    private static final Map<IRenderableDisplay, MPEDisplayManager> instanceMap = new HashMap<IRenderableDisplay, MPEDisplayManager>();

    /* Define the MPE product generation rules. */
    public static final DisplayFieldData[] mpe_qpe_fields = {
            DisplayFieldData.rMosaic, DisplayFieldData.avgrMosaic,
            DisplayFieldData.maxrMosaic, DisplayFieldData.bMosaic,
            DisplayFieldData.lMosaic, DisplayFieldData.gageOnly,
            DisplayFieldData.mMosaic, DisplayFieldData.mlMosaic,
            DisplayFieldData.satPre, DisplayFieldData.lsatPre,
            DisplayFieldData.srMosaic, DisplayFieldData.sgMosaic,
            DisplayFieldData.srgMosaic, DisplayFieldData.p3lMosaic,
            DisplayFieldData.Xmrg, DisplayFieldData.rfcMosaic,
            DisplayFieldData.rfcbMosaic, DisplayFieldData.rfcmMosaic,
            DisplayFieldData.qmosaic, DisplayFieldData.lqmosaic,
            DisplayFieldData.mlqmosaic,
            
            // Dual Pol fields
            DisplayFieldData.rdMosaic, DisplayFieldData.avgrdMosaic,
            DisplayFieldData.maxrdMosaic, DisplayFieldData.bdMosaic,
            DisplayFieldData.ldMosaic,
            DisplayFieldData.mdMosaic, DisplayFieldData.mldMosaic,
            DisplayFieldData.srdMosaic, DisplayFieldData.srdgMosaic,
            
            DisplayFieldData.localField1,
            DisplayFieldData.localField2, DisplayFieldData.localField3 };

    public static final String APPLICATION_NAME = "hmapmpe";

    private static final List<NamedColorUseSet> pColorSetGroup = MPEColors
            .build_mpe_colors();

    private static final ICommandService service = (ICommandService) PlatformUI
            .getWorkbench().getService(ICommandService.class);

    private static String fontId;

    private static GageColor gageColor;

    private static GageMissingOptions gageMissing;
    
    private static IExtent defaultExtent;

    static {
        gageMissing = getCommandStateEnum(
                MPECommandConstants.GAGE_MISSING_OPTION,
                GageMissingOptions.MissingNone);
        gageColor = getCommandStateEnum(MPECommandConstants.GAGE_COLOR,
                GageColor.Solid);
        fontId = getCommandState(MPECommandConstants.FONT);
    }

    /**
     * Get the MPEDisplayManager instance given the pane
     * 
     * @param pane
     * @return
     */
    public static synchronized MPEDisplayManager getInstance(IDisplayPane pane) {
        if (pane == null) {
            IDisplayPaneContainer container = EditorUtil
                    .getActiveVizContainer();
            if (container != null) {
                pane = container.getActiveDisplayPane();
            }
        }

        if (pane == null) {
            return null;
        }

        return getInstance(pane.getRenderableDisplay());
    }

    /**
     * Get the MPEDisplayManager instance given the display
     * 
     * @param pane
     *            the display to get the instance for
     * @return the instance
     */
    public static synchronized MPEDisplayManager getInstance(
            IRenderableDisplay display) {
        if (display == null) {
            IDisplayPaneContainer container = EditorUtil
                    .getActiveVizContainer();
            if (container != null) {
                IDisplayPane pane = container.getActiveDisplayPane();
                if (pane != null) {
                    display = pane.getRenderableDisplay();
                }
            }
        }

        if (display == null) {
            return null;
        }

        MPEDisplayManager instance = instanceMap.get(display);

        if (instance == null) {
            instance = new MPEDisplayManager(display);
            instanceMap.put(display, instance);
        }
        return instance;
    }

    /**
     * Disposes the display by removing it from the instance map
     * 
     * @param display
     */
    public static void disposeDisplay(MPEMapRenderableDisplay display) {
        instanceMap.remove(display);
    }

    /**
     * Get the current display manager on the active editor
     * 
     * @return
     */
    public static MPEDisplayManager getCurrent() {
        MPEDisplayManager instance = null;
        IEditorPart editor = EditorUtil.getActiveEditor();
        if ((editor != null) && (editor instanceof IDisplayPaneContainer)) {
            IDisplayPane pane = null;
            if (editor instanceof IMultiPaneEditor) {
                IMultiPaneEditor multiPane = (IMultiPaneEditor) editor;
                if (multiPane.displayedPaneCount() > 1) {
                    pane = multiPane
                            .getSelectedPane(IMultiPaneEditor.LOAD_ACTION);
                }
            }

            if (pane == null) {
                pane = ((IDisplayPaneContainer) editor).getActiveDisplayPane();
            }
            instance = lookupManager(pane);
        }
        return instance;
    }

    /**
     * Look up the display manager for the given pane
     * 
     * @param pane
     * @return
     */
    private static MPEDisplayManager lookupManager(IDisplayPane pane) {
        if (pane != null) {
            return instanceMap.get(pane.getRenderableDisplay());
        }
        return null;
    }

    private final Set<IEditTimeChangedListener> timeChangedListeners = new LinkedHashSet<IEditTimeChangedListener>();

    private final Set<IDisplayFieldChangedListener> fieldChangedListeners = new LinkedHashSet<IDisplayFieldChangedListener>();

    private final IRenderableDisplay display;

    private DisplayFieldData displayedField;

    private int displayedAccumHrs;

    private ArealDisplay displayedArealDisplay;

    private final MPEFieldResourceData fieldResourceData = new MPEFieldResourceData();

    private MPEFieldResource displayedFieldResource;
    
    private ComparisonFields comparisonFields = new ComparisonFields();

    private Date editTime;

    /**
     * Constructs an {@link MPEDisplayManager} for the specified
     * {@link IRenderableDisplay}
     * 
     * @param display
     */
    private MPEDisplayManager(IRenderableDisplay display) {
        this.display = display;
        try {
            display.getDescriptor().redoTimeMatching();
        } catch (VizException e) {
            throw new RuntimeException("Error time matching MPE", e);
        }
        editTime = getCurrentDisplayedDate();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTime(editTime);
        editTime = cal.getTime();

        // token for default display
        String mdd = AppsDefaults.getInstance().getToken("mpe_def_display");

        if(mdd != null){
            displayedField = DisplayFieldData.fromString(mdd);
        } else {
            displayedField = DisplayFieldData.fromString("MMOSAIC");
        }
        
        ChangeTimeProvider.update(this);

        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                MPEDisplayManager.this.toggleDisplayMode(DisplayMode.Image);
                if (defaultExtent == null) {
                    IDisplayPaneContainer container = EditorUtil.getActiveVizContainer();
                    if (container != null) {
                        IDisplayPane pane = container.getActiveDisplayPane();
                        if (pane != null) {
                            setDefaultExtent(pane.getRenderableDisplay().getExtent());
                        }
                    }
                }
            }
        });

        // Add listener so we can null out displayed resource when removed
        display.getDescriptor().getResourceList()
                .addPostRemoveListener(new RemoveListener() {
                    @Override
                    public void notifyRemove(ResourcePair rp)
                            throws VizException {
                        if (rp.getResource() == displayedFieldResource) {
                            displayedFieldResource = null;
                        }
                    }
                });
    }

    /**
     * Synchronizes this manager with the otherMgr passed in.
     * 
     * @param otherMgr
     */
    public void synchronize(MPEDisplayManager otherMgr) {
        // Force edit date to change
        setCurrentEditDate(otherMgr.getCurrentEditDate(), true);
        // TODO: Synchronize time matchers on display?
    }

    public void registerEditTimeChangedListener(
            IEditTimeChangedListener listener) {
        synchronized (timeChangedListeners) {
            timeChangedListeners.add(listener);
        }
    }

    public void unregisterEditTimeChangedListener(
            IEditTimeChangedListener listener) {
        synchronized (timeChangedListeners) {
            timeChangedListeners.remove(listener);
        }
    }

    public void registerDisplayFieldChangedListener(
            IDisplayFieldChangedListener listener) {
        synchronized (fieldChangedListeners) {
            fieldChangedListeners.add(listener);
        }
    }

    public void unregisterDisplayFieldChangedListener(
            IDisplayFieldChangedListener listener) {
        synchronized (fieldChangedListeners) {
            fieldChangedListeners.remove(listener);
        }
    }

    /**
     * Returns the currently displayed resource, if null, no data is currently
     * displayed
     * 
     * @return
     */
    public MPEFieldResource getDisplayedFieldResource() {
        return displayedFieldResource;
    }

    /**
     * @return the displayedResource
     */
    public AbstractVizResource<?, ?> getDisplayedResource() {
        // TODO: Manage resources internally, get rid of setDisplayedResource
        return displayedFieldResource != null ? displayedFieldResource
                : displayedResource;
    }

    /**
     * Gets the currently active {@link DisplayFieldData}. This field has no
     * indication if data is actually being actively displayed
     * 
     * @return
     */
    public DisplayFieldData getDisplayFieldType() {
        return displayedField;
    }

    public IRenderableDisplay getRenderableDisplay() {
        return display;
    }

    /**
     * @return the displayMode
     */
    public Set<DisplayMode> getDisplayMode() {
        return new HashSet<MPEDisplayManager.DisplayMode>(
                fieldResourceData.getDisplayModes());
    }

    /**
     * @param displayMode
     *            the displayMode to set
     */
    public void toggleDisplayMode(DisplayMode displayMode) {
        fieldResourceData.toggleDisplayMode(displayMode);
        display.refresh();

        final ICommandService service = (ICommandService) PlatformUI
                .getWorkbench().getService(ICommandService.class);
        service.refreshElements(MPECommandConstants.DISPLAY_MODE, null);
    }

    /**
     * @param gageDisplay
     *            the gageDisplay to set
     * @param isOn
     *            is set to draw
     */
    public void toggleGageDisplay(GageDisplay gageDisplay, boolean isOn) {
        List<MPEGageResource> rscs = display.getDescriptor().getResourceList()
                .getResourcesByTypeAsType(MPEGageResource.class);
        for (MPEGageResource rsc : rscs) {
            rsc.toggleGageDisplay(gageDisplay, isOn);
        }
        display.refresh();
    }

    /**
     * Returns the currently displayed date. This may be different from the edit
     * date returned from {@link #getCurrentEditDate()} in cases of time lapsing
     * or looping. This is purely for convenience and in most cases the
     * resources will only care about current displayed date and can get it from
     * their {@link PaintProperties}
     * 
     * @return
     */
    public Date getCurrentDisplayedDate() {
        DataTime currTime = display.getDescriptor().getFramesInfo()
                .getCurrentFrame();
        Date date = null;
        if (currTime != null) {
            date = currTime.getRefTime();
        }
        return date;
    }

    /**
     * Changes the date to be displayed, this function will ONLY change
     * displayed date, all edits will still occur on
     * {@link #getCurrentEditDate()} time and therefore will not prompt for
     * saving of edits
     * 
     * @param dateToDisplay
     */
    public void setCurrentDisplayedDate(Date dateToDisplay) {
        display.getDescriptor().getFrameCoordinator()
                .changeFrame(dateToDisplay);
        display.refresh();
    }

    /**
     * Returns the active edit time. This may not be the same as what time is
     * currently being drawn on the display in case of looping or time lapsing
     * but it is the time any edits should be made for despite displayed date
     * 
     * @return
     */
    public Date getCurrentEditDate() {
        return editTime;
    }

    /**
     * Changes the active edit time (and currently displayed time). Will prompt
     * user if unsaved edits exist
     * 
     * @param newDate
     * @return true if time was changed false if not
     */
    public boolean setCurrentEditDate(Date newDate) {
        return setCurrentEditDate(newDate, false);
    }

    /**
     * Changes the active edit time (and currently displayed time). Will only
     * prompt user if unsaved edits exist if force==false
     * 
     * @param newDate
     * @param force
     * @return
     */
    public boolean setCurrentEditDate(Date newDate, boolean force) {

        if (editTime.equals(newDate) == false) {
            // new time, check for save
            if (!isDataSaved()) {
                if (!okToProceed("Data Not Saved")) {
                    return false;
                }
            }
        }
        Date oldDate = editTime;
        editTime = newDate;

        if (display.getContainer() != null) {
            stopLooping(display.getContainer());
        }
        try {
            display.getDescriptor().redoTimeMatching();
        } catch (VizException e) {
            Activator.statusHandler.handle(Priority.PROBLEM, 
                    "Error in redoTimeMatching for MPEDisplayManager", e);
        }
        setCurrentDisplayedDate(newDate);

        if (displayedFieldResource == null) {
            displayFieldData(getDisplayFieldType());
        }
        ChangeTimeProvider.update(this);

        try {
            List<IEditTimeChangedListener> listeners = new ArrayList<IEditTimeChangedListener>();
            synchronized (timeChangedListeners) {
                listeners.addAll(timeChangedListeners);
            }
            for (IEditTimeChangedListener listener : listeners) {
                listener.editTimeChanged(oldDate, newDate);
            }
        } catch (Throwable t) {
            Activator.statusHandler.handle(Priority.PROBLEM,
                    "Error firing edit time changed listeners", t);
        }

        return true;
    }

    /**
     * Changes the {@link DisplayFieldData} for the current edit time. No
     * accumulation is done (0), if accumulation desired, call
     * {@link #displayFieldData(DisplayFieldData, int)}
     * 
     * @param displayFieldType
     */
    public void displayFieldData(DisplayFieldData displayFieldType) {
        displayFieldData(displayFieldType, 0);
    }

    /**
     * Changes the {@link DisplayFieldData} with accumulation passed for the
     * current edit time. Displays data as {@link ArealDisplay#GRID} if a
     * different areal display is desired, call
     * {@link #displayFieldData(DisplayFieldData, int, ArealDisplay)}
     * 
     * @param fieldToDisplay
     * @param accumulationHrs
     */
    public void displayFieldData(DisplayFieldData fieldToDisplay,
            int accumulationHrs) {
        displayFieldData(fieldToDisplay, accumulationHrs, ArealDisplay.GRID);
    }

    /**
     * Changes product displayed for current edit time with accumulation passed
     * in and as areal type specified
     * 
     * @param fieldToDisplay
     * @param accumulationHrs
     * @param arealDisplay
     */
    public void displayFieldData(DisplayFieldData fieldToDisplay,
            int accumulationHrs, ArealDisplay arealDisplay) {
    	
        if (displayedField != fieldToDisplay || displayedFieldResource == null
                || accumulationHrs != displayedAccumHrs
                || arealDisplay != displayedArealDisplay) {
            DisplayFieldData oldField = displayedField;
            displayedField = fieldToDisplay;
            
            //check for comparisonFields
            if  ( displayedField.isAComparisonField() )
            {
            	displayedField.setComparisonFields(comparisonFields);
            }
            else
            {
            	displayedField.setComparisonFields(null);
            }
            
            displayedAccumHrs = accumulationHrs;
            displayedArealDisplay = arealDisplay;
            ResourceList list = display.getDescriptor().getResourceList();

            if (displayedFieldResource != null) {
                // Remove old resource
                list.removeRsc(displayedFieldResource);
            }

            fieldResourceData.setFieldData(fieldToDisplay);
            fieldResourceData.setArealDisplay(arealDisplay);
            fieldResourceData.setAccumulationInterval(accumulationHrs);
            // Add new resource
            displayedFieldResource = new MPEFieldResource(fieldResourceData,
                    new LoadProperties());
            list.add(displayedFieldResource);

            IDisplayPaneContainer container = display.getContainer();
            for (IDisplayPane pane : container.getDisplayPanes()) {
                if (pane.getRenderableDisplay() == display) {
                    try {
                        displayedFieldResource.init(pane.getTarget());
                        break;
                    } catch (VizException e) {
                        Activator.statusHandler.handle(Priority.PROBLEM,
                                e.getLocalizedMessage(), e);
                    }
                }
            }

            if (oldField != fieldToDisplay) {
                List<IDisplayFieldChangedListener> listeners = new ArrayList<IDisplayFieldChangedListener>();
                synchronized (fieldChangedListeners) {
                    listeners.addAll(fieldChangedListeners);
                }
                for (IDisplayFieldChangedListener listener : listeners) {
                    listener.displayFieldChanged(oldField, fieldToDisplay);
                }
            }
        }

        displayedFieldResource.issueRefresh();
    }

    // TODO: May make these on a per MPEMapRenderableDisplay basis or per window
    /**
     * @return the gageColor
     */
    public static GageColor getGageColor() {
        return gageColor;
    }

    /**
     * @return the gageMissing
     */
    public static GageMissingOptions getGageMissing() {
        return gageMissing;
    }

    /**
     * @return the fontId
     */
    public static String getFontId() {
        return fontId;
    }

    /**
     * 
     * @param miss
     */
    public static void setGageMissing(GageMissingOptions miss) {
        gageMissing = miss;
        updateCommandState(MPECommandConstants.GAGE_MISSING_OPTION, miss.name());
    }

    /**
     * 
     * @param gc
     */
    public static void setGageColor(GageColor gc) {
        gageColor = gc;
        updateCommandState(MPECommandConstants.GAGE_COLOR, gc.name());
    }

    /**
     * Set the fontId
     * 
     * @param fontId
     */
    public static void setFontId(String fontId) {
        MPEDisplayManager.fontId = fontId;
        updateCommandState(MPECommandConstants.FONT, fontId);
    }

    /**
     * Checks configuration if MPE QC option enabled
     * 
     * @return the mpeDqcOption
     */
    public static boolean isMpeQcOptionEnabled() {
        return AppsDefaults.getInstance().getBoolean("mpe_dqc_options", false);
    }

    public static String getPolygonEditDir() {
        return AppsDefaults.getInstance().getToken("rfcwide_drawpre_dir");
    }

    /**
     * Gets the current command radio state id or null if no command or state
     * associated with the command
     * 
     * @param commandId
     * @return
     */
    public static String getCommandState(String commandId) {
        Command command = service.getCommand(commandId);
        if (command != null) {
            State state = command.getState(RadioState.STATE_ID);
            if (state != null) {
                Object stateValue = state.getValue();
                return stateValue != null ? String.valueOf(stateValue) : null;
            }
        }
        return null;
    }

    /**
     * Gets the current command toggle state.
     * 
     * @param commandId
     *            The Command ID
     * @return the toggle state, or false if command not found
     */
    public static Boolean getToggleState(String commandId) {
        Command command = service.getCommand(commandId);
        if (command != null) {
            State state = command.getState(RegistryToggleState.STATE_ID);
            if (state != null) {
                return (Boolean) state.getValue();
            }
        }
        return Boolean.FALSE;
    }

    /**
     * Set the toggle state.
     * 
     * @param commandId
     *            The command Id to set the toggle state on
     * @param toggleState
     *            The toggle state value
     */
    public static void setToggleState(String commandId, boolean toggleState) {
        Command command = service.getCommand(commandId);
        if (command != null) {
            State state = command.getState(RegistryToggleState.STATE_ID);
            if (state != null) {
                state.setValue(toggleState);
            }
        }
    }

    /**
     * Returns the radio state id of the command specified by commandId as an
     * enum of the type of defaultValue with defaultValue being the default to
     * use if no state is present
     * 
     * @param commandId
     * @param defaultValue
     * @return
     */
    public static <T extends Enum<T>> T getCommandStateEnum(String commandId,
            T defaultValue) {
        Class<T> type = defaultValue.getDeclaringClass();
        String stateId = getCommandState(commandId);
        if (stateId != null) {
            return Enum.valueOf(type, stateId);
        }
        return defaultValue;
    }

    /**
     * Updates the command's radio state with the new state id
     * 
     * @param commandId
     * @param newStateId
     */
    public static void updateCommandState(String commandId, String newStateId) {
        Command command = service.getCommand(commandId);
        if (command != null) {
            try {
                HandlerUtil.updateRadioState(command, newStateId);
            } catch (ExecutionException e) {
                Activator.statusHandler.handle(Priority.PROBLEM,
                        e.getLocalizedMessage(), e);
            }

            for (MPEDisplayManager mgr : instanceMap.values()) {
                mgr.getRenderableDisplay().refresh();
            }
        }
    }

    /**
     * Gets an {@link XmrgFile} reference for a {@link DisplayFieldData} and
     * {@link Date}
     * 
     * @param fieldData
     * @param date
     * @return an XmrgFile, will not be null but may not point to existing file
     */
    public static XmrgFile getXmrgFile(DisplayFieldData fieldData, Date date) {
        AppsDefaults appsDefaults = AppsDefaults.getInstance();
        String dirname = appsDefaults.getToken(fieldData.getDirToken());
        String fileNamePrefix = fieldData.getFileNamePrefix();
        String prismType = null;
        String dateFormatString = MPEDateFormatter.yyyyMMddHH;
        switch (fieldData) {
        case rfcMosaic:
        	fileNamePrefix += "01";
            break;
        case Xmrg:
        	fileNamePrefix = fileNamePrefix.toLowerCase();
            dateFormatString = MPEDateFormatter.MMddyyyyHH;
            break;
        case Prism:
            prismType = "mean_precip";
            dateFormatString = MPEDateFormatter.MMM;
            break;
        case maxtempPrism:
            prismType = "max_temp";
            dateFormatString = MPEDateFormatter.MMM;
            break;
        case mintempPrism:
            prismType = "min_temp";
            dateFormatString = MPEDateFormatter.MMM;
            break;
        default:
            break;
        }

        String dateString = MPEDateFormatter.format(date, dateFormatString);

        String fname = null;
        if (prismType != null) {
            // Load prism type
            dateString= dateString.toLowerCase();
            String mpe_site_id = appsDefaults.getToken("mpe_site_id");
            fname = FileUtil.join(dirname, "prism_" + prismType + "_"
                    + mpe_site_id + "_" + dateString);
        } else {
            fname = FileUtil.join(dirname, fileNamePrefix + dateString + "z");
        }
        return new XmrgFile(fname);
    }

    /**
     * Constructs a new {@link ColorMapParameters} object for the given
     * parameters
     * 
     * @param cvUse
     * @param durationInHrs
     * @param dataUnit
     * @param displayUnit
     * @return
     */
    public static ColorMapParameters createColorMap(String cvUse,
            int durationInHrs, Unit<?> dataUnit, Unit<?> displayUnit) {
        if (durationInHrs == 0) {
            durationInHrs = 1;
        }
        
        ColorMapParameters params = new ColorMapParameters();
        params.setFormatString("0.00");
        params.setDisplayUnit(displayUnit);
        params.setDataUnit(dataUnit);

        UnitConverter displayToData = params.getDisplayToDataConverter();
        UnitConverter dataToDisplay = params.getDataToDisplayConverter();

        DataMappingPreferences dm = new DataMappingPreferences();

        Colorvalue[] colorSet = GetColorValues.get_colorvalues(
                LocalizationManager.getContextName(LocalizationLevel.USER),
                APPLICATION_NAME, cvUse, durationInHrs * 60 * 60, "E",
                pColorSetGroup).toArray(new Colorvalue[0]);

        DisplayFieldData displayField = DisplayFieldData.fromString(cvUse);

        if (displayField == DisplayFieldData.Height) {
            params.setFormatString("0");
        }
        DecimalFormat format = new DecimalFormat(params.getFormatString());

        int numColors = colorSet.length;
        float[] red = new float[numColors];
        float[] green = new float[numColors];
        float[] blue = new float[numColors];

        for (int i = 0; i < numColors; ++i) {
            Colorvalue cv = colorSet[i];
            RGB rgb = RGBColors.getRGBColor(cv.getColorname().getColorName());
            red[i] = rgb.red / 255f;
            green[i] = rgb.green / 255f;
            blue[i] = rgb.blue / 255f;

            double threshold = cv.getId().getThresholdValue();
            DataMappingEntry entry = new DataMappingEntry();
            if (threshold == -9999.0 || threshold == -8888.0) {
                entry.setDisplayValue(threshold);
                entry.setOperator("<");
                entry.setLabel("");
            } else {
                // Convert display to data, cast to int, convert back to display
                entry.setDisplayValue(dataToDisplay.convert((int) displayToData
                        .convert(threshold)));
                if (displayField != DisplayFieldData.Index) {
                    entry.setLabel(format.format(threshold));
                }
            }
            entry.setPixelValue((double) i);

            dm.addEntry(entry);
        }

        DataMappingEntry entry = new DataMappingEntry();
        entry.setDisplayValue(Double.MAX_VALUE);
        entry.setPixelValue((double) (numColors - 1));
        entry.setLabel("");
        dm.addEntry(entry);

        params.setColorMap(new ColorMap(cvUse, red, green, blue));
        params.setDataMapping(dm);
        params.setDataMin(0);
        params.setDataMax(numColors - 1);
        params.setColorMapMin(params.getDataMin());
        params.setColorMapMax(params.getDataMax());

        // Check for Index parameter and set labels to radar sites
        // Determine and store radar type (S/D/M) for display in legend
        // See getAvailableRadarGridType method in .../MPEGui/TEXT/drawMpeLegend.c
        if (displayField == DisplayFieldData.Index)
        {
            MPERadarLoc[] radars = MPEDataManager.getInstance().getRadars()
                    .toArray(new MPERadarLoc[0]);
            DataMappingEntry[] entries = dm.getEntries().toArray(
                    new DataMappingEntry[0]);
          
            int offset = 2;
            for (int i = offset; i < entries.length; ++i)
            {
                int radarIdx = i - offset;
                if (radarIdx < radars.length)
                {
                    entries[i].setLabel(radars[radarIdx].getId());
                }
                else
                {
                    entries[i].setLabel("");
                }
            }
        }

        return params;
    }

    /**
     * Starts looping on the container passed in. Times looped will be from
     * {@link #getCurrentEditDate()} and go back hour hours
     * 
     * @param container
     * @param hour
     */
    public static void startLooping(IDisplayPaneContainer container, int hour) {
        LoopProperties loopProps = container.getLoopProperties();
        int frameRate = Integer.parseInt(AppsDefaults.getInstance().getToken(
                "hydroview_mpe_timelapse", "1000"));
        loopProps.setFwdFrameTime(frameRate);
        for (IDisplayPane pane : container.getDisplayPanes()) {
            MPEDisplayManager mgr = MPEDisplayManager.getInstance(pane);
            Date editDate = mgr.getCurrentEditDate();
            IDescriptor descriptor = pane.getDescriptor();
            FramesInfo info = descriptor.getFramesInfo();
            DataTime[] times = info.getFrameTimes();
            if (times != null) {
                for (DataTime time : times) {
                    // mark all visible
                    time.setVisible(true);
                }
                int frameCount = times.length;
                Calendar cal = Calendar.getInstance();
                cal.setTime(editDate);
                cal.add(Calendar.HOUR, -(hour - 1));

                for (int i = 0; i < frameCount; ++i) {
                    Date refTime = times[i].getRefTime();
                    // mark all before cal not visible
                    if (cal.getTime().after(refTime)
                            || editDate.before(refTime)) {
                        times[i].setVisible(false);
                    }
                }
            }
            pane.refresh();
        }

        loopProps.setLooping(true);
    }

    /**
     * Stops looping on the container passed in
     * 
     * @param container
     */
    public static void stopLooping(IDisplayPaneContainer container) {
        container.getLoopProperties().setLooping(false);
        // Ensure all frames are visible and change displayed to edit
        for (IDisplayPane pane : container.getDisplayPanes()) {
            MPEDisplayManager mgr = MPEDisplayManager.getInstance(pane);
            IDescriptor descriptor = pane.getDescriptor();
            FramesInfo info = descriptor.getFramesInfo();
            DataTime[] times = info.getFrameTimes();
            if (times != null) {
                for (DataTime time : times) {
                    time.setVisible(true);
                }
            }
            mgr.setCurrentDisplayedDate(mgr.getCurrentEditDate());
        }
    }

    /**
     * Prompt user to continue
     * 
     * @return
     */
    public static boolean okToProceed(String message) {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        return MessageDialog.openConfirm(shell, message, message
                + " - OK to Proceed?");
    }

    /**
     * Update the Gage resource.
     */
    public void updateGages() {
        // reset gages
        List<MPEGageResource> rscs = display.getDescriptor().getResourceList()
                .getResourcesByTypeAsType(MPEGageResource.class);
        for (MPEGageResource rsc : rscs) {
            rsc.reloadGages();
        }
    }

	public ComparisonFields getComparisonFields() {
		return comparisonFields;
	}

	public void setComparisonFields(ComparisonFields comparisonFields) {
		this.comparisonFields = comparisonFields;
	}
	
	public  AvailableRadarGridType getAvailableRadarType(String radId) {
		// get radar type (SINGLE_POL/DUAL_POL/MISSING)
		// check for DAA radar in DAARadarResult table - if found then return "DUAL_POL"
		// else check for DPA radar in RWRadarResult table - if found then return "SINGLE_POL"
		// else return "MISSING" - this is the default value
		AvailableRadarGridType type = AvailableRadarGridType.MISSING;
	
		Date dateTime = getCurrentDisplayedDate();
		int available;
		
		boolean dualPolAvailable = false;
		boolean singlePolAvailable = false;
		
		RadarDataManager radarDataManager = RadarDataManager.getInstance();

		try 
		{
			available = radarDataManager.getAvailableRadarDP(radId, dateTime);

			if(available == 0 || available == 2)
			{
				dualPolAvailable = true;
			}

			available = radarDataManager.getAvailableRadarSP(radId, dateTime);
			if(available == 0 || available == 2)
			{
				singlePolAvailable = true;
			}
		}
		
		catch (VizException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (singlePolAvailable)
		{
			if (dualPolAvailable)
			{
				type = AvailableRadarGridType.SINGLE_AND_DUAL_POL;
			}
			else
			{
				type = AvailableRadarGridType.SINGLE_POL;
			}
		}
		
		else if (dualPolAvailable)
		{
			type = AvailableRadarGridType.DUAL_POL;
		}
		
		else
		{
			type = AvailableRadarGridType.MISSING;
		}
		
		
		return type;
	}

    public int getDisplayedAccumHrs() {
       
        return displayedAccumHrs;
    }
    
    public static IExtent getDefaultExtent() {
        return defaultExtent;
    }
    
    public static void setDefaultExtent(IExtent defaultExtent) {
        MPEDisplayManager.defaultExtent = defaultExtent;
    }
}
