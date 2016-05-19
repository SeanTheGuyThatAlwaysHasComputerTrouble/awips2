package com.raytheon.uf.viz.collaboration.ui.session;

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

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.osgi.framework.Bundle;

import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.collaboration.comm.identity.CollaborationException;
import com.raytheon.uf.viz.collaboration.comm.identity.ISharedDisplaySession;
import com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession;
import com.raytheon.uf.viz.collaboration.comm.identity.invite.ColorPopulator;
import com.raytheon.uf.viz.collaboration.comm.identity.user.IUser;
import com.raytheon.uf.viz.collaboration.comm.identity.user.SharedDisplayRole;
import com.raytheon.uf.viz.collaboration.comm.provider.event.LeaderChangeEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueParticipant;
import com.raytheon.uf.viz.collaboration.display.IRemoteDisplayContainer;
import com.raytheon.uf.viz.collaboration.display.IRemoteDisplayContainer.IRemoteDisplayChangedListener;
import com.raytheon.uf.viz.collaboration.display.IRemoteDisplayContainer.RemoteDisplay;
import com.raytheon.uf.viz.collaboration.display.IRemoteDisplayContainer.RemoteDisplayChangeType;
import com.raytheon.uf.viz.collaboration.display.data.ColorChangeEvent;
import com.raytheon.uf.viz.collaboration.display.data.SessionContainer;
import com.raytheon.uf.viz.collaboration.display.data.SessionContainer.IDisplayContainerChangedListener;
import com.raytheon.uf.viz.collaboration.display.data.SharedDisplaySessionMgr;
import com.raytheon.uf.viz.collaboration.display.data.UserColorInfo;
import com.raytheon.uf.viz.collaboration.display.rsc.SelfAddingSystemResourceListener;
import com.raytheon.uf.viz.collaboration.display.rsc.telestrator.CollaborationDrawingEvent;
import com.raytheon.uf.viz.collaboration.display.rsc.telestrator.CollaborationDrawingEvent.CollaborationEventType;
import com.raytheon.uf.viz.collaboration.display.rsc.telestrator.CollaborationDrawingResource;
import com.raytheon.uf.viz.collaboration.display.rsc.telestrator.CollaborationDrawingResourceData;
import com.raytheon.uf.viz.collaboration.display.rsc.telestrator.CollaborationDrawingToolLayer;
import com.raytheon.uf.viz.collaboration.display.rsc.telestrator.CollaborationDrawingUIManager;
import com.raytheon.uf.viz.collaboration.ui.Activator;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeTextColorAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeTextColorAction.ChangeTextColorCallback;
import com.raytheon.uf.viz.core.ContextManager;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.icon.IconUtil;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.uf.viz.drawing.DrawingToolLayer;
import com.raytheon.uf.viz.drawing.DrawingToolLayer.DrawMode;
import com.raytheon.viz.ui.input.EditableManager;

/**
 * View class for a collaboration (shared display) session
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 1, 2012             rferrel     Initial creation
 * Jan 28, 2014 2698       bclement    removed venue info
 * Jan 30, 2014 2698       bclement    changed UserId to VenueParticipant
 * Feb 12, 2014 2751       njensen     Added transfer leadership and shutdown safety
 * Feb 18, 2014 2751       bclement    update participants list and notify on leader change
 * Feb 19, 2014 2751       bclement    add change color and transfer leader icons
 * Mar 06, 2014 2751       bclement    moved users table refresh logic to refreshParticipantList()
 * Mar 06, 2014 2848       bclement    moved colormanager update code to session container
 * Mar 11, 2014 2865       lvenable    Added null checks in threads
 * Mar 18, 2014 2895       njensen     Fix lockAction enable/disable logic
 * Apr 15, 2014 2822       bclement    only allow transfer leader if participant is using shared display
 * May 05, 2014 3076       bclement    added clear all action
 * Jun 30, 2014 1798       bclement    added disableCurrentLayer()
 * Dev 02, 2014 3709       mapeters    added {@link #initComponents()} override
 * Jan 09, 2015 3709       bclement    now uses ForegroundColorDlg for consistency
 * Jan 13, 2015 3709       bclement    now uses ChangeTextColorAction for consistency
 * May 22, 2015 4328       mapeters    Add NOTIFICATION_IMAGE_NAME, getter
 * 
 * </pre>
 * 
 * @author rferrel
 * @version 1.0
 */
