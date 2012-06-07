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
package com.raytheon.uf.viz.collaboration.ui.role;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;

import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.collaboration.comm.identity.CollaborationException;
import com.raytheon.uf.viz.collaboration.comm.identity.ISharedDisplaySession;
import com.raytheon.uf.viz.collaboration.comm.identity.event.IVenueParticipantEvent;
import com.raytheon.uf.viz.collaboration.comm.identity.event.ParticipantEventType;
import com.raytheon.uf.viz.collaboration.comm.identity.user.SharedDisplayRole;
import com.raytheon.uf.viz.collaboration.comm.provider.TransferRoleCommand;
import com.raytheon.uf.viz.collaboration.display.editor.ReprojectEditor;
import com.raytheon.uf.viz.collaboration.display.editor.SharedEditorData;
import com.raytheon.uf.viz.collaboration.ui.Activator;
import com.raytheon.uf.viz.collaboration.ui.ColorChangeEvent;
import com.raytheon.uf.viz.collaboration.ui.SessionColorManager;
import com.raytheon.uf.viz.collaboration.ui.data.SessionContainer;
import com.raytheon.uf.viz.collaboration.ui.data.SharedDisplaySessionMgr;
import com.raytheon.uf.viz.collaboration.ui.editor.EditorSetup;
import com.raytheon.uf.viz.collaboration.ui.editor.SharedResource;
import com.raytheon.uf.viz.collaboration.ui.editor.event.InputEvent;
import com.raytheon.uf.viz.collaboration.ui.role.dataprovider.CollaborationDispatcher;
import com.raytheon.uf.viz.collaboration.ui.rsc.CollaborationWrapperResource;
import com.raytheon.uf.viz.collaboration.ui.rsc.CollaborationWrapperResourceData;
import com.raytheon.uf.viz.collaboration.ui.rsc.DataProviderRscData;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceList.AddListener;
import com.raytheon.uf.viz.core.rsc.ResourceList.RemoveListener;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.remote.graphics.Dispatcher;
import com.raytheon.uf.viz.remote.graphics.DispatcherFactory;
import com.raytheon.uf.viz.remote.graphics.DispatchingGraphicsFactory;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * TODO: This class is in severe need of a refactor!
 * 
 * Handles the events of a session that are specific to the Data Provider role.
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 26, 2012            njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class DataProviderEventController extends AbstractRoleEventController
        implements IRenderableDisplayChangedListener {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataProviderEventController.class);

    private ResourceWrapperListener wrappingListener;

    private DispatcherFactory factory = new DispatcherFactory() {
        @Override
        public Dispatcher createNewDispatcher(IRenderableDisplay display)
                throws InstantiationException {
            try {
                CollaborationDispatcher dispatcher = new CollaborationDispatcher(
                        session, display);
                synchronized (dispatchers) {
                    dispatchers.add(dispatcher);
                    dispatcher.setActiveDisplay(activeDisplay);
                }
                return dispatcher;
            } catch (CollaborationException e) {
                throw new InstantiationException(
                        "Error constructing collaboration dispatcher: "
                                + e.getLocalizedMessage());
            }
        }
    };

    private List<CollaborationDispatcher> dispatchers = new LinkedList<CollaborationDispatcher>();

    private IRenderableDisplay activeDisplay;

    public DataProviderEventController(ISharedDisplaySession session) {
        super(session);
    }

    @Subscribe
    public void participantChanged(IVenueParticipantEvent event) {
        if (event.getEventType().equals(ParticipantEventType.ARRIVED)
                && !event.getParticipant().equals(session.getUserID())) {
            // TODO send over the one that is currently active, not the one
            SharedDisplaySessionMgr
                    .getSessionContainer(this.session.getSessionId())
                    .getSharedEditors().get(0);
            AbstractEditor editor = EditorUtil
                    .getActiveEditorAs(AbstractEditor.class);
            SharedEditorData se = EditorSetup.extractSharedEditorData(editor);

            // new color for each user
            SessionColorManager manager = SharedDisplaySessionMgr
                    .getSessionContainer(session.getSessionId())
                    .getColorManager();
            RGB color = manager.getColorFromUser(event.getParticipant());

            ColorChangeEvent cce = new ColorChangeEvent(event.getParticipant(),
                    color);
            try {
                session.sendObjectToVenue(cce);
                session.sendObjectToPeer(event.getParticipant(), se);
            } catch (CollaborationException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error sending initialization data to new participant "
                                + event.getParticipant().getName(), e);
            }

        }
    }

    @Subscribe
    public void roleTransferred(TransferRoleCommand cmd) {
        if (cmd.getRole() == SharedDisplayRole.SESSION_LEADER) {
            session.setCurrentSessionLeader(cmd.getUser());
            if (cmd.getUser().getFQName()
                    .equals(session.getUserID().getFQName())) {
                // this cave should assume session leader control
                InputUtil.enableDataProviderInput(session.getSessionId());
            } else if (session.getCurrentSessionLeader().getFQName()
                    .equals(session.getUserID().getFQName())
                    && !session.getCurrentSessionLeader().getFQName()
                            .equals(cmd.getUser().getFQName())) {
                // this cave should release session leader control
                InputUtil.disableDataProviderInput(session.getSessionId());
            }
        }
    }

    @Subscribe
    public void sessionLeaderInput(InputEvent event) {
        // TODO needs to be based on the editor that is both shared and active
        final AbstractEditor editor = SharedDisplaySessionMgr
                .getSessionContainer(session.getSessionId()).getSharedEditors()
                .get(0);
        IDisplayPane pane = editor.getDisplayPanes()[0];
        final Event swtEvent = new Event();
        swtEvent.display = editor.getActiveDisplayPane().getDisplay();

        // translate event type
        switch (event.getType()) {
        case MOUSE_DOWN:
            swtEvent.type = SWT.MouseDown;
            break;
        case MOUSE_UP:
            swtEvent.type = SWT.MouseUp;
            break;
        case MOUSE_DOWN_MOVE:
        case MOUSE_MOVE:
            swtEvent.type = SWT.MouseMove;
            break;
        case DOUBLE_CLICK:
            swtEvent.type = SWT.MouseDoubleClick;
            break;
        case MOUSE_HOVER:
            swtEvent.type = SWT.MouseHover;
            break;
        case MOUSE_WHEEL:
            swtEvent.type = SWT.MouseWheel;
            break;
        case KEY_DOWN:
            swtEvent.type = SWT.KeyDown;
            break;
        case KEY_UP:
            swtEvent.type = SWT.KeyUp;
            break;
        }

        // translate coordinates of event
        switch (event.getType()) {
        case MOUSE_DOWN:
        case MOUSE_DOWN_MOVE:
        case MOUSE_UP:
        case MOUSE_HOVER:
        case MOUSE_MOVE:
        case MOUSE_WHEEL:
        case DOUBLE_CLICK:
            double[] screen = pane.gridToScreen(new double[] { event.getX(),
                    event.getY(), 0.0 });
            swtEvent.x = (int) Math.round(screen[0]);
            swtEvent.y = (int) Math.round(screen[1]);
            break;
        }

        // translate specific metadata
        switch (event.getType()) {
        case MOUSE_DOWN:
        case MOUSE_DOWN_MOVE:
        case MOUSE_UP:
        case DOUBLE_CLICK:
            swtEvent.button = event.getEventData();
            break;
        case MOUSE_WHEEL:
            swtEvent.count = event.getEventData();
            break;
        case KEY_DOWN:
        case KEY_UP:
            swtEvent.keyCode = event.getEventData();
            break;
        }

        VizApp.runAsync(new Runnable() {

            @Override
            public void run() {
                editor.getMouseManager().handleEvent(swtEvent);
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.role.AbstractRoleEventController
     * #startup()
     */
    @Override
    public void startup() {
        super.startup();

        SessionContainer sessionContainer = SharedDisplaySessionMgr
                .getSessionContainer(session.getSessionId());
        SessionColorManager manager = sessionContainer.getColorManager();
        manager.addUser(session.getCurrentDataProvider());
        wrappingListener = new ResourceWrapperListener();
        for (AbstractEditor editor : sessionContainer.getSharedEditors()) {
            super.activateResources(editor);

            // Replace pane resources that will be shared with
            // CollaborationWrapperResource objects
            for (IDisplayPane pane : editor.getDisplayPanes()) {
                handleNewDisplay(pane);
                setActiveDisplay(pane.getRenderableDisplay());
            }

            editor.addRenderableDisplayChangedListener(this);
        }
    }

    private void setActiveDisplay(IRenderableDisplay display) {
        synchronized (dispatchers) {
            this.activeDisplay = display;
            for (CollaborationDispatcher dispatcher : dispatchers) {
                dispatcher.setActiveDisplay(display);
            }
        }
        ReprojectEditor event = new ReprojectEditor();
        event.setTargetGeometry(display.getDescriptor().getGridGeometry());
        try {
            session.sendObjectToVenue(event);
        } catch (CollaborationException e) {
            Activator.statusHandler.handle(
                    Priority.PROBLEM,
                    "Error sending reprojection event: "
                            + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Handles a new display pane
     * 
     * @param pane
     * @return true if the pane is not currently being handled by the controller
     */
    private boolean handleNewDisplay(IDisplayPane pane) {
        boolean newDisplay = false;
        synchronized (dispatchers) {
            for (CollaborationDispatcher dispatcher : dispatchers) {
                if (dispatcher.getDisplay() == pane.getRenderableDisplay()) {
                    // We already have a dispatcher for this display
                    return false;
                }
            }
        }
        try {
            if (DispatchingGraphicsFactory.injectRemoteFunctionality(pane,
                    factory)) {
                // If we injected successfully, do resource management
                ResourceList list = pane.getDescriptor().getResourceList();
                for (ResourcePair rp : pane.getDescriptor().getResourceList()) {
                    wrapResourcePair(rp);
                }
                list.addPreAddListener(wrappingListener);
                list.addPostRemoveListener(wrappingListener);
                newDisplay = true;
            }
        } catch (InstantiationException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        return newDisplay;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.role.AbstractRoleEventController
     * #getResourcesToAdd()
     */
    @Override
    protected List<ResourcePair> getResourcesToAdd() {
        List<ResourcePair> resources = super.getResourcesToAdd();
        ResourcePair resource = new ResourcePair();
        DataProviderRscData resourceData = new DataProviderRscData();
        resourceData.setSessionId(session.getSessionId());
        resource.setResourceData(resourceData);
        resource.setProperties(new ResourceProperties());
        resource.setLoadProperties(new LoadProperties());
        resources.add(resource);
        return resources;
    }

    private void sendSharedResource(ResourcePair rp, boolean remove) {
        // Send event to venue to load resource
        SharedResource sr = new SharedResource();
        ResourcePair copy = new ResourcePair();
        copy.setLoadProperties(rp.getLoadProperties());
        copy.setProperties(rp.getProperties());
        if (rp.getResourceData() instanceof CollaborationWrapperResourceData) {
            copy.setResourceData(((CollaborationWrapperResourceData) rp
                    .getResourceData()).getWrappedResourceData());
        } else if (rp.getResource() instanceof CollaborationWrapperResource) {
            copy.setResourceData(rp.getResource().getResourceData());
        }
        sr.setResource(copy);
        sr.setRemoveResource(remove);
        try {
            session.sendObjectToVenue(sr);
        } catch (CollaborationException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Wraps ResourcePair in collaboration wrapper resource if resource should
     * be loaded on locally for every user in venue
     * 
     * @param rp
     * @return true if ResourcePair was wrapped, false otherwise
     */
    private boolean wrapResourcePair(ResourcePair rp) {
        if (rp.getProperties() != null && rp.getProperties().isMapLayer()
                && (rp.getResource() != null || rp.getResourceData() != null)) {
            CollaborationWrapperResourceData wrapperRscData = new CollaborationWrapperResourceData();
            if (rp.getResource() != null) {
                wrapperRscData.setWrappedResourceData(rp.getResource()
                        .getResourceData());
                rp.setResource(new CollaborationWrapperResource(wrapperRscData,
                        rp.getLoadProperties(), rp.getResource()));
            } else {
                wrapperRscData.setWrappedResourceData(rp.getResourceData());
            }

            if (rp.getResourceData() != null) {
                rp.setResourceData(wrapperRscData);
            }
            return true;
        }
        return false;
    }

    private void unwrapResourcePair(ResourcePair rp) {
        if (rp.getResource() instanceof CollaborationWrapperResource) {
            rp.setResource(((CollaborationWrapperResource) rp.getResource())
                    .getWrappedResource());
        }
        if (rp.getResourceData() instanceof CollaborationWrapperResourceData) {
            rp.setResourceData(((CollaborationWrapperResourceData) rp
                    .getResourceData()).getWrappedResourceData());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.role.AbstractRoleEventController
     * #shutdown()
     */
    @Override
    public void shutdown() {
        super.shutdown();
        SessionContainer sc = SharedDisplaySessionMgr
                .getSessionContainer(session.getSessionId());
        if (sc != null) {
            for (AbstractEditor editor : sc.getSharedEditors()) {
                for (IDisplayPane pane : editor.getDisplayPanes()) {
                    ResourceList list = pane.getDescriptor().getResourceList();
                    for (ResourcePair rp : list) {
                        unwrapResourcePair(rp);
                    }
                    list.removePreAddListener(wrappingListener);
                    list.removePostRemoveListener(wrappingListener);
                }
                editor.removeRenderableDisplayChangedListener(this);
            }
        }

        synchronized (dispatchers) {
            // Dispatchers created are responsible for display extraction
            for (CollaborationDispatcher dispatcher : dispatchers) {
                dispatcher.dispose();
            }
            dispatchers.clear();
        }
    }

    private class ResourceWrapperListener implements AddListener,
            RemoveListener {
        @Override
        public void notifyAdd(ResourcePair rp) throws VizException {
            if (wrapResourcePair(rp)) {
                // Send event to venue to load
                sendSharedResource(rp, false);
            }
        }

        @Override
        public void notifyRemove(ResourcePair rp) throws VizException {
            if (rp.getResource() instanceof CollaborationWrapperResource) {
                // Send event to venue to unload
                sendSharedResource(rp, true);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.IRenderableDisplayChangedListener#
     * renderableDisplayChanged(com.raytheon.uf.viz.core.IDisplayPane,
     * com.raytheon.uf.viz.core.drawables.IRenderableDisplay,
     * com.raytheon.uf.viz
     * .core.IRenderableDisplayChangedListener.DisplayChangeType)
     */
    @Override
    public void renderableDisplayChanged(IDisplayPane pane,
            IRenderableDisplay newRenderableDisplay, DisplayChangeType type) {
        if (type == DisplayChangeType.ADD) {
            if (handleNewDisplay(pane)) {
                activateResources(newRenderableDisplay);
            }
            if (newRenderableDisplay.getGraphicsAdapter() instanceof DispatchingGraphicsFactory) {
                synchronized (dispatchers) {
                    for (CollaborationDispatcher dispatcher : dispatchers) {
                        if (dispatcher.getDisplay() == newRenderableDisplay) {
                            setActiveDisplay(newRenderableDisplay);
                            break;
                        }
                    }
                }
            }
        }
    }
}
