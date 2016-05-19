package com.raytheon.uf.viz.collaboration.ui;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PartInitException;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.osgi.framework.Bundle;

import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession;
import com.raytheon.uf.viz.collaboration.comm.identity.event.IRosterChangeEvent;
import com.raytheon.uf.viz.collaboration.comm.identity.event.RosterChangeType;
import com.raytheon.uf.viz.collaboration.comm.identity.user.IUser;
import com.raytheon.uf.viz.collaboration.comm.provider.connection.CollaborationConnection;
import com.raytheon.uf.viz.collaboration.comm.provider.event.BookmarkEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.event.UserPresenceChangedEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.user.ContactsManager;
import com.raytheon.uf.viz.collaboration.comm.provider.user.ContactsManager.GroupListener;
import com.raytheon.uf.viz.collaboration.comm.provider.user.IDConverter;
import com.raytheon.uf.viz.collaboration.comm.provider.user.SharedGroup;
import com.raytheon.uf.viz.collaboration.comm.provider.user.UserId;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueId;
import com.raytheon.uf.viz.collaboration.ui.actions.AddNotifierAction;
import com.raytheon.uf.viz.collaboration.ui.actions.AddToGroupAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ArchiveViewerAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeFontAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangePasswordAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeRoleAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeSiteAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeStatusAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeStatusMessageAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeTextColorAction;
import com.raytheon.uf.viz.collaboration.ui.actions.CreateSessionAction;
import com.raytheon.uf.viz.collaboration.ui.actions.DeleteGroupAction;
import com.raytheon.uf.viz.collaboration.ui.actions.DisplayFeedAction;
import com.raytheon.uf.viz.collaboration.ui.actions.InviteAction;
import com.raytheon.uf.viz.collaboration.ui.actions.JoinRoomAction;
import com.raytheon.uf.viz.collaboration.ui.actions.LinkToEditorAction;
import com.raytheon.uf.viz.collaboration.ui.actions.LoginAction;
import com.raytheon.uf.viz.collaboration.ui.actions.LogoutAction;
import com.raytheon.uf.viz.collaboration.ui.actions.PeerToPeerChatAction;
import com.raytheon.uf.viz.collaboration.ui.actions.RemoveBookmarkAction;
import com.raytheon.uf.viz.collaboration.ui.actions.RemoveFromGroupAction;
import com.raytheon.uf.viz.collaboration.ui.actions.RemoveFromRosterAction;
import com.raytheon.uf.viz.collaboration.ui.actions.SendSubReqAction;
import com.raytheon.uf.viz.collaboration.ui.actions.ShowVenueAction;
import com.raytheon.uf.viz.collaboration.ui.actions.UserSearchAction;
import com.raytheon.uf.viz.collaboration.ui.colors.UserColorConfigManager;
import com.raytheon.uf.viz.collaboration.ui.data.AlertWordWrapper;
import com.raytheon.uf.viz.collaboration.ui.data.CollaborationGroupContainer;
import com.raytheon.uf.viz.collaboration.ui.data.PublicRoomContainer;
import com.raytheon.uf.viz.collaboration.ui.data.SessionGroupContainer;
import com.raytheon.uf.viz.collaboration.ui.notifier.NotifierTools;
import com.raytheon.uf.viz.collaboration.ui.prefs.CollabPrefConstants;
import com.raytheon.uf.viz.collaboration.ui.session.AbstractSessionView;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.icon.IconUtil;
import com.raytheon.viz.ui.views.CaveFloatingView;
import com.raytheon.viz.ui.views.CaveWorkbenchPageManager;
import com.raytheon.viz.ui.widgets.FilterDelegate;

