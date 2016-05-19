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
package com.raytheon.uf.viz.collaboration.ui.session;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jivesoftware.smack.packet.Presence;

import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.viz.collaboration.comm.identity.IMessage;
import com.raytheon.uf.viz.collaboration.comm.identity.info.SiteConfigInformation;
import com.raytheon.uf.viz.collaboration.comm.provider.connection.CollaborationConnection;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueParticipant;
import com.raytheon.uf.viz.collaboration.ui.Activator;
import com.raytheon.uf.viz.collaboration.ui.SiteConfigurationManager;
import com.raytheon.uf.viz.collaboration.ui.actions.ChangeTextColorAction;
import com.raytheon.uf.viz.collaboration.ui.prefs.CollabPrefConstants;

/**
 * Built for the session in which everyone joins
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 7, 2012            mnash     Initial creation
 * Dec  6, 2013 2561       bclement    removed ECF
 * Dec 19, 2013 2563       bclement    moved participant filter logic to one method
 * Jan 08, 2014 2563       bclement    changes to match SiteConfigurationManager user sites config
 * Jan 30, 2014 2698       bclement    changed UserId to VenueParticipant
 * Feb 13, 2014 2751       bclement    VenueParticipant refactor
 * Feb 18, 2014 2631       mpduff      Add processJoinAlert()
 * Feb 19, 2014 2751       bclement    add change color icon, fix NPE when user cancels change color
 * Mar 05, 2014 2798       mpduff      Changed how messages are processed for the feed view.
 * Mar 06, 2014 2751       bclement    moved users table refresh logic to refreshParticipantList()
 * Mar 18, 2014 2798       mpduff      Fixed issue with user changing site and participant list not 
 *                                         having the color update to reflect the change.
 * Mar 24, 2014 2936       mpduff      Remove join alerts from feed view.
 * Mar 25, 2014 2938       mpduff      Show status message for site and role changes.
 * Apr 01, 2014 2938       mpduff      Update logic for site and role changes.
 * Apr 22, 2014 3038       bclement    added initialized flag to differentiate between roster population and new joins
 * Oct 10, 2014 3708       bclement    SiteConfigurationManager refactor
 * Nov 26, 2014 3709       mapeters    support foreground/background color preferences for each site
 * Dec 08, 2014 3709       mapeters    Removed ChangeSiteColorAction, uses {@link ChangeTextColorAction}.
 * Dec 12, 2014 3709       mapeters    Store {@link ChangeTextColorAction}s in map, dispose them.
 * Jan 05, 2015 3709       mapeters    Use both site and user name as key in siteColorActions map.
 * Jan 09, 2015 3709       bclement    color config manager API changes
 * Jan 12, 2015 3709       bclement    use parent object's session color manager, colors now based on user, not site
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class SessionFeedView extends SessionView {

    public static final String ID = "com.raytheon.uf.viz.collaboration.SessionFeedView";

    private Action autoJoinAction;

    private Action userAddSiteAction;

    private Action userRemoveSiteAction;

    private String actingSite;

    /**
     * Set of users logged in.
     */
    private final ConcurrentHashMap<String, Presence> otherParticipants = new ConcurrentHashMap<String, Presence>();

    private volatile boolean initialized = false;

    /**
     * 
     */
    public SessionFeedView() {
        super();
        actingSite = CollaborationConnection.getConnection().getPresence()
                .getProperty(SiteConfigInformation.SITE_NAME).toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#initComponents
     * (org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void initComponents(Composite parent) {
        super.initComponents(parent);
        usersTable.refresh();
    }

    @Subscribe
    public void refreshBlockList(SiteChangeEvent event) {
        this.actingSite = event.getNewSite();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#createActions()
     */
    @Override
    protected void createActions() {
        super.createActions();

        autoJoinAction = new Action(CollabPrefConstants.AUTO_JOIN, SWT.TOGGLE) {
            @Override
            public void run() {
                Activator
                        .getDefault()
                        .getPreferenceStore()
                        .setValue(CollabPrefConstants.AUTO_JOIN,
                                autoJoinAction.isChecked());
            };
        };

        autoJoinAction.setChecked(Activator.getDefault().getPreferenceStore()
                .getBoolean(CollabPrefConstants.AUTO_JOIN));
        Activator.getDefault().getPreferenceStore()
                .addPropertyChangeListener(new IPropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent event) {
                        autoJoinAction.setChecked(Activator.getDefault()
                                .getPreferenceStore()
                                .getBoolean(CollabPrefConstants.AUTO_JOIN));
                    }
                });

        userAddSiteAction = new Action("Show Messages from Site") {
            @Override
            public void run() {
                SiteConfigurationManager
                        .showSite(actingSite, getSelectedSite());
            };
        };

        userRemoveSiteAction = new Action("Hide Messages from Site") {
            @Override
            public void run() {
                SiteConfigurationManager
                        .hideSite(actingSite, getSelectedSite());
            }
        };

        MenuManager manager = (MenuManager) getViewSite().getActionBars()
                .getMenuManager();
        manager.add(autoJoinAction);
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
        String site = getSelectedSite();
        if (!SiteConfigurationManager.isVisible(actingSite, site)) {
            userAddSiteAction
                    .setText("Show Messages from " + getSelectedSite());
            manager.add(userAddSiteAction);
        } else {
            userRemoveSiteAction.setText("Hide Messages from "
                    + getSelectedSite());
            manager.add(userRemoveSiteAction);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#setParticipantValues
     * (com.raytheon.uf.viz.collaboration.ui.session.ParticipantsLabelProvider)
     */
    @Override
    protected void setParticipantValues(ParticipantsLabelProvider labelProvider) {
        super.setParticipantValues(labelProvider);
        labelProvider.setActingSite(actingSite);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.ui.session.SessionView#handleMessage
     * (com.raytheon.uf.viz.collaboration.comm.identity.IMessage)
     */
    @Override
    public void handleMessage(IMessage message) {
        final IMessage msg = message;
        boolean isHistory = isHistory(msg);

        // so not to have delay, going to handle messages from yourself
        // separately
        if (isSelf(msg, isHistory)) {
            return;
        }

        Object site = null;
        if (isHistory) {
            site = msg.getSubject();
        } else if (msg.getFrom() instanceof VenueParticipant) {
            Presence presence = session.getVenue().getPresence(
                    (VenueParticipant) msg.getFrom());
            site = presence.getProperty(SiteConfigInformation.SITE_NAME);
        }

        // should we append?
        if (site == null
                || SiteConfigurationManager.isVisible(actingSite,
                        site.toString())) {
            appendMessage(msg);
        }
    }

    /**
     * Get the selected user
     * 
     * @return
     */
    private VenueParticipant getSelectedParticipant() {
        IStructuredSelection selection = (IStructuredSelection) usersTable
                .getSelection();
        return (VenueParticipant) selection.getFirstElement();
    }

    /**
     * Get the acting site based on the first user that is selected out of the
     * list
     * 
     * @return
     */
    private String getSelectedSite() {
        VenueParticipant selectedEntry = getSelectedParticipant();
        Presence pres = session.getVenue().getPresence(selectedEntry);
        Object selectedSite = pres.getProperty(SiteConfigInformation.SITE_NAME);
        return selectedSite == null ? "" : selectedSite.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.collaboration.ui.session.SessionView#
     * sendParticipantSystemMessage
     * (com.raytheon.uf.viz.collaboration.comm.provider.user.UserId,
     * java.lang.String)
     */
    @Override
    protected void sendParticipantSystemMessage(VenueParticipant participant,
            String message) {
        super.sendParticipantSystemMessage(participant, message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.collaboration.ui.session.SessionView#
     * participantPresenceUpdated
     * (com.raytheon.uf.viz.collaboration.comm.provider.user.UserId,
     * org.jivesoftware.smack.packet.Presence)
     */
    @Override
    protected void participantPresenceUpdated(VenueParticipant participant,
            Presence presence) {
        /*
         * when we join, the room will send presence for everyone in the room,
         * then send us our own presence to signify that the list is done and we
         * have been initialized.
         */
        if (!initialized && session.getUserID().isSameUser(participant)) {
            initialized = true;
            /*
             * continue and print the message for ourselves joining which will
             * serve as a delimiter between historical messages and new messages
             */
        }
        // Verify we have properties
        if (!presence.getPropertyNames().contains(
                SiteConfigInformation.SITE_NAME)) {
            return;
        }

        String siteName = getSiteName(presence);

        // only show sites you care about
        if (SiteConfigurationManager.isVisible(actingSite, siteName)) {
            String user = participant.getName();
            Presence prev = otherParticipants.get(user);

            String roleName = getRoleName(presence);
            if (presence.isAvailable()) {
                /* only print announcements after we are initialized */
                if (initialized
                        && (prev == null || hasPresenceChanged(prev, presence))) {
                    StringBuilder message = getMessage(roleName, siteName, user);
                    sendSystemMessage(message);
                }

                otherParticipants.put(user, presence);
            }
        }

        refreshParticipantList();
    }

    /**
     * Determine if the user's presence has changed.
     * 
     * @param prev
     *            The previous Presence object
     * @param current
     *            The current Presence object
     * @return true if the presence has changed
     */
    private boolean hasPresenceChanged(Presence prev, Presence current) {
        if (!getRoleName(prev).equals(getRoleName(current))) {
            return true;
        }

        if (!getSiteName(prev).equals(getSiteName(current))) {
            return true;
        }

        return false;
    }

    /**
     * Get the role name from the presence.
     * 
     * @param presence
     *            The Presence
     * @return the role name for this presence
     */
    private String getRoleName(Presence presence) {
        Object roleObj = presence.getProperty(SiteConfigInformation.ROLE_NAME);
        return roleObj == null ? "" : roleObj.toString();
    }

    /**
     * Get the site name from the presence.
     * 
     * @param presence
     *            The Presence
     * @return the site name for this presence
     */
    private String getSiteName(Presence presence) {
        Object siteObj = presence.getProperty(SiteConfigInformation.SITE_NAME);
        return siteObj == null ? "" : siteObj.toString();
    }

    /**
     * Get the status message.
     * 
     * @param roleName
     * @param siteName
     * @param user
     * 
     * @return The StringBuilder instance holding the message
     */
    private StringBuilder getMessage(String roleName, String siteName,
            String user) {
        StringBuilder message = new StringBuilder();
        message.append(user);
        message.append(" ").append(roleName).append(" ").append(siteName);
        return message;
    }

    /**
     * No operation for Session Feed View
     */
    @Override
    protected void participantArrived(VenueParticipant participant,
            String description) {
        refreshParticipantList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void participantDeparted(VenueParticipant participant,
            String description) {
        if (otherParticipants.remove(participant.getName()) != null) {
            super.participantDeparted(participant, description);
        }
    }

}
