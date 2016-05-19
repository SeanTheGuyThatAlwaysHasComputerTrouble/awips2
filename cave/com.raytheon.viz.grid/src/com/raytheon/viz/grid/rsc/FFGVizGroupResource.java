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
package com.raytheon.viz.grid.rsc;

import java.util.ArrayList;
import java.util.Map;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IRefreshListener;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.capabilities.AbstractCapability;

/**
 * FFG Group Resource class.
 * 
 * Based off VizGroupResource.java
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 19, 2012  1162      mpduff      Initial creation.
 * Jun 21, 2013 DR15394	mgamazaychikov Implement IResourceDataChanged and 
 * 									   override resourceChanged method.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class FFGVizGroupResource extends
		AbstractVizResource<FfgVizGroupResourceData, MapDescriptor> implements
		IResourceDataChanged, IRefreshListener {

    private final String NO_DATA = "No Data";

    /**
     * Constructor.
     * 
     * @param resourceData
     * @param loadProperties
     */
    protected FFGVizGroupResource(FfgVizGroupResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        dataTimes = new ArrayList<DataTime>();
        this.resourceData.addChangeListener(this);        
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.rsc.AbstractVizResource#disposeInternal()
     */
    @Override
    protected void disposeInternal() {
        for (ResourcePair rp : this.resourceData.getResourceList()) {
            if (rp.getResource() != null) {
                rp.getResource().dispose();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#paintInternal(com.raytheon
     * .uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        for (ResourcePair rp : this.resourceData.getResourceList()) {
            if (rp.getResource() != null) {
                rp.getResource().paint(target, paintProps);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#inspect(com.raytheon
     * .uf.common.geospatial.ReferencedCoordinate)
     */
    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        ResourceList rl = resourceData.getResourceList();
        String value = "No Data";
        Map<AbstractVizResource<?, ?>, DataTime[]> timeMap = descriptor
                .getTimeMatchingMap();
        for (ResourcePair pair : rl) {
            if (pair.getResource() != null) {
                AbstractVizResource<?, ?> rsc = pair.getResource();
                timeMap.put(rsc, timeMap.get(this));
                value = rsc.inspect(coord);
                if (!NO_DATA.equalsIgnoreCase(value)) {
                    return value;
                }
            }
        }

        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#interrogate(com.raytheon
     * .uf.common.geospatial.ReferencedCoordinate)
     */
    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {
        // TODO Auto-generated method stub
        return super.interrogate(coord);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#initInternal(com.raytheon
     * .uf.viz.core.IGraphicsTarget)
     */
    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        for (AbstractVizResource<?, ?> resource : resourceData.getRscs()) {
            if (resource != null) {
                resource.init(target);
                resource.registerListener(this);
            }
        }

        // If child resources have capabilities that this does not, steal them
        for (AbstractVizResource<?, ?> rcs : getResourceData().getRscs()) {
            for (AbstractCapability capability : rcs.getCapabilities()
                    .getCapabilityClassCollection()) {
                this.getCapabilities().addCapability(capability);
                capability.setResourceData(resourceData);
            }
        }

        // Spread my master capability set to all my children
        for (AbstractCapability capability : getCapabilities()
                .getCapabilityClassCollection()) {
            for (AbstractVizResource<?, ?> rcs : getResourceData().getRscs()) {
                rcs.getCapabilities().addCapability(capability);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.rsc.IRefreshListener#refresh()
     */
    @Override
    public void refresh() {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.IResourceDataChanged#resourceChanged(com
     * .raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType,
     * java.lang.Object)
     */
	@Override
	public void resourceChanged(ChangeType type, Object object) {
        if ( object instanceof Object[]){
        	this.resourceData.getRscs().get(0).getResourceData().update(object);
        }
        else if (object instanceof Object){
        	ArrayList<Object> theObjectList = new ArrayList<Object>(); 
        	theObjectList.add(object); 
        	this.resourceData.getRscs().get(0).getResourceData().update(theObjectList.toArray());
        }
    }
}
