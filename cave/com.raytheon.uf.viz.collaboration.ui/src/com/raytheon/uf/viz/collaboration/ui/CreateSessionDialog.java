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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.jivesoftware.smack.XMPPException;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.collaboration.comm.identity.CollaborationException;
import com.raytheon.uf.viz.collaboration.comm.identity.ISharedDisplaySession;
import com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession;
import com.raytheon.uf.viz.collaboration.comm.identity.user.SharedDisplayRole;
import com.raytheon.uf.viz.collaboration.comm.provider.Tools;
import com.raytheon.uf.viz.collaboration.comm.provider.connection.CollaborationConnection;
import com.raytheon.uf.viz.collaboration.comm.provider.connection.PeerToPeerCommHelper;
import com.raytheon.uf.viz.collaboration.comm.provider.session.CreateSessionData;
import com.raytheon.uf.viz.collaboration.comm.provider.session.SharedDisplaySession;
import com.raytheon.uf.viz.collaboration.comm.provider.session.VenueSession;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueId;
import com.raytheon.uf.viz.collaboration.display.data.SharedDisplaySessionMgr;
import com.raytheon.uf.viz.collaboration.display.roles.dataprovider.ISharedEditorsManagerListener;
import com.raytheon.uf.viz.collaboration.display.roles.dataprovider.SharedEditorsManager;
import com.raytheon.uf.viz.collaboration.ui.editor.CollaborationEditor;
import com.raytheon.uf.viz.collaboration.ui.prefs.CollabPrefConstants;
import com.raytheon.uf.viz.collaboration.ui.prefs.HandleUtil;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Collaboration creation dialog for sessions.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 15, 2012            rferrel     Initial creation
 * Dec 19, 2013 2563       bclement    disable shared display option if not supported by server
 * Jan 28, 2014 2698       bclement    added error display text
 * Jan 30, 2014 2698       bclement    added handle to join room with
 * Feb  3, 2014 2699       bclement    added default handle preference
 * Feb  7, 2014 2699       bclement    removed handle validation
 * Feb 11, 2014 2699       bclement    require non-blank handle
 * Mar 06, 2014 2848       bclement    moved session creation logic to separate method
 * Apr 16, 2014 3021       bclement    increased width of dialog
 * Apr 22, 2014 3056       bclement    made room name lowercase to match xmpp server
 * Jun 16, 2014 3288       bclement    added call to get full venue ID for chosen name
 * Jan 06, 2014 3933       bclement    moved logic to check for editor shareability to SharedEditorsmanager
 * 
 * </pre>
 * 
 * @author rferrel
 * @version 1.0
 */
public class CreateSessionDialog extends CaveSWTDialog {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(CreateSessionDialog.class);

    private Text nameTF;

    private Text handleTF;

    private Text subjectTF;

    private Button sharedSessionDisplay;

    private Button inviteUsers;

    private StyledText inviteMessageTF;

    private Label inviteLabel;

    private IPartListener editorChangeListener;

    private ISharedEditorsManagerListener sharedEditorsListener;

    private Text errorMessage;

    public CreateSessionDialog(Shell parentShell) {
        super(parentShell);
        setText("Create Session");
    }