public class CollaborationSessionView extends SessionView implements
        IPartListener, IRemoteDisplayChangedListener,
        IDisplayContainerChangedListener {
    public static final String ID = "com.raytheon.uf.viz.collaboration.CollaborationSession";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(CollaborationSessionView.class);

    private static final String COLLABORATION_SESSION_IMAGE_NAME = "messages.gif";

    private static final String NOTIFICATION_IMAGE_NAME = "messages_notification.gif";

    private Runnable actionUpdater = new Runnable() {
        @Override
        public void run() {
            updateToolItems();
        }
    };

    private Action leaderChangeAction;

    private ActionContributionItem drawAction;

    private ActionContributionItem undoAction;

    private ActionContributionItem redoAction;

    private ActionContributionItem eraseAction;

    private ActionContributionItem clearAction;

    private ActionContributionItem lockAction;

    private ActionContributionItem clearAllAction;

    private ControlContribution noEditorAction;

    private ISharedDisplaySession session;

    private IRemoteDisplayContainer container;

    private IRenderableDisplay currentDisplay;

    private boolean locked = false;

    private DrawMode drawMode = DrawMode.NONE;

    private Map<IRenderableDisplay, SelfAddingSystemResourceListener> listeners = new IdentityHashMap<IRenderableDisplay, SelfAddingSystemResourceListener>();

    public IRemoteDisplayContainer getDisplayContainer() {
        return container;
    }

    public CollaborationDrawingResource getCurrentDrawingResource() {
        CollaborationDrawingResource currentResource = null;
        if (currentDisplay != null) {
            for (CollaborationDrawingResource resource : currentDisplay
                    .getDescriptor()
                    .getResourceList()
                    .getResourcesByTypeAsType(
                            CollaborationDrawingResource.class)) {
                currentResource = resource;
                break;
            }
        }
        return currentResource;
    }

    private DrawingToolLayer getCurrentLayer() {
        CollaborationDrawingResource resource = getCurrentDrawingResource();
        if (resource != null) {
            return resource.getDrawingLayerFor(resource.getMyUser());
        }
        return null;
    }

    /**
     * @see CollaborationDrawingResource#getAllDrawingLayers()
     * @return empty collection if no layers are found
     */
    private Collection<DrawingToolLayer> getAllLayers() {
        Collection<DrawingToolLayer> rval;
        CollaborationDrawingResource resource = getCurrentDrawingResource();
        if (resource != null) {
            rval = resource.getAllDrawingLayers();
        } else {
            rval = Collections.emptyList();
        }
        return rval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite)
     */
    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        site.getPage().addPartListener(this);
        SessionContainer sc = SharedDisplaySessionMgr
                .getSessionContainer(sessionId);
        if (sc != null) {
            session = sc.getSession();
            container = sc.getDisplayContainer();
            if (container != null) {
                container.addRemoteDisplayChangedListener(this);
                /*
                 * need to watch the session container's reference to an
                 * IRemoteDisplayContainer because if this CAVE loses or gains
                 * leadership, that reference will be swapped out underneath
                 */
                sc.addDisplayContainerChangedListener(this);
                RemoteDisplay remoteDisplay = container.getActiveDisplay();
                if (remoteDisplay != null) {
                    remoteDisplayChanged(container.getActiveDisplay(),
                            RemoteDisplayChangeType.ACTIVATED);
                }
            }
        }
    }

    @Override
    protected void createActions() {
        super.createActions();
        Bundle bundle = Activator.getDefault().getBundle();

        leaderChangeAction = new Action("Transfer Leadership",
                IconUtil.getImageDescriptor(bundle, "leader_transfer.gif")) {
            @Override
            public void run() {
                IStructuredSelection selection = (IStructuredSelection) usersTable
                        .getSelection();
                VenueParticipant newLeader = (VenueParticipant) selection
                        .getFirstElement();
                try {
                    session.changeLeader(newLeader);
                } catch (CollaborationException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error transferring leadership", e);
                }
            }
        };

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.views.CaveFloatingView#createToolbarButton()
     */
    @Override
    protected void createToolbarButton() {
        super.createToolbarButton();

        drawAction = new ActionContributionItem(new Action("Draw", SWT.TOGGLE) {
            @Override
            public void run() {
                toggleDrawMode(DrawMode.DRAW);
            }
        });
        drawAction.getAction().setImageDescriptor(
                IconUtil.getImageDescriptor(
                        com.raytheon.uf.viz.drawing.Activator.getDefault()
                                .getBundle(), "draw.gif"));
        CollaborationDrawingResource resource = getCurrentDrawingResource();
        if (resource != null) {
            addEditableListener(resource.getResourceData());
        }
        undoAction = new ActionContributionItem(new Action("Undo") {
            @Override
            public void run() {
                DrawingToolLayer layer = getCurrentLayer();
                if (layer != null) {
                    layer.undo();
                }
                updateToolItems();
            }
        });
        undoAction.getAction().setImageDescriptor(
                IconUtil.getImageDescriptor(
                        com.raytheon.uf.viz.drawing.Activator.getDefault()
                                .getBundle(), "undo.gif"));

        redoAction = new ActionContributionItem(new Action("Redo") {
            @Override
            public void run() {
                DrawingToolLayer layer = getCurrentLayer();
                if (layer != null) {
                    layer.redo();
                }
                updateToolItems();
            }
        });
        redoAction.getAction().setImageDescriptor(
                IconUtil.getImageDescriptor(
                        com.raytheon.uf.viz.drawing.Activator.getDefault()
                                .getBundle(), "redo.gif"));

        eraseAction = new ActionContributionItem(
                new Action("Erase", SWT.TOGGLE) {
                    @Override
                    public void run() {
                        toggleDrawMode(DrawMode.ERASE);
                    }
                });
        eraseAction.getAction().setImageDescriptor(
                IconUtil.getImageDescriptor(
                        com.raytheon.uf.viz.drawing.Activator.getDefault()
                                .getBundle(), "eraser.png"));

        clearAction = new ActionContributionItem(new Action("Clear") {
            @Override
            public void run() {
                DrawingToolLayer layer = getCurrentLayer();
                if (layer != null) {
                    layer.clear();
                }
                updateToolItems();
            };
        });
        clearAction.getAction().setImageDescriptor(
                IconUtil.getImageDescriptor(
                        com.raytheon.uf.viz.drawing.Activator.getDefault()
                                .getBundle(), "remove.gif"));

        lockAction = new ActionContributionItem(new Action(
                "Lock Collaborators", SWT.TOGGLE) {
            @Override
            public void run() {
                CollaborationDrawingResource resource = getCurrentDrawingResource();
                if (resource != null) {
                    resource.setLockingDrawing(((ToolItem) lockAction
                            .getWidget()).getSelection());
                    locked = resource.isLockingDrawing();
                    updateToolItems();
                }
            };
        });
        lockAction.getAction().setImageDescriptor(
                IconUtil.getImageDescriptor(Activator.getDefault().getBundle(),
                        "lock.gif"));
        clearAllAction = new ActionContributionItem(new Action("Clear All") {
            @Override
            public void run() {
                clearAllDrawingLayers();
            }
        });
        clearAllAction.getAction().setImageDescriptor(
                IconUtil.getImageDescriptor(Activator.getDefault().getBundle(),
                        "clear_all.gif"));
        noEditorAction = new ControlContribution("noEditorAction") {

            @Override
            protected Control createControl(Composite parent) {
                Label l = new Label(parent, SWT.NONE);
                l.setImage(IconUtil.getImageDescriptor(
                        Activator.getDefault().getBundle(), "warning.gif")
                        .createImage());
                if (session.getCurrentSessionLeader().isSameUser(
                        session.getUserID())) {
                    l.setToolTipText("You are not viewing a shared display");
                } else {
                    l.setToolTipText("The leader is not viewing a shared display.");
                }
                return l;
            }
        };

        ToolBarManager mgr = (ToolBarManager) getViewSite().getActionBars()
                .getToolBarManager();

        mgr.insert(mgr.getSize() - 1, new Separator());
        mgr.insert(mgr.getSize() - 1, drawAction);

        mgr.insert(mgr.getSize() - 1, undoAction);
        mgr.insert(mgr.getSize() - 1, redoAction);
        mgr.insert(mgr.getSize() - 1, clearAction);
        mgr.insert(mgr.getSize() - 1, eraseAction);
        mgr.insert(mgr.getSize() - 1, new Separator());
        mgr.insert(mgr.getSize() - 1, lockAction);
        mgr.insert(mgr.getSize() - 1, clearAllAction);
        mgr.insert(mgr.getSize() - 1, new Separator());

        updateToolItems();
    }

    /**
     * Clear all drawing layers and send clear all event
     */
    private void clearAllDrawingLayers() {
        for (DrawingToolLayer layer : getAllLayers()) {
            layer.clearAllDrawingData();
        }
        CollaborationDrawingResource resource = getCurrentDrawingResource();
        CollaborationDrawingEvent event = new CollaborationDrawingEvent(
                resource.getResourceData().getDisplayId());
        event.setType(CollaborationEventType.CLEAR_ALL);
        event.setUserName(resource.getMyUser());
        resource.sendEvent(event);
        updateToolItems();
    }

    private void toggleDrawMode(DrawMode mode) {
        if (mode != DrawMode.NONE) {
            CollaborationDrawingResource resource = getCurrentDrawingResource();
            DrawingToolLayer layer = resource != null ? resource
                    .getDrawingLayerFor(resource.getMyUser()) : null;
            if (layer != null) {
                if (layer.getDrawMode() == mode) {
                    layer.setDrawMode(DrawMode.NONE);
                } else {
                    layer.setDrawMode(mode);
                }
                drawMode = layer.getDrawMode();

                // make editable so mouse actions work on it and not others
                if (resource.isSessionLeader()) {
                    EditableManager.makeEditable(resource, true);
                }
                updateToolItems();
            }
        }
    }

    /**
     * @return true if any drawing layer has been drawn on
     */
    private boolean anyLayerHasDrawing() {
        boolean anyCanClear = false;
        for (DrawingToolLayer dtl : getAllLayers()) {
            if (dtl.hasDrawing()) {
                anyCanClear = true;
                break;
            }
        }
        return anyCanClear;
    }

    public void updateToolItems() {
        ToolBarManager mgr = (ToolBarManager) getViewSite().getActionBars()
                .getToolBarManager();
        mgr.remove(noEditorAction);
        if (currentDisplay == null) {
            mgr.insert(0, noEditorAction);
        }
        CollaborationDrawingResource currentResource = getCurrentDrawingResource();
        DrawingToolLayer layer = getCurrentLayer();
        if (layer != null && currentResource.isSessionLeader()) {
            lockAction.getAction().setEnabled(true);
            clearAllAction.getAction().setEnabled(anyLayerHasDrawing());
        } else {
            lockAction.getAction().setEnabled(false);
            clearAllAction.getAction().setEnabled(false);
        }
        // enable/disable toolbar buttons based on locked
        if (layer != null
                && (locked == false || currentResource.isSessionLeader())) {
            drawAction.getAction().setEnabled(true);
            undoAction.getAction().setEnabled(layer.canUndo());
            redoAction.getAction().setEnabled(layer.canRedo());
            clearAction.getAction().setEnabled(layer.hasDrawing());
            eraseAction.getAction().setEnabled(true);
            switch (layer.getDrawMode()) {
            case DRAW:
                drawAction.getAction().setChecked(
                        currentResource.getCapability(EditableCapability.class)
                                .isEditable());
                eraseAction.getAction().setChecked(false);
                break;
            case ERASE:
                drawAction.getAction().setChecked(false);
                eraseAction.getAction().setChecked(
                        currentResource.getCapability(EditableCapability.class)
                                .isEditable());
                break;
            case NONE:
                drawAction.getAction().setChecked(false);
                eraseAction.getAction().setChecked(false);
                break;
            }
            lockAction.getAction().setChecked(
                    currentResource.isLockingDrawing());
        } else {
            drawAction.getAction().setEnabled(false);
            undoAction.getAction().setEnabled(false);
            redoAction.getAction().setEnabled(false);
            clearAction.getAction().setEnabled(false);
            eraseAction.getAction().setEnabled(false);
        }
        getViewSite().getActionBars().getToolBarManager().update(true);
    }

    /**
     * @return the redoAction
     */
    public ActionContributionItem getRedoAction() {
        return redoAction;
    }

    /**
     * @return the undoAction
     */
    public ActionContributionItem getUndoAction() {
        return undoAction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#initColorManager
     * ()
     */
    @Override
    protected void initColorManager() {
        colorManager = SharedDisplaySessionMgr.getSessionContainer(sessionId)
                .getColorManager();
    }

    @Override
    protected String getSessionImageName() {
        return COLLABORATION_SESSION_IMAGE_NAME;
    }

    @Override
    protected String getNotificationImageName() {
        return NOTIFICATION_IMAGE_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#sendMessage()
     */
    @Override
    public void sendMessage() {
        String message = getComposedMessage();
        if (message.length() > 0) {
            try {
                VenueParticipant id = session.getUserID();
                appendMessage(id, System.currentTimeMillis(), message, null);
                ((IVenueSession) session).sendChatMessage(message);
            } catch (CollaborationException e) {
                statusHandler.handle(Priority.ERROR,
                        "Unable to send chat message", e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#fillContextMenu
     * (org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        super.fillContextMenu(manager);
        if (session.hasRole(SharedDisplayRole.DATA_PROVIDER)
                || session.hasRole(SharedDisplayRole.SESSION_LEADER)) {
            if (session.hasRole(SharedDisplayRole.SESSION_LEADER)) {
                manager.add(new Separator());

                IStructuredSelection selection = (IStructuredSelection) usersTable
                        .getSelection();
                VenueParticipant entry = (VenueParticipant) selection
                        .getFirstElement();
                if (!entry.isSameUser(session.getUserID())
                        && session.isSharedDisplayClient(entry)) {
                    manager.add(leaderChangeAction);
                }
            }
        }
    }

    /**
     * Callback used in the change color action. Gets the new color from the
     * dialog and sends a color change event to the session
     */
    private final ChangeTextColorCallback sendColorEventCallback = new ChangeTextColorCallback() {
        @Override
        public void newColor(IUser user, UserColorInfo colors) {
            IStructuredSelection selection = (IStructuredSelection) usersTable
                    .getSelection();
            VenueParticipant entry = (VenueParticipant) selection
                    .getFirstElement();
            ColorChangeEvent event = new ColorChangeEvent(entry,
                    colors.getForeground());
            try {
                session.sendObjectToVenue(event);
            } catch (CollaborationException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to send color change to venue", e);
            }
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#addColorAction
     * (org.eclipse.jface.action.IMenuManager,
     * com.raytheon.uf.viz.collaboration.comm.provider.user.VenueParticipant,
     * boolean)
     */
    @Override
    protected void addColorAction(IMenuManager manager, VenueParticipant user,
            boolean me) {
        if (session.hasRole(SharedDisplayRole.SESSION_LEADER)) {
            String colorActionKey = user.getFQName();

            ChangeTextColorAction<VenueParticipant> userColorAction = userColorActions
                    .get(colorActionKey);
            if (userColorAction == null) {
                userColorAction = new ChangeTextColorAction<VenueParticipant>(
                        user, me, me, true, colorManager);
                userColorAction.setActionCallback(sendColorEventCallback);
                userColorActions.put(colorActionKey, userColorAction);
            }
            manager.add(userColorAction);
        }
    }

    @Subscribe
    public void modifyColors(ColorPopulator populator) {
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                if (usersTable.getTable().isDisposed() == false) {
                    usersTable.refresh();
                }
            }
        });
    }

    @Subscribe
    public void modifyColors(ColorChangeEvent event) {
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                if (usersTable.getTable().isDisposed() == false) {
                    usersTable.refresh();
                }
            }
        });
    }

    @Subscribe
    public void collaborationEvent(CollaborationDrawingEvent event) {
        // we need to check here for the event type otherwise it may not get set
        // before this is called (since it depends on the event bus), this
        // handles locking and unlocking of the toolbar in the view
        if (event.getType() == CollaborationEventType.LOCK_USERS) {
            locked = true;
        } else if (event.getType() == CollaborationEventType.UNLOCK_USERS) {
            locked = false;
        }
        VizApp.runAsync(actionUpdater);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.collaboration.ui.session.AbstractSessionView#
     * setMessageLabel(org.eclipse.swt.widgets.Label)
     */
    @Override
    protected void setMessageLabel(Composite comp) {
        Label label = new Label(comp, SWT.NONE);
        StringBuilder labelInfo = new StringBuilder();
        if (session != null) {
            String subject = ((IVenueSession) session).getVenue().getSubject();
            labelInfo.append(subject);
            label.setToolTipText(subject);
        }
        label.setText(labelInfo.toString());
    }

    public String getSessionId() {
        return session.getSessionId();
    }

    @Override
    public void dispose() {
        synchronized (listeners) {
            Collection<SelfAddingSystemResourceListener> values = listeners
                    .values();
            for (SelfAddingSystemResourceListener listener : values) {
                listener.dispose();
            }
            listeners.clear();
        }

        SharedDisplaySessionMgr.exitSession(session.getSessionId());
        session.close();
        super.dispose();
        getSite().getPage().removePartListener(this);
        if (container != null) {
            container.removeRemoteDisplayChangedListener(this);
        }
    }

    // =================== Context activation code ===================

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void partActivated(IWorkbenchPart part) {
        // only done if we care about the part that was activated
        if (container != null && container.getActiveDisplayEditor() == part) {
            ContextManager
                    .getInstance(getSite().getPage().getWorkbenchWindow())
                    .activateContexts(this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart
     * )
     */
    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void partClosed(IWorkbenchPart part) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart
     * )
     */
    @Override
    public void partDeactivated(IWorkbenchPart part) {
        // only done if we care about the part that was deactivated
        if (container != null && container.getActiveDisplayEditor() == part) {
            ContextManager
                    .getInstance(getSite().getPage().getWorkbenchWindow())
                    .deactivateContexts(this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
     */
    @Override
    public void partOpened(IWorkbenchPart part) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.collaboration.display.IRemoteDisplayContainer.
     * IRemoteDisplayChangedListener
     * #remoteDisplayChanged(com.raytheon.uf.viz.core
     * .drawables.IRenderableDisplay,
     * com.raytheon.uf.viz.collaboration.display.IRemoteDisplayContainer
     * .RemoteDisplayChangeType)
     */
    @Override
    public void remoteDisplayChanged(RemoteDisplay remoteDisplay,
            RemoteDisplayChangeType changeType) {
        // TODO this method has more to do with displays than the view,
        // would be good to separate it out somehow

        /* a null remoteDisplay means DEACTIVATE */
        if (remoteDisplay == null) {
            disableCurrentLayer();
            currentDisplay = null;
            VizApp.runAsync(actionUpdater);
            return;
        }
        IRenderableDisplay display = remoteDisplay.getDisplay();
        int displayId = remoteDisplay.getDisplayId();
        synchronized (listeners) {
            switch (changeType) {
            case CREATED:
                if (listeners.containsKey(display) == false) {
                    CollaborationDrawingResourceData resourceData = new CollaborationDrawingResourceData();
                    resourceData.setSessionId(sessionId);
                    resourceData.setDisplayId(displayId);
                    addEditableListener(resourceData);
                    try {
                        listeners.put(display,
                                new SelfAddingSystemResourceListener(
                                        resourceData, display.getDescriptor()));

                        CollaborationDrawingResource resource = getCurrentDrawingResource();
                        if (resource != null) {
                            DrawingToolLayer layer = getCurrentLayer();
                            /*
                             * on create, session leader will remove the
                             * drawing/erasing ability on create, participants
                             * will keep the drawing/erasing ability if it is
                             * active
                             */
                            if (layer != null) {
                                if (resource.isSessionLeader()) {
                                    layer.setDrawMode(DrawMode.NONE);
                                } else {
                                    layer.setDrawMode(drawMode);
                                }
                                drawMode = layer.getDrawMode();
                            }
                            resource.setLockingDrawing(locked);
                        }

                        display.refresh();
                    } catch (VizException e) {
                        Activator.statusHandler.handle(Priority.PROBLEM,
                                e.getLocalizedMessage(), e);
                    }
                }
                break;
            case ACTIVATED:
                if (listeners.containsKey(display) == false) {
                    remoteDisplayChanged(remoteDisplay,
                            RemoteDisplayChangeType.CREATED);
                }

                /*
                 * grab the current resource (before we change the display) so
                 * that we can set the DrawMode to DrawMode.NONE, therefore
                 * preventing drawing on the active editor and having it paint
                 * to the non-active editor
                 */
                CollaborationDrawingResource resource = getCurrentDrawingResource();
                if (resource != null) {
                    disableCurrentLayer();
                }
                currentDisplay = display;

                resource = getCurrentDrawingResource();
                /*
                 * on activate, all users will keep the current ability,
                 * drawing, erasing, or none
                 */
                if (resource != null) {
                    DrawingToolLayer layer = getCurrentLayer();
                    if (layer != null) {
                        layer.setDrawMode(drawMode);
                    }
                    resource.setLockingDrawing(locked);
                }
                VizApp.runAsync(actionUpdater);
                break;
            case DISPOSED:
                SelfAddingSystemResourceListener listener = listeners
                        .remove(display);
                if (listener != null) {
                    listener.dispose();
                }
                if (display == currentDisplay) {
                    disableCurrentLayer();
                    currentDisplay = null;
                    VizApp.runAsync(actionUpdater);
                }
                break;
            }
        }
    }

    /**
     * prevent the current layer from drawing/erasing and clear the cursor
     */
    private void disableCurrentLayer() {
        final DrawingToolLayer layer = getCurrentLayer();
        if (layer != null) {
            /*
             * Set done to handle disabling when the user is currently
             * drawing/erasing. Must be done before clearing cursor.
             */
            switch (layer.getDrawMode()) {
            case DRAW:
                layer.doneDrawing();
                break;
            case ERASE:
                layer.doneErasing();
                break;
            default:
                // not drawing
            }
            /*
             * Handles the case where the user has a draw/erase cursor, has the
             * mouse over the map, and the layer gets deactivated.
             */
            VizApp.runAsync(new Runnable() {
                @Override
                public void run() {
                    /* skip if we got a new layer which that updated the cursor */
                    if (getCurrentLayer() == null
                            && layer instanceof CollaborationDrawingToolLayer) {
                        CollaborationDrawingToolLayer cdtl = (CollaborationDrawingToolLayer) layer;
                        CollaborationDrawingUIManager manager = cdtl
                                .getResource().getManager();
                        if (manager != null) {
                            manager.clearCursor();
                        }
                    }
                }
            });
            layer.setDrawMode(DrawMode.NONE);
        }
    }

    /**
     * Listener provides the view with information that the resource has become
     * editable or not, which will affect any mouse handlers associated with it
     * 
     * @param resourceData
     */
    private void addEditableListener(
            CollaborationDrawingResourceData resourceData) {
        resourceData.addChangeListener(new IResourceDataChanged() {
            @Override
            public void resourceChanged(ChangeType type, Object object) {
                if (type == ChangeType.CAPABILITY
                        && object instanceof EditableCapability) {
                    VizApp.runAsync(actionUpdater);
                }
            }
        });
    }

    @Override
    public void displayContainerChanged(
            IRemoteDisplayContainer oldDisplayContainer,
            IRemoteDisplayContainer newDisplayContainer) {
        if (oldDisplayContainer != null) {
            oldDisplayContainer.removeRemoteDisplayChangedListener(this);
        }

        if (newDisplayContainer != null) {
            newDisplayContainer.addRemoteDisplayChangedListener(this);
        }
    }

    @Subscribe
    public void leaderChanged(final LeaderChangeEvent event) {
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                if (usersTable != null && !usersTable.getTable().isDisposed()) {
                    refreshParticipantList();
                }
                sendParticipantSystemMessage(event.getNewLeader(),
                        "is now leader.");
                updateToolItems();
            }
        });
    }

}