/**
 * This class is the main view to display the user's information and allow the
 * user to create sessions.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 1, 2012             rferrel     Initial creation
 * Oct 22, 2013 #2483      lvenable    Fixed image memory leak.
 * Dec  6, 2013 2561       bclement    removed ECF
 * Dec 19, 2013 2563       bclement    added subscribe method for server disconnection
 * Dec 20, 2013 2563       bclement    fixed support for ungrouped roster items
 * Jan 24, 2014 2701       bclement    removed local groups, added shared groups
 *                                     removed option to create empty group
 * Jan 27, 2014 2700       bclement    fixed context menu for roster entries
 * Jan 30, 2014 2698       bclement    fixed alias not working for roster entries
 *                                     removed unneeded subscription for nickname changed events
 * Feb 12, 2014 2799       bclement    fixed double click chat not working for roster entries
 * Feb 24, 2014 2632       mpduff      Add Notifier actions.
 * Mar 05, 2014 2837       bclement    separate rename action for groups, added more icons
 * Mar 05, 2014 2798       mpduff      Add getter for displayFeedAction.
 * Mar 12, 2014 2632       mpduff      Force group deletes from UI if last user is removed.
 * Apr 11, 2014 2903       bclement    login action changes, removed server disconnect listener, 
 *                                      added static utility method to show view
 * May 19, 2014 3180       bclement    fixed inviting multiple users to session
 * Oct 08, 2014 3705       bclement    added room search and bookmarking
 * Oct 14, 2014 3709       mapeters    Added change background/foreground color actions to menu.
 * Nov 14, 2014 3709       mapeters    Removed change background/foreground color actions from menu.
 * Dec 08, 2014 3709       mapeters    Added MB3 change user text color actions to contacts list.
 * Dec 12, 2014 3709       mapeters    Store {@link ChangeTextColorAction}s in map, dispose them.
 * Jan 09, 2015 3709       bclement    color config manager API changes
 * Jan 13, 2015 3709       bclement    ChangeTextColorAction API changes
 * Jun 16, 2015 4401       bkowal      Re-factored filtering into a common location.
 * 
 * </pre>
 * 
 * @author rferrel
 * @version 1.0
 */