    private Control createDialogArea(Composite parent) {
        Composite body = new Composite(parent, SWT.NONE);
        body.setLayout(new GridLayout(2, false));

        Label label = null;
        label = new Label(body, SWT.NONE);
        label.setText("Name: ");
        nameTF = new Text(body, SWT.BORDER);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.minimumWidth = 300;
        nameTF.setLayoutData(gd);
        VerifyListener validNameListener = new VerifyListener() {

            @Override
            public void verifyText(VerifyEvent e) {
                if (" \t\"&'/,<>@".indexOf(e.character) >= 0) {
                    e.doit = false;
                    // Toolkit.getDefaultToolkit().beep();
                }
            }
        };
        nameTF.addVerifyListener(validNameListener);

        label = new Label(body, SWT.NONE);
        label.setText("Handle: ");
        handleTF = new Text(body, SWT.BORDER);
        handleTF.setText(HandleUtil.getDefaultHandle());
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        handleTF.setLayoutData(gd);
        handleTF.setToolTipText("Default handle configuration available in preferences.");

        label = new Label(body, SWT.NONE);
        label.setText("Subject: ");
        subjectTF = new Text(body, SWT.BORDER);
        subjectTF.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sharedSessionDisplay = new Button(body, SWT.CHECK);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        gd.horizontalSpan = 2;
        sharedSessionDisplay.setLayoutData(gd);
        updateSharedSessionDisplay();

        inviteUsers = new Button(body, SWT.CHECK);
        inviteUsers.setSelection(true);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        gd.horizontalSpan = 2;
        inviteUsers.setLayoutData(gd);
        inviteUsers.setText("Invite Selected Users");
        // inviteUsers.setSelection(true);
        inviteUsers.setVisible(true);
        // label = new Label(body, SWT.NONE);
        // label.setText("");
        // label.setVisible(showInvite);
        inviteLabel = new Label(body, SWT.NONE);
        inviteLabel.setText("Message: ");
        inviteLabel.setToolTipText("Message to send to invited users");
        inviteMessageTF = new StyledText(body, SWT.BORDER | SWT.MULTI
                | SWT.WRAP | SWT.V_SCROLL);
        inviteMessageTF.setLayoutData(new GridData(GridData.FILL_BOTH));
        inviteMessageTF.pack();
        inviteMessageTF.setToolTipText("Message to send to invited users");
        Point p = inviteMessageTF.getSize();
        gd = (GridData) inviteMessageTF.getLayoutData();
        gd.heightHint = p.y * 3;
        inviteUsers.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selected = ((Button) e.widget).getSelection();
                inviteLabel.setVisible(selected);
                inviteMessageTF.setVisible(selected);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                boolean selected = ((Button) e.widget).getSelection();
                inviteLabel.setVisible(selected);
                inviteMessageTF.setVisible(selected);

            }
        });
        inviteLabel.setVisible(true);
        inviteMessageTF.setVisible(true);

        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            editorChangeListener = new IPartListener() {

                @Override
                public void partOpened(IWorkbenchPart part) {
                    // not used
                }

                @Override
                public void partDeactivated(IWorkbenchPart part) {
                    // not used
                }

                @Override
                public void partClosed(IWorkbenchPart part) {
                    // not used
                }

                @Override
                public void partBroughtToTop(IWorkbenchPart part) {
                    // not used
                }

                @Override
                public void partActivated(IWorkbenchPart part) {
                    if (part instanceof IEditorPart) {
                        updateSharedSessionDisplay();
                    }
                }
            };
            page.addPartListener(editorChangeListener);
        }

        AbstractEditor editor = EditorUtil
                .getActiveEditorAs(AbstractEditor.class);
        if (editor != null) {
            ISharedDisplaySession session = SharedEditorsManager
                    .getSharedEditorSession(editor);
            if (session != null) {
                sharedEditorsListener = new ISharedEditorsManagerListener() {
                    @Override
                    public void shareEditor(AbstractEditor editor) {
                        updateSharedSessionDisplay();
                    }

                    @Override
                    public void removeEditor(AbstractEditor editor) {
                        updateSharedSessionDisplay();
                    }
                };
                SharedEditorsManager.getManager(session).addListener(
                        sharedEditorsListener);
            }
        }

        gd = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        errorMessage = new Text(body, SWT.READ_ONLY | SWT.WRAP);
        errorMessage.setLayoutData(gd);
        Display display = errorMessage.getDisplay();
        errorMessage.setBackground(display
                .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessage.setForeground(display.getSystemColor(SWT.COLOR_RED));

        return body;
    }

    private static boolean isShareable(IWorkbenchPart part) {
        boolean rval = false;
        if (part instanceof AbstractEditor) {
            rval = SharedEditorsManager.canBeShared((AbstractEditor) part);
        }
        return rval;
    }

    private void updateSharedSessionDisplay() {
        IEditorPart editor = EditorUtil.getActiveEditorAs(IEditorPart.class);

        if (!sharedSessionDisplay.isDisposed()) {
            if (!serverSupportsSharing()) {
                disableShareOption(
                        "Not Supported By Server",
                        "Unable to create a shared display session because"
                                + " the server doesn't support shared display sessions.");
            } else if (editor instanceof CollaborationEditor) {
                disableShareOption("Client Session",
                        "Unable to create a shared display session because"
                                + " the active editor is a client session.");
            } else if (!isShareable(editor)) {
                disableShareOption("Not Shareable",
                        "Unable to create a shared display session because"
                                + " the active editor is not shareable.");
            } else if (editor != null
                    && editor instanceof AbstractEditor
                    && SharedEditorsManager
                            .isBeingShared((AbstractEditor) editor)) {
                disableShareOption("Already Shared",
                        "Unable to create a shared display session because"
                                + " the active editor is already "
                                + "in a shared display session.");
            } else {
                sharedSessionDisplay.setText("Create Shared Display Session");
                sharedSessionDisplay.getParent().setToolTipText("");
                this.enableOrDisableSharedDisplays();
            }
        }
    }

    /**
     * Disable create shared display checkbox
     * 
     * @param shortReason
     * @param longReason
     */
    private void disableShareOption(String shortReason, String longReason) {
        String text = String.format("Create Shared Display Session *%s*",
                shortReason);
        sharedSessionDisplay.setText(text);
        sharedSessionDisplay.setEnabled(false);
        sharedSessionDisplay.setSelection(false);
        sharedSessionDisplay.getParent().setToolTipText(longReason);
    }

    /**
     * @return true if the server supports shared display sessions
     */
    private boolean serverSupportsSharing() {
        return PeerToPeerCommHelper.getCollaborationHttpServer() != null;
    }

    @Override
    protected void disposed() {
        super.disposed();
        if (editorChangeListener != null) {
            IWorkbenchPage page = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage();
            page.removePartListener(editorChangeListener);
        }
        if (sharedEditorsListener != null) {
            AbstractEditor editor = EditorUtil
                    .getActiveEditorAs(AbstractEditor.class);
            if (editor != null) {
                ISharedDisplaySession session = SharedEditorsManager
                        .getSharedEditorSession(editor);
                if (session != null) {
                    SharedEditorsManager.getManager(session).removeListener(
                            sharedEditorsListener);
                }
            }
        }
    }

    @Override
    protected void initializeComponents(Shell shell) {
        shell.setLayout(new GridLayout(1, false));
        createDialogArea(shell);
        createButtonBar(shell);
    }

    private void createButtonBar(Composite parent) {
        GridData gd = null;
        Composite bar = new Composite(parent, SWT.NONE);

        // set up to center buttons.
        bar.setLayout(new GridLayout(0, true));
        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        bar.setLayoutData(gd);
        createButton(bar, IDialogConstants.OK_ID, "Create", true);

        createButton(bar, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void preOpened() {
        super.preOpened();
    }

    /**
     * Creates a new button with the given id.
     * <p>
     * The <code>Dialog</code> implementation of this framework method creates a
     * standard push button, registers it for selection events including button
     * presses, and registers default buttons with its shell. The button id is
     * stored as the button's client data. If the button id is
     * <code>IDialogConstants.CANCEL_ID</code>, the new button will be
     * accessible from <code>getCancelButton()</code>. If the button id is
     * <code>IDialogConstants.OK_ID</code>, the new button will be accesible
     * from <code>getOKButton()</code>. Note that the parent's layout is assumed
     * to be a <code>GridLayout</code> and the number of columns in this layout
     * is incremented. Subclasses may override.
     * </p>
     * 
     * @param parent
     *            the parent composite
     * @param id
     *            the id of the button (see <code>IDialogConstants.*_ID</code>
     *            constants for standard dialog button ids)
     * @param label
     *            the label from the button
     * @param defaultButton
     *            <code>true</code> if the button is to be the default button,
     *            and <code>false</code> otherwise
     * 
     * @return the new button
     * 
     * @see #getCancelButton
     * @see #getOKButton()
     */
    protected Button createButton(Composite parent, int id, String label,
            boolean defaultButton) {
        // increment the number of columns in the button bar
        ((GridLayout) parent.getLayout()).numColumns++;
        Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.minimumWidth = 70;
        button.setLayoutData(gd);
        button.setData(new Integer(id));
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                Integer val = (Integer) event.widget.getData();
                if (val != IDialogConstants.OK_ID) {
                    setReturnValue(null);
                    CreateSessionDialog.this.getShell().dispose();
                } else {
                    Text focusField = null;
                    List<String> errorMessages = new ArrayList<String>();
                    String subject = subjectTF.getText().trim();
                    String err = validateVenueName();
                    /*
                     * xmpp server lowercases all room names, lowercase here to
                     * match when we get names back from the server
                     */
                    String name = nameTF.getText().toLowerCase();
                    if (err != null) {
                        focusField = nameTF;
                        errorMessages.add(err);
                    }
                    String handle = handleTF.getText().trim();
                    if (handle.isEmpty()) {
                        if (focusField == null) {
                            focusField = handleTF;
                        }
                        errorMessages.add("Handle cannot be empty.");
                    }

                    if (focusField == null) {
                        VenueId venueId = VenueSession.createVenueId(name);
                        CreateSessionData result = new CreateSessionData(
                                venueId, handle);
                        result.setSubject(subject);
                        result.setCollaborationSessioh(sharedSessionDisplay
                                .getSelection());
                        if (inviteUsers == null) {
                            result.setInviteUsers(false);
                        } else {
                            result.setInviteUsers(inviteUsers.getSelection());
                            result.setInviteMessage(inviteMessageTF.getText());
                        }

                        IVenueSession session = null;
                        try {
                            session = create(result);
                            result.setSessionId(session.getSessionId());
                            setReturnValue(result);
                            CreateSessionDialog.this.getShell().dispose();
                        } catch (CollaborationException ex) {
                            errorMessage.setText(ex.getLocalizedMessage());
                            errorMessage.setVisible(true);
                            statusHandler.handle(Priority.ERROR,
                                    "Session Creation Error", ex);
                            event.doit = false;
                            setReturnValue(null);
                        }
                    } else {
                        StringBuilder sb = new StringBuilder();
                        String prefix = "";
                        for (String msg : errorMessages) {
                            sb.append(prefix).append(msg);
                            prefix = "\n";
                        }
                        errorMessage.setText(sb.toString());
                        errorMessage.setVisible(true);
                        event.doit = false;
                        setReturnValue(null);
                        focusField.setFocus();
                        focusField.selectAll();
                    }
                }
            }
        });
        if (defaultButton) {
            Shell shell = parent.getShell();
            if (shell != null) {
                shell.setDefaultButton(button);
            }
        }
        return button;
    }

    /**
     * Create session object, register required listeners and join.
     * 
     * @param data
     * @return
     * @throws CollaborationException
     */
    private IVenueSession create(CreateSessionData data)
            throws CollaborationException {
        CollaborationConnection connection = CollaborationConnection
                .getConnection();
        VenueSession session;
        if (data.isCollaborationSession()) {
            SharedDisplaySession displaySession = connection
                    .createCollaborationVenue(data);
            /*
             * this will register event bus listeners, needs to be done before
             * connecting to venue
             */
            SharedDisplaySessionMgr.registerSession(displaySession,
                    SharedDisplayRole.DATA_PROVIDER);
            session = displaySession;
        } else {
            session = connection.createTextOnlyVenue(data);
        }
        try {
            session.createVenue(data);
            if (data.isCollaborationSession()) {
                SharedDisplaySessionMgr.joinSession(session.getSessionId());
            }
        } catch (CollaborationException e) {
            if (data.isCollaborationSession()) {
                SharedDisplaySessionMgr.exitSession(session.getSessionId());
            }
            connection.removeSession(session);
            throw e;
        }
        connection.postEvent(session);
        return session;
    }

    private String validateVenueName() {
        String name = nameTF.getText().trim();
        nameTF.setText(name);
        String err = null;
        if (name.length() <= 0) {
            err = "Must have session name.";
        } else if (!Tools.isValidId(name)) {
            err = "Session name contains invalid characters.";
        } else {
            try {
                if (VenueSession.roomExistsOnServer(VenueId.DEFAULT_SUBDOMAIN,
                        name)) {
                    err = "Session already exists. Pick a different name.";
                }
            } catch (XMPPException e) {
                statusHandler.error("Unable to check room existence on server",
                        e);
            }
        }
        return err;
    }

    private void enableOrDisableSharedDisplays() {
        boolean sharedSessionsEnabled = Activator
                .getDefault()
                .getPreferenceStore()
                .getBoolean(
                        CollabPrefConstants.HttpCollaborationConfiguration.P_SESSION_CONFIGURED);
        this.sharedSessionDisplay.setSelection(sharedSessionsEnabled);
        this.sharedSessionDisplay.setEnabled(sharedSessionsEnabled);
    }

}