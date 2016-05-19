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
package com.raytheon.uf.viz.d2d.core.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.drawables.AbstractDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.maps.scales.MapScaleRenderableDisplay;
import com.raytheon.uf.viz.core.maps.scales.MapScalesManager;
import com.raytheon.uf.viz.core.maps.scales.MapScalesManager.ManagedMapScale;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceList.AddListener;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.DensityCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.MagnificationCapability;
import com.raytheon.uf.viz.d2d.core.D2DProperties;
import com.raytheon.uf.viz.d2d.core.ID2DRenderableDisplay;
import com.raytheon.uf.viz.d2d.core.time.D2DTimeMatcher;
import com.raytheon.viz.core.imagery.ImageCombiner;

/**
 * Implementation of a D2D-specific map renderable display
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 9, 2009             njensen     Initial creation
 * Mar 21, 2013       1638 mschenke    Made map scales not tied to d2d
 * Mar 22, 2013       1638 mschenke    Moved map scale code to MapScaleRenderableDisplay
 * Apr 06, 2015 ASM #17215 D. Friedman Implement clear to avoid removing time match basis
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class D2DMapRenderableDisplay extends MapScaleRenderableDisplay
        implements ID2DRenderableDisplay {

    /** The magnification */
    @XmlAttribute
    protected double magnification = ((Double) VizGlobalsManager
            .getCurrentInstance().getPropery(VizConstants.MAGNIFICATION_ID))
            .doubleValue();

    /** The density */
    @XmlAttribute
    protected double density = ((Double) VizGlobalsManager.getCurrentInstance()
            .getPropery(VizConstants.DENSITY_ID)).doubleValue();

    protected DataScaleListener scaleListener = null;

    protected ImageCombiner combinerListener = null;

    protected boolean scaleOnNextPaint = false;

    public D2DMapRenderableDisplay() {
        super();
    }

    public D2DMapRenderableDisplay(MapDescriptor desc) {
        super(desc);
        desc.setTimeMatcher(new D2DTimeMatcher());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.d2d.core.ID2DRenderableDisplay#getMagnification()
     */
    @Override
    public double getMagnification() {
        return magnification;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.d2d.core.ID2DRenderableDisplay#getDensity()
     */
    @Override
    public double getDensity() {
        return density;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.d2d.core.ID2DRenderableDisplay#setMagnification(double
     * )
     */
    @Override
    public void setMagnification(double magnification) {
        this.magnification = magnification;
        List<ResourcePair> rps = new ArrayList<ResourcePair>(
                descriptor.getResourceList());
        for (int i = 0; i < rps.size(); i++) {
            AbstractVizResource<?, ?> resource = rps.get(i).getResource();
            if (resource != null) {
                if (resource instanceof IResourceGroup) {
                    rps.addAll(((IResourceGroup) resource).getResourceList());
                }
                if (resource.hasCapability(MagnificationCapability.class)) {
                    resource.getCapability(MagnificationCapability.class)
                            .setMagnification(magnification);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.d2d.core.ID2DRenderableDisplay#setDensity(double)
     */
    @Override
    public void setDensity(double density) {
        this.density = density;
        List<ResourcePair> rps = new ArrayList<ResourcePair>(
                descriptor.getResourceList());
        for (int i = 0; i < rps.size(); i++) {
            AbstractVizResource<?, ?> resource = rps.get(i).getResource();
            if (resource != null) {
                if (resource instanceof IResourceGroup) {
                    rps.addAll(((IResourceGroup) resource).getResourceList());
                }
                if (resource.hasCapability(DensityCapability.class)) {
                    resource.getCapability(DensityCapability.class).setDensity(
                            density);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.d2d.core.ID2DRenderableDisplay#getScale()
     */
    @Override
    public String getScale() {
        return getScaleName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.d2d.core.ID2DRenderableDisplay#setScale(java.lang
     * .String)
     */
    @Override
    public void setScale(String scale) {
        setScaleName(scale);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.map.MapRenderableDisplay#paint(com.raytheon.
     * uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        D2DProperties props = new D2DProperties();
        props.setScale(getScale());
        PaintProperties myProps = new PaintProperties(paintProps);
        myProps.setPerspectiveProps(props);

        if (scaleOnNextPaint) {
            scaleToClientArea(paintProps.getCanvasBounds());
            scaleOnNextPaint = false;
        }

        super.paint(target, myProps);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.drawables.AbstractRenderableDisplay#setDescriptor
     * (com.raytheon.uf.viz.core.drawables.IDescriptor)
     */
    @Override
    public void setDescriptor(IDescriptor desc) {
        super.setDescriptor(desc);
        if (!(descriptor.getTimeMatcher() instanceof D2DTimeMatcher)) {
            descriptor.setTimeMatcher(new D2DTimeMatcher());
        }
    }

    @Override
    public IMapDescriptor getDescriptor() {
        return (IMapDescriptor) super.getDescriptor();
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.raytheon.viz.core.drawables.AbstractRenderableDisplay#
     * setAbstractDescriptor(com.raytheon.viz.core.drawables.AbstractDescriptor)
     */
    @Override
    protected void setAbstractDescriptor(AbstractDescriptor ad) {
        super.setAbstractDescriptor(ad);
        if (descriptor.getTimeMatcher() == null) {
            descriptor.setTimeMatcher(new D2DTimeMatcher());
        }
    }

    @Override
    public Map<String, Object> getGlobalsMap() {
        Map<String, Object> globals = super.getGlobalsMap();
        globals.put(VizConstants.FRAMES_ID, new Integer(getDescriptor()
                .getNumberOfFrames()));
        globals.put(VizConstants.DENSITY_ID, new Double(density));
        globals.put(VizConstants.MAGNIFICATION_ID, new Double(magnification));
        globals.put(VizConstants.LOADMODE_ID, ((D2DTimeMatcher) getDescriptor()
                .getTimeMatcher()).getLoadMode());
        return globals;
    }

    public void setScaleOnNextPaint(boolean scale) {
        this.scaleOnNextPaint = scale;
    }

    public long getBlinkInterval() {
        return super.getBlinkInterval();
    }

    public void setBlinkInterval(long blinkInterval) {
        super.setBlinkInterval(blinkInterval);
    }

    @Override
    protected void customizeResourceList(ResourceList resourceList) {
        // Add the d2d colorbar resource
        resourceList.add(ResourcePair
                .constructSystemResourcePair(colorBarRscData));
        // Add d2d legend resource
        resourceList.add(ResourcePair
                .constructSystemResourcePair(legendRscData));
        // Add the d2d select pane resource
        resourceList.add(ResourcePair
                .constructSystemResourcePair(selectedRscData));
        // Add the d2d sample resource
        resourceList.add(ResourcePair
                .constructSystemResourcePair(samplingRscData));

        // Add the image combiner
        resourceList.addPostAddListener(getImageCombinerListener());

        // Add scale listeners
        resourceList.addPostAddListener(getScaleListener());
        resourceList.addPostRemoveListener(getScaleListener());
    }

    /**
     * Get the data scale listener, instantiates if null
     * 
     * @return
     */
    private DataScaleListener getScaleListener() {
        if (scaleListener == null) {
            scaleListener = new DataScaleListener(this);
        }
        return scaleListener;
    }

    /**
     * Get the image combiner listener, instantiates if null
     * 
     * @return
     */
    private AddListener getImageCombinerListener() {
        if (combinerListener == null) {
            combinerListener = new ImageCombiner(getDescriptor());
        }
        return combinerListener;
    }

    /** Like MapScaleRenderableDisplayer.clear, but avoids removing the time match
     * basis until other resources are removed.  This reduces time matching churn
     * and reduces the chances of lockups.
     * @see com.raytheon.uf.viz.core.maps.scales.MapScaleRenderableDisplay#clear()
     */
    @Override
    public void clear() {
        AbstractVizResource<?, ?> timeMatchBasis = null;
        AbstractTimeMatcher timeMatcher = descriptor.getTimeMatcher();
        if (timeMatcher instanceof D2DTimeMatcher) {
            timeMatchBasis = ((D2DTimeMatcher) timeMatcher).getTimeMatchBasis();
        }
        ManagedMapScale scale = MapScalesManager.getInstance().getScaleByName(
                getScaleName());
        if (scale != null) {
            ResourceList list = descriptor.getResourceList();
            for (ResourcePair rp : list) {
                if (rp.getProperties().isSystemResource() == false) {
                    // Keep system resources
                    if (rp.getResource() != timeMatchBasis) {
                        list.remove(rp);
                    }
                }
            }
            if (timeMatchBasis != null) {
                list.removeRsc(timeMatchBasis);
            }
            loadScale(scale);
        } else {
            // Map scale could not be found, default to remove all
            // non-map/system layers and reset display
            ResourceList list = descriptor.getResourceList();
            for (ResourcePair rp : list) {
                ResourceProperties props = rp.getProperties();
                if (props.isMapLayer() == false
                        && props.isSystemResource() == false) {
                    if (rp.getResource() != timeMatchBasis) {
                        list.remove(rp);
                    }
                } else {
                    try {
                        props.setVisible(true);
                        rp.getResource().recycle();
                    } catch (Throwable e) {
                        props.setVisible(false);
                        statusHandler.handle(Priority.PROBLEM, "Clear error: "
                                + e.getMessage() + ":: The resource ["
                                + rp.getResource().getSafeName()
                                + "] has been disabled.", e);
                    }
                }
            }
            if (timeMatchBasis != null) {
                list.removeRsc(timeMatchBasis);
            }

            scaleToClientArea(getBounds());
        }
    }

}