public class CollaborationGroupView extends CaveFloatingView implements
        GroupListener, IUserSelector {
    public static final String ID = "com.raytheon.uf.viz.collaboration.ui.CollaborationGroupView";

    private TreeViewer usersTreeViewer;

    private CollaborationGroupContainer topLevel;

    private FilterDelegate filterDelegate;

    private CreateSessionAction createSessionAction;

    private Action aliasAction;

    private Action renameAction;

    private DisplayFeedAction displayFeedAction;

    private Action collapseAllAction;

    private TreeEditor treeEditor;

    private Composite parent;

    private LogoutAction logOut;

    private Action roomSearchAction;

    private Map<String, ChangeTextColorAction<?>> userColorActions;

    /**
     * @param parent
     */
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        this.parent = parent;
        this.parent.setLayout(new GridLayout());

        // build the necessary actions for the view
        createActions();

        // add some actions to the toolbar
        createToolbar();

        // add some actions to the menubar
        createMenubar();
        openConnection();
    }

    private void openConnection() {
        CollaborationConnection connection = CollaborationConnection
                .getConnection();
        if (connection == null) {
            if (!new LoginAction().login()) {
                // user cancelled login
                return;
            }
            connection = CollaborationConnection.getConnection();
        }

        this.filterDelegate = new FilterDelegate(parent, new UsersTreeFilter());
        createUsersTree(parent);
        addDoubleClickListeners();
        createContextMenu();

        if (connection != null) {
            connection.registerEventHandler(this);
        }
        connection.getContactsManager().addGroupListener(this);
        populateTree();
        usersTreeViewer.refresh();
        parent.layout();
    }

    @Override
    public void dispose() {
        CollaborationConnection connection = CollaborationConnection
                .getConnection();
        if (connection != null) {
            connection.unregisterEventHandler(this);
            connection.getContactsManager().removeGroupListener(this);
        }
        super.dispose();

        for (ChangeTextColorAction<?> userColorAction : userColorActions
                .values()) {
            userColorAction.dispose();
        }
    }

    /**
     * 
     */
    private void createActions() {
        Bundle bundle = Activator.getDefault().getBundle();
        final IUserSelector userSelector = this;

        userColorActions = new HashMap<>();

        createSessionAction = new CreateSessionAction(userSelector);

        aliasAction = new Action("Alias", IconUtil.getImageDescriptor(Activator
                .getDefault().getBundle(), "alias.gif")) {
            @Override
            public void run() {
                aliasItem(getId());
            };
        };

        renameAction = new Action("Rename Group", IconUtil.getImageDescriptor(
                Activator.getDefault().getBundle(), "rename_group.gif")) {
            @Override
            public void run() {
                aliasItem(getId());
            };
        };

        collapseAllAction = new Action("Collapse All") {
            @Override
            public void run() {
                if (usersTreeViewer != null) {
                    usersTreeViewer.collapseAll();
                }
            }
        };
        collapseAllAction.setImageDescriptor(IconUtil.getImageDescriptor(
                bundle, "collapseall.gif"));

        // this is either on or off, so it is a toggle
        displayFeedAction = new DisplayFeedAction();

        roomSearchAction = new Action("Public Room Search...",
                IconUtil.getImageDescriptor(Activator.getDefault().getBundle(),
                        "spyglass.gif")) {
            public void run() {
                new RoomSearchDialog(Display.getCurrent().getActiveShell())
                        .open();
            };
        };

        this.disableOrEnableToolbarActions();
    }

    /**
     * Create the toolbar on top of the group view
     */
    private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(createSessionAction);
        mgr.add(displayFeedAction);
        mgr.add(collapseAllAction);
        mgr.add(LinkToEditorAction.getInstance(getViewSite()
                .getWorkbenchWindow()));
    }

    /**
     * Create the menu bar that is shown when the user clicks the down arrow
     * next to the toolbar
     */
    private void createMenubar() {
        IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
        createMenu(mgr);
        mgr.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.removeAll();
                createMenu(manager);
            }
        });
    }

    private void createMenu(IMenuManager mgr) {
        mgr.add(new UserSearchAction());
        mgr.add(roomSearchAction);
        mgr.add(new Separator());
        mgr.add(new ChangeFontAction());
        mgr.add(new Separator("afterFont"));
        mgr.add(new ChangeStatusAction());
        mgr.add(new ChangeStatusMessageAction());
        mgr.add(new ChangePasswordAction());
        mgr.add(new Separator());
        mgr.add(new ChangeRoleAction());
        mgr.add(new ChangeSiteAction());
        mgr.add(new Separator());
        mgr.add(new ArchiveViewerAction());
        mgr.add(new Separator());

        if (CollaborationConnection.getConnection() != null) {
            logOut = new LogoutAction();
            mgr.add(logOut);
        } else {
            mgr.add(new LoginAction());
        }
    }

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(usersTreeViewer.getControl());
        usersTreeViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, usersTreeViewer);
    }

    /**
     * Get entries for all part of the Tree Viewer and enable actions.
     */
    protected void populateTree() {
        CollaborationConnection connection = CollaborationConnection
                .getConnection();
        // set all the menu actions to false to start with
        if (connection == null) {
            usersTreeViewer.getTree().setEnabled(false);
            return;
        }

        // add ability to drop new groups as well as drop roster entries into
        // new "custom" groups
        addDragAndDrop();

        // enable the tree, and then refresh it just to be safe
        usersTreeViewer.getTree().setEnabled(true);
        usersTreeViewer.refresh(topLevel, true);
        this.disableOrEnableToolbarActions();
    }

    /**
     * Filling the context menu for the tree depending on whether the item is a
     * group or a user
     * 
     * @paramfillContextMenu manager
     */
    private void fillContextMenu(IMenuManager manager) {
        TreeSelection selection = (TreeSelection) usersTreeViewer
                .getSelection();
        Object o = selection.getFirstElement();

        // handle the session group portion of the group view
        if (o instanceof SessionGroupContainer) {
            manager.add(createSessionAction);
            return;
        } else if (o instanceof PublicRoomContainer) {
            manager.add(roomSearchAction);
            return;
        } else if (o instanceof IVenueSession) {
            manager.add(new ShowVenueAction((IVenueSession) o));
            manager.add(new ArchiveViewerAction((IVenueSession) o));
            return;
        } else if (o instanceof RosterEntry) {
            RosterEntry entry = (RosterEntry) o;
            UserId user = IDConverter.convertFrom(entry);
            addOnlineMenuOptions(manager, selection, user);
            addAliasAction(manager, selection, user);
            manager.add(new ArchiveViewerAction(user));
            manager.add(new AddToGroupAction(getSelectedUsers()));
            String groupName = null;
            Object group = selection.getPaths()[0].getFirstSegment();
            if (group instanceof RosterGroup) {
                groupName = ((RosterGroup) group).getName();
                manager.add(new RemoveFromGroupAction(groupName,
                        getSelectedUsers()));
            } else if (!(group instanceof SharedGroup)) {
                manager.add(new RemoveFromRosterAction(entry));
            }
            if (ContactsManager.isBlocked(entry)) {
                manager.add(new SendSubReqAction(entry));
            }
            manager.add(new AddNotifierAction(this));
            manager.add(new Separator());
            String colorActionKey = user.getFQName();
            ChangeTextColorAction<?> userColorAction = userColorActions
                    .get(colorActionKey);
            if (userColorAction == null) {
                userColorAction = new ChangeTextColorAction<IUser>(user, false,
                        false, false, UserColorConfigManager.getInstance());
                userColorActions.put(colorActionKey, userColorAction);
            }
            manager.add(userColorAction);
        } else if (o instanceof UserId) {
            // the user
            UserId user = (UserId) o;
            CollaborationConnection connection = CollaborationConnection
                    .getConnection();
            UserId me = connection.getUser();
            if (me.isSameUser(user)) {
                createMenu(manager);
                String colorActionKey = user.getFQName();
                ChangeTextColorAction<?> userColorAction = userColorActions
                        .get(colorActionKey);
                if (userColorAction == null) {
                    userColorAction = new ChangeTextColorAction<IUser>(user,
                            true, true, false,
                            UserColorConfigManager.getInstance());
                    userColorActions.put(colorActionKey, userColorAction);
                }
                manager.insertBefore("afterFont", userColorAction);
            }
        } else if (o instanceof RosterGroup || o instanceof SharedGroup) {
            Action inviteAction = new InviteAction(getSelectedUsers());
            if (inviteAction.isEnabled()) {
                manager.add(inviteAction);
            }
            manager.add(createSessionAction);
            if (o instanceof RosterGroup) {
                RosterGroup group = (RosterGroup) o;
                manager.add(new DeleteGroupAction(group.getName()));
                renameAction.setId(group.getName());
                manager.add(renameAction);
            }
        } else if (o instanceof VenueId) {
            VenueId venue = (VenueId) o;
            manager.add(new JoinRoomAction(venue, false));
            manager.add(new JoinRoomAction(venue, true));
            manager.add(new RemoveBookmarkAction(venue));
        }
    }

    /**
     * Add interaction menu options for contact if they are online
     * 
     * @param manager
     * @param selection
     * @param user
     */
    private void addOnlineMenuOptions(IMenuManager manager,
            TreeSelection selection, UserId user) {
        CollaborationConnection connection = CollaborationConnection
                .getConnection();
        Presence presence = connection.getContactsManager().getPresence(user);
        if (presence != null && presence.getType() == Type.available) {
            Action inviteAction = new InviteAction(getSelectedUsers());
            if (inviteAction.isEnabled()) {
                manager.add(inviteAction);
            }
            Action p2pAction = new PeerToPeerChatAction(user);
            if (p2pAction.isEnabled()) {
                manager.add(p2pAction);
            }
            manager.add(new Separator());
            manager.add(createSessionAction);
        }
    }

    /**
     * Add menu option for aliasing username
     * 
     * @param manager
     * @param selection
     * @param user
     */
    private void addAliasAction(IMenuManager manager, TreeSelection selection,
            UserId user) {
        CollaborationConnection connection = CollaborationConnection
                .getConnection();
        String name = connection.getContactsManager().getDisplayName(user);
        aliasAction.setId(name);
        manager.add(aliasAction);
    }

    private void addDoubleClickListeners() {
        usersTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                TreeSelection selection = (TreeSelection) usersTreeViewer
                        .getSelection();
                Object o = selection.getFirstElement();
                if (o instanceof RosterEntry) {
                    UserId user = IDConverter.convertFrom((RosterEntry) o);
                    new PeerToPeerChatAction(user).run();
                } else if (o instanceof IVenueSession) {
                    new ShowVenueAction((IVenueSession) o).run();
                } else if (o instanceof VenueId) {
                    new JoinRoomAction((VenueId) o, false).run();
                }
            }
        });
    }

    /**
     * Add the ability to alias items in the group view and then to have that
     * alias be used anywhere else that the UserId is used
     * 
     * Saves to an xml file in localization where the user can edit if desired
     */
    protected void aliasItem(final String editableText) {
        Control oldEditor = treeEditor.getEditor();
        if (oldEditor != null) {
            oldEditor.dispose();
        }
        TreeSelection selection = (TreeSelection) usersTreeViewer
                .getSelection();
        final Object selectedObj = selection.getFirstElement();

        final TreeItem item = usersTreeViewer.getTree().getSelection()[0];
        final Composite composite = new Composite(usersTreeViewer.getTree(),
                SWT.NONE);
        composite.setBackground(Display.getCurrent().getSystemColor(
                SWT.COLOR_BLACK));
        final Text modText = new Text(composite, SWT.NONE);
        composite.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Rectangle rect = composite.getClientArea();
                modText.setBounds(rect.x + 1, rect.y + 1, rect.width - 2,
                        rect.height - 2);
            }
        });
        Listener textListener = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                switch (e.type) {
                case SWT.KeyUp:
                    if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                        // do nothing, want to go on to the focus out
                    } else {
                        break;
                    }
                case SWT.FocusOut:
                    String fullText = modText.getText();
                    composite.dispose();
                    changeText(selectedObj, fullText);
                    refreshUsersTreeViewerAsync(selectedObj);
                    break;
                case SWT.Verify:
                    String newText = modText.getText();
                    String leftText = newText.substring(0, e.start);
                    String rightText = newText.substring(e.end,
                            newText.length());
                    GC gc = new GC(modText);
                    Point size = gc.textExtent(leftText + e.text + rightText);
                    gc.dispose();
                    size = modText.computeSize(size.x, SWT.DEFAULT);
                    treeEditor.horizontalAlignment = SWT.LEFT;
                    Rectangle itemRect = item.getBounds(),
                    rect = usersTreeViewer.getTree().getClientArea();
                    treeEditor.minimumWidth = Math.max(size.x, itemRect.width) + 2;
                    int left = itemRect.x,
                    right = rect.x + rect.width;
                    treeEditor.minimumWidth = Math.min(treeEditor.minimumWidth,
                            right - left);
                    treeEditor.minimumHeight = size.y + 2;
                    treeEditor.layout();
                    break;
                }
            }
        };

        modText.addListener(SWT.KeyUp, textListener);
        modText.addListener(SWT.Verify, textListener);
        modText.addListener(SWT.FocusOut, textListener);
        treeEditor.setEditor(composite, item);

        modText.setText(editableText);
        modText.selectAll();
        modText.setFocus();
    }

    protected void changeText(Object selectedObj, String newText) {
        if (selectedObj instanceof RosterEntry) {
            UserId user = IDConverter.convertFrom((RosterEntry) selectedObj);
            user.setAlias(newText);
            CollaborationConnection.getConnection().getContactsManager()
                    .setNickname(user, newText);
            CollaborationConnection.getConnection().postEvent(user);
            for (RosterGroup group : CollaborationConnection.getConnection()
                    .getContactsManager().getGroups(user)) {
                usersTreeViewer.refresh(group);
            }
        } else if (selectedObj instanceof RosterGroup) {
            RosterGroup group = (RosterGroup) selectedObj;
            CollaborationConnection.getConnection().getContactsManager()
                    .renameGroup(group.getName(), newText);
        }
    }

    /**
     * Generate the Tree View component and add tooltip tracking.
     * 
     * @param parent
     */
    private void createUsersTree(Composite parent) {
        Composite child = new Composite(parent, SWT.NONE);
        child.setLayout(new GridLayout(1, false));
        child.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        usersTreeViewer = new TreeViewer(child, SWT.VIRTUAL | SWT.MULTI
                | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        this.filterDelegate.setTreeViewer(usersTreeViewer);
        usersTreeViewer.getTree().setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumn column = new TreeColumn(usersTreeViewer.getTree(), SWT.NONE);
        column.setWidth(200); // any width would work

        usersTreeViewer.setContentProvider(new UsersTreeContentProvider());
        usersTreeViewer.setLabelProvider(new UsersTreeLabelProvider());
        usersTreeViewer.setSorter(new UsersTreeViewerSorter());
        ColumnViewerToolTipSupport.enableFor(usersTreeViewer, ToolTip.RECREATE);
        topLevel = new CollaborationGroupContainer();
        this.filterDelegate.setFilterInput(topLevel);
        usersTreeViewer.setInput(topLevel);

        treeEditor = new TreeEditor(usersTreeViewer.getTree());
    }

    /**
     * Get a unique set of selected users that have a Type of AVAILABLE. This
     * does a recursive search so will work even when groups contain groups.
     * 
     * @return
     */
    @Override
    public UserId[] getSelectedUsers() {
        Set<UserId> selectedUsers = new HashSet<UserId>();
        IStructuredSelection selection = (IStructuredSelection) usersTreeViewer
                .getSelection();
        Object[] nodes = selection.toArray();

        for (Object node : nodes) {
            if (node instanceof UserId) {
                UserId user = (UserId) node;
                selectedUsers.add(user);
            } else if (node instanceof RosterEntry) {
                UserId user = IDConverter.convertFrom((RosterEntry) node);
                selectedUsers.add(user);
            } else if (node instanceof RosterGroup) {
                Collection<RosterEntry> entries = ((RosterGroup) node)
                        .getEntries();
                selectedUsers.addAll(getSelectedUsers(entries));
            } else if (node instanceof SharedGroup) {
                Collection<RosterEntry> entries = ((SharedGroup) node)
                        .getEntries();
                selectedUsers.addAll(getSelectedUsers(entries));
            }
        }

        return selectedUsers.toArray(new UserId[selectedUsers.size()]);
    }

    /**
     * This searches group entries and returns all users with Type AVAILABLE.
     * 
     * @param entries
     * @return users
     */
    private Set<UserId> getSelectedUsers(Collection<RosterEntry> entries) {
        Set<UserId> selectedUsers = new HashSet<UserId>();
        ContactsManager contacts = CollaborationConnection.getConnection()
                .getContactsManager();
        for (RosterEntry node : entries) {
            UserId user = IDConverter.convertFrom(node);
            Presence presence = contacts.getPresence(user);
            if (presence.getType() == Type.available) {
                selectedUsers.add(user);
            }
        }
        return selectedUsers;
    }

    private void refreshUsersTreeViewerAsync(final Object element) {
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                if (usersTreeViewer.getControl().isDisposed() == false) {
                    usersTreeViewer.refresh(element);
                }
            }
        });
    }

    /**
     * Refresh the labels on the View Tree to reflect presence change.
     * 
     * @param rosterEntry
     */
    @Subscribe
    public void handleModifiedPresence(UserPresenceChangedEvent event) {
        refreshUsersTreeViewerAsync(CollaborationConnection.getConnection()
                .getUser());
    }

    @Subscribe
    public void addAlertWords(AlertWordWrapper words) {
        for (IViewReference ref : getViewSite().getWorkbenchWindow()
                .getActivePage().getViewReferences()) {
            IViewPart viewPart = ref.getView(false);
            if (viewPart instanceof AbstractSessionView) {
                ((AbstractSessionView<?>) viewPart).setAlertWords(Arrays
                        .asList(words.getAlertWords()));
            }
        }
    }

    @Subscribe
    public void handleRosterChangeEvent(IRosterChangeEvent rosterChangeEvent) {
        /*
         * If a user is deleted and that user is the only user in a group the
         * group does not get removed from the UI even though the group was
         * deleted within XMPP. Force the delete here.
         */
        if (rosterChangeEvent.getType() == RosterChangeType.DELETE) {
            ContactsManager contacts = CollaborationConnection.getConnection()
                    .getContactsManager();
            for (RosterGroup group : contacts.getGroups()) {
                if (group.getEntryCount() == 0) {
                    groupDeleted(group);
                }
            }
        }

        // Refresh the whole tree since there can be instances of the same user
        // elsewhere that might not .equals this one.
        refreshUsersTreeViewerAsync(usersTreeViewer.getInput());
        NotifierTools.processNotifiers(rosterChangeEvent.getPresence());
    }

    @Subscribe
    public void handlSessionEvent(IVenueSession rosterChangeEvent) {
        refreshUsersTreeViewerAsync(topLevel.getSessionGroup());
    }

    @Subscribe
    public void handleBookmarkEvent(BookmarkEvent event) {
        refreshUsersTreeViewerAsync(topLevel.getPublicRoomGroup());
    }

    /**
     * Enables or disables the toolbar buttons based on whether or not the user
     * is connected to the xmpp server.
     */
    private void disableOrEnableToolbarActions() {
        boolean enabled = (CollaborationConnection.getConnection() != null);
        createSessionAction.setEnabled(enabled);
        displayFeedAction.setEnabled(enabled);
        collapseAllAction.setEnabled(enabled);
        LinkToEditorAction.getInstance(getViewSite().getWorkbenchWindow());
    }

    private void addDragAndDrop() {
        // CollaborationGroupDragNDrop dragNDropSource = new
        // CollaborationGroupDragNDrop(
        // usersTreeViewer);
        // usersTreeViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY,
        // new Transfer[] { TextTransfer.getInstance() }, dragNDropSource);
        // usersTreeViewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY,
        // new Transfer[] { TextTransfer.getInstance() }, dragNDropSource);
    }

    // Does nothing, but necessary due to ViewPart
    @Override
    public void setFocus() {
        // nothing to do in this method
    }

    @Override
    public void groupCreated(RosterGroup group) {
        refreshUsersTreeViewerAsync(usersTreeViewer.getInput());
    }

    @Override
    public void groupDeleted(RosterGroup group) {
        refreshUsersTreeViewerAsync(group);
        refreshUsersTreeViewerAsync(usersTreeViewer.getInput());
    }

    @Override
    public void userAdded(RosterGroup group, UserId user) {
        refreshUsersTreeViewerAsync(group);
    }

    @Override
    public void userDeleted(RosterGroup group, UserId user) {
        refreshUsersTreeViewerAsync(group);
    }

    /**
     * @return the displayFeedAction
     */
    public DisplayFeedAction getDisplayFeedAction() {
        return displayFeedAction;
    }

    /**
     * @see CaveWorkbenchPageManager#showView(String)
     * 
     * @param initFeedView
     *            true if feed view should be initialized like this is the first
     *            time the view has been opened
     * @throws PartInitException
     */
    public static void showView(boolean initFeedView) throws PartInitException {
        CollaborationGroupView view = (CollaborationGroupView) CaveWorkbenchPageManager
                .getActiveInstance().showView(ID);
        if (initFeedView) {
            // if autojoin is selected (to join the default room)
            if (Activator.getDefault().getPreferenceStore()
                    .getBoolean(CollabPrefConstants.AUTO_JOIN)) {
                DisplayFeedAction action = view.getDisplayFeedAction();
                action.setChecked(true);
                action.run();
            }
        }
    }
}
