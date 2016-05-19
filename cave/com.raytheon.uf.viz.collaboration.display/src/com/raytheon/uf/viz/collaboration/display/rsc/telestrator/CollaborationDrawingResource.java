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
package com.raytheon.uf.viz.collaboration.display.rsc.telestrator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.collaboration.comm.identity.CollaborationException;
import com.raytheon.uf.viz.collaboration.comm.identity.event.IVenueParticipantEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueParticipant;
import com.raytheon.uf.viz.collaboration.display.Activator;
import com.raytheon.uf.viz.collaboration.display.data.SessionContainer;
import com.raytheon.uf.viz.collaboration.display.data.SharedDisplaySessionMgr;
import com.raytheon.uf.viz.collaboration.display.data.UserColorInfo;
import com.raytheon.uf.viz.collaboration.display.rsc.telestrator.CollaborationDrawingEvent.CollaborationEventType;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.uf.viz.drawing.DrawingToolLayer;
import com.raytheon.uf.viz.drawing.DrawingToolLayer.DrawMode;
import com.raytheon.uf.viz.remote.graphics.DispatchGraphicsTarget;
import com.raytheon.viz.ui.input.EditableManager;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Resource that uses DrawingToolLayer to render drawn data for multiple users
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 23, 2012            mschenke    Initial creation
 * Jan 30, 2014 2698       bclement    changed UserId to VenueParticipant
 * Feb 13, 2014 2751       bclement    VenueParticipant refactor
 * Mar 18, 2014 2895       njensen     Fix concurrent mod exception on dispose
 * May 05, 2014 3076       bclement    old CLEAR_ALL is now DISPOSE_ALL,
 *                                      added clearLayers() and getAllDrawingLayers()
 * Jun 30, 2014 1798       bclement    added getManager()
 * Jan 13, 2015 3709       bclement    SessionColorManager API changes
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class CollaborationDrawingResource extends
        AbstractVizResource<CollaborationDrawingResourceData, IDescriptor> {

    private SessionContainer container;

    private VenueParticipant myUser;

    private Map<VenueParticipant, DrawingToolLayer> layerMap;

    private CollaborationDrawingUIManager manager;

    private boolean lockingDrawing = false;

    /**
     * @param resourceData
     * @param loadProperties
     */
    public CollaborationDrawingResource(
            CollaborationDrawingResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        container = SharedDisplaySessionMgr.getSessionContainer(resourceData
                .getSessionId());
        if (container == null) {
            throw new VizException("Could not find container for sessionId: "
                    + resourceData.getSessionId());
        }

        myUser = container.getSession().getUserID();
        layerMap = new HashMap<VenueParticipant, DrawingToolLayer>();
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
        EditableManager.makeEditable(this, true);
        if (layerMap == null) {
            layerMap = new HashMap<VenueParticipant, DrawingToolLayer>();
        }

        OutlineCapability outline = getCapability(OutlineCapability.class);
        outline.setLineStyle(LineStyle.SOLID);
        outline.setOutlineWidth(4);
        outline.setSuppressingMenuItems(true);

        ColorableCapability colorable = getCapability(ColorableCapability.class);
        colorable.setSuppressingMenuItems(true);

        if (isSessionLeader()) {
            CollaborationDrawingEvent event = new CollaborationDrawingEvent(
                    resourceData.getDisplayId());
            event.setUserName(myUser);
            event.setType(CollaborationEventType.DISPOSE_ALL);
            sendEvent(event);
        }

        manager = new CollaborationDrawingUIManager(this);
        container.getSession().registerEventHandler(this);

        getDrawingLayerFor(getMyUser()).setDrawMode(
                resourceData.getResourceMode());
        setLockingDrawing(resourceData.isLocking());

        if (!isSessionLeader()) {
            CollaborationDrawingEvent event = new CollaborationDrawingEvent(
                    resourceData.getDisplayId());
            event.setUserName(getMyUser());
            event.setType(CollaborationEventType.NEW_USER_ARRIVED);
            sendEvent(event);
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
        if (target instanceof DispatchGraphicsTarget) {
            // Ensure we paint to our own target only
            target = ((DispatchGraphicsTarget) target).getWrappedObject();
        }

        OutlineCapability outline = getCapability(OutlineCapability.class);

        synchronized (layerMap) {
            for (VenueParticipant user : layerMap.keySet()) {
                DrawingToolLayer layer = layerMap.get(user);
                if (layer != null) {
                    layer.setEraserWidth(16); // Configure?
                    layer.setLineStyle(outline.getLineStyle());
                    layer.setLineWidth(outline.getOutlineWidth());
                    UserColorInfo colorInfo = container.getColorManager()
                            .getColorForUser(user);
                    layer.setColor(colorInfo.getForeground());
                    layer.paint(target, paintProps);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.rsc.AbstractVizResource#disposeInternal()
     */
    @Override
    protected void disposeInternal() {
        container.getSession().unregisterEventHandler(this);
        disposeLayers();
        layerMap = null;

        manager.dispose();
    }

    private void disposeLayers() {
        synchronized (layerMap) {
            for (DrawingToolLayer layer : layerMap.values()) {
                layer.dispose();
            }
            layerMap.clear();
        }
    }

    /**
     * Clear all drawing layers. Does not generate any collaboration events.
     * This is not "undoable".
     */
    private void clearLayers() {
        synchronized (layerMap) {
            for (DrawingToolLayer layer : layerMap.values()) {
                layer.clearAllDrawingData();
            }
        }
    }

    /**
     * @return the myUser
     */
    public VenueParticipant getMyUser() {
        return myUser;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.rsc.IVizResource#getName()
     */
    @Override
    public String getName() {
        return "Telestrator Drawing Tool";
    }

    /**
     * Get the DrawingToolLayer object associated with the user, one will be
     * created if none exists
     * 
     * @param user
     * @return
     */
    public DrawingToolLayer getDrawingLayerFor(VenueParticipant user) {
        if (layerMap != null) {
            synchronized (layerMap) {
                DrawingToolLayer layer = layerMap.get(user);
                if (layer == null) {
                    if (user == myUser) {
                        layer = new CollaborationDrawingToolLayer(
                                descriptor.getGridGeometry(), this);
                    } else {
                        layer = new DrawingToolLayer(
                                descriptor.getGridGeometry());
                    }
                    layerMap.put(user, layer);
                }
                return layer;
            }
        }
        return null;
    }

    /**
     * A collection of drawing layers for resource
     * 
     * @return empty collection if there are no layers
     */
    public Collection<DrawingToolLayer> getAllDrawingLayers() {
        Collection<DrawingToolLayer> rval;
        if (layerMap != null) {
            synchronized (layerMap) {
                rval = new ArrayList<DrawingToolLayer>(layerMap.values());
            }
        } else {
            rval = Collections.emptyList();
        }
        return rval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractVizResource#project(org.opengis.
     * referencing.crs.CoordinateReferenceSystem)
     */
    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        if (layerMap != null) {
            synchronized (layerMap) {
                for (DrawingToolLayer layer : layerMap.values()) {
                    layer.reproject(descriptor.getGridGeometry());
                }
            }
        }
    }

    /**
     * @return
     */
    public boolean isSessionLeader() {
        return container.getSession().getCurrentSessionLeader()
                .isSameUser(myUser);
    }

    /**
     * @return
     */
    public boolean isLockingDrawing() {
        return lockingDrawing;
    }

    public void setLockingDrawing(boolean lockingDrawing) {
        if (this.lockingDrawing != lockingDrawing) {
            resourceData.setLocking(lockingDrawing);
            this.lockingDrawing = lockingDrawing;
            if (isSessionLeader()) {
                CollaborationDrawingEvent event = new CollaborationDrawingEvent(
                        resourceData.getDisplayId());
                event.setUserName(myUser);
                event.setType(lockingDrawing ? CollaborationEventType.LOCK_USERS
                        : CollaborationEventType.UNLOCK_USERS);
                sendEvent(event);
            }
        }
    }

    /**
     * Checks if this resource is able to tellestrate given its state
     * 
     * @return
     */
    public boolean canTellestrate() {
        return getCapability(EditableCapability.class).isEditable()
                && (isSessionLeader() || isLockingDrawing() == false);
    }

    @Subscribe
    public void participantChanged(IVenueParticipantEvent event) {
        VenueParticipant user = event.getParticipant();
        switch (event.getEventType()) {
        case DEPARTED:
            synchronized (layerMap) {
                DrawingToolLayer layer = layerMap.remove(user);
                if (layer != null) {
                    layer.dispose();
                    issueRefresh();
                }
            }
            break;
        default:
            break;
        }
    }

    @Subscribe
    public void handleDrawEvent(CollaborationDrawingEvent event) {
        VenueParticipant user = event.getUserName();
        if (event.getDisplayId() != resourceData.getDisplayId()
                || user.isSameUser(myUser)) {
            // Early exit case, don't process my own events twice
            issueRefresh();
            return;
        }

        synchronized (layerMap) {
            DrawingToolLayer layer = getDrawingLayerFor(user);
            List<Coordinate> points = event.getCoordinates();
            switch (event.getType()) {
            case CLEAR:
                layer.clear();
                break;
            case LOCK_USERS:
                // lock
                setLockingDrawing(true);
                break;
            case UNLOCK_USERS:
                // unlock
                setLockingDrawing(false);
                break;
            case DRAW:
                layer.setDrawMode(DrawMode.DRAW);
                if (points != null && points.size() > 0) {
                    for (Coordinate c : points) {
                        layer.addCoordinate(c);
                    }
                    layer.doneDrawing();
                }
                break;
            case ERASE:
                layer.setDrawMode(DrawMode.ERASE);
                if (points != null && points.size() > 0) {
                    for (Coordinate c : points) {
                        layer.addCoordinate(c);
                    }
                    layer.doneErasing();
                    IRenderableDisplay display = descriptor
                            .getRenderableDisplay();
                    layer.processErase(display.getExtent(), display.getBounds());
                }
                break;
            case REDO:
                layer.redo();
                break;
            case UNDO:
                layer.undo();
                break;
            case DISPOSE_ALL:
                disposeLayers();
                break;
            case CLEAR_ALL:
                clearLayers();
                break;
            case NEW_USER_ARRIVED:
                CollaborationDrawingToolLayer myLayer = (CollaborationDrawingToolLayer) getDrawingLayerFor(getMyUser());
                InitialCollaborationData dataBundle = new InitialCollaborationData(
                        getMyUser(), resourceData.getDisplayId(),
                        isSessionLeader(), isLockingDrawing(), myLayer);
                sendEventToUser(dataBundle, event.getUserName());
                break;
            default:
                break;
            }
        }
        issueRefresh();
    }

    @Subscribe
    public void initialDataReceived(InitialCollaborationData dataBundle) {
        if (resourceData.getDisplayId() == dataBundle.getDisplayId()) {
            DrawingToolLayer layer = getDrawingLayerFor(dataBundle
                    .getUserName());
            if (layer != null) {
                layer.rebuildLayer(dataBundle.getCurrrentData(),
                        dataBundle.getUndoData(), dataBundle.getRedoData());
            }
            if (dataBundle.isSessionLeader()) {
                setLockingDrawing(dataBundle.isDrawingLocked());
            }
            issueRefresh();
        }
    }

    public void sendEvent(Object event) {
        try {
            container.getSession().sendObjectToVenue(event);
        } catch (CollaborationException e) {
            Activator.statusHandler.handle(Priority.PROBLEM,
                    e.getLocalizedMessage(), e);
        }
    }

    public void sendEventToUser(Object event, VenueParticipant user) {
        try {
            container.getSession().sendObjectToPeer(user, event);
        } catch (CollaborationException e) {
            Activator.statusHandler.handle(Priority.PROBLEM,
                    e.getLocalizedMessage(), e);
        }
    }

    /**
     * @return the container
     */
    public SessionContainer getContainer() {
        return container;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.rsc.AbstractVizResource#okToUnload()
     */
    @Override
    public boolean okToUnload() {
        // Though I hate this methods exists, it serves its purpose
        return false;
    }

    /**
     * @return the manager
     */
    public CollaborationDrawingUIManager getManager() {
        return manager;
    }

}
