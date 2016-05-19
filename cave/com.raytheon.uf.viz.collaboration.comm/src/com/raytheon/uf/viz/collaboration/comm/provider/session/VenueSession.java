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
package com.raytheon.uf.viz.collaboration.comm.provider.session;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.UserStatusListener;
import org.jivesoftware.smackx.packet.DiscoverItems;

import com.google.common.eventbus.EventBus;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.collaboration.comm.Activator;
import com.raytheon.uf.viz.collaboration.comm.identity.CollaborationException;
import com.raytheon.uf.viz.collaboration.comm.identity.IMessage;
import com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession;
import com.raytheon.uf.viz.collaboration.comm.identity.event.ParticipantEventType;
import com.raytheon.uf.viz.collaboration.comm.identity.info.IVenue;
import com.raytheon.uf.viz.collaboration.comm.identity.invite.VenueInvite;
import com.raytheon.uf.viz.collaboration.comm.identity.user.IUser;
import com.raytheon.uf.viz.collaboration.comm.provider.TextMessage;
import com.raytheon.uf.viz.collaboration.comm.provider.Tools;
import com.raytheon.uf.viz.collaboration.comm.provider.connection.CollaborationConnection;
import com.raytheon.uf.viz.collaboration.comm.provider.event.UserNicknameChangedEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.event.VenueParticipantEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.event.VenueUserEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.info.Venue;
import com.raytheon.uf.viz.collaboration.comm.provider.user.IDConverter;
import com.raytheon.uf.viz.collaboration.comm.provider.user.UserId;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueId;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueParticipant;

/**
 * Represents a multi-user chat room
 * 
 * <ul>
 * <li>EventBus subscription events.</li>
 * <ul>
 * <li><strong>IVenueParticipantEvent</strong> : This event is posted when a
 * venue participant enters, leaves a venue, or updates their status in the
 * venue.</li>
 * <li><strong>TextMessage</strong> : Text messages send between users. Meant to
 * be displayed as conversation.</li>
 * <li><strong>CollaborationMessage</strong> : These messages are CAVE to CAVE
 * command messages.</li>
 * </ul>
 * </ul>
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 24, 2012            jkorman     Initial creation
 * Apr 17, 2012            njensen      Major refactor
 * Dec  6, 2013 2561       bclement    removed ECF
 * Dec 18, 2013 2562       bclement    moved data to packet extension
 * Dec 19, 2013 2563       bclement    status listeners now send all events to bus
 * Jan 07, 2013 2563       bclement    use getServiceName instead of getHost when creating room id
 * Jan 08, 2014 2563       bclement    fixed service name in user IDs from chat history
 * Jan 28, 2014 2698       bclement    removed venue info, new rooms are now invite-only
 *                                     improved error handling for when room already exists
 * Jan 30, 2014 2698       bclement    changed UserId to VenueParticipant, added handle
 * Feb 13, 2014 2751       bclement    VenueParticipant refactor
 * Feb 18, 2014 2751       bclement    Fixed history message 'from' type
 * Feb 18, 2014 2751       bclement    log privilege changes instead of spamming chat window
 * Feb 24, 2014 2751       bclement    added isRoomOwner()
 * Mar 05, 2014 2798       mpduff      Don't handle Presence, get from MUC instead..
 * Mar 06, 2014 2751       bclement    added isAdmin()
 * Mar 07, 2014 2848       bclement    added getVenueName() and hasOtherParticipants()
 *                                      moved muc close logic to closeMuc()
 *                                      handle is now set in constructor
 * Apr 11, 2014 2903       bclement    made constructor public b/c connection code moved packages
 * Apr 16, 2014 3020       bclement    added check for invited rooms in roomExistsOnServer()
 * Apr 21, 2014 2822       bclement    added hasMultipleHandles()
 * Apr 22, 2014 2903       bclement    added connection test to close method
 * Apr 23, 2014 2822       bclement    added formatInviteAddress()
 * Apr 29, 2014 3061       bclement    moved invite payload to shared display session
 * May 09, 2014 3107       bclement    removed catch from isRoomOwner() so callers know about errors
 * Jun 16, 2014 3288       bclement    changed String venueName to VenueId venueId, added createVenueId()
 * Oct 08, 2014 3705       bclement    added getVenueId()
 * Jan 12, 2015 3709       bclement    fixed rare invite bug
 * 
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.0
 * @see com.raytheon.uf.viz.collaboration.comm.identity.event.IVenueParticipantEvent
 * @see com.raytheon.uf.viz.collaboration.comm.provider.TextMessage
 */

public class VenueSession extends BaseSession implements IVenueSession {

    private static final IUFStatusHandler log = UFStatus
            .getHandler(VenueSession.class);

    private static final String SEND_TXT = "[[TEXT]]";

    public static final String SEND_HISTORY = "[[HISTORY]]";

    protected MultiUserChat muc = null;

    private PacketListener intListener = null;

    private PacketListener participantListener = null;

    private String handle;

    private Venue venue;

    private volatile boolean admin = false;

    private VenueId venueId;

    private volatile boolean otherParticipants = false;

    /**
     * 
     * @param container
     * @param eventBus
     */
    public VenueSession(EventBus externalBus, CollaborationConnection manager,
            VenueId venueId, String handle, String sessionId) {
        super(externalBus, manager, sessionId);
        this.venueId = venueId;
        this.handle = handle;
    }

    /**
     * 
     * @param container
     * @param eventBus
     */
    public VenueSession(EventBus externalBus, CollaborationConnection manager,
            CreateSessionData data) {
        super(externalBus, manager);
        this.venueId = data.getVenueId();
        this.handle = data.getHandle();
    }

    /**
     * Close this session. Closing clears all listeners and disposes of the
     * container. No errors for attempting to close an already closed session.
     * 
     * @see com.raytheon.uf.viz.collaboration.comm.identity.ISession#close()
     */
    @Override
    public void close() {
        closeMuc();
        super.close();
    }

    private void closeMuc() {
        if (muc == null) {
            return;
        }
        if (intListener != null) {
            muc.removeMessageListener(intListener);
            intListener = null;
        }
        if (participantListener != null) {
            muc.removeParticipantListener(participantListener);
            participantListener = null;
        }
        CollaborationConnection conn = getConnection();
        if (conn != null && conn.isConnected()) {
            muc.leave();
        }
        muc = null;
    }

    /**
     * Get information about this venue.
     * 
     * @return The information about this venue. May return a null reference if
     *         the venue is not connected.
     */
    @Override
    public IVenue getVenue() {
        return venue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#sendInvitation
     * (com.raytheon.uf.viz.collaboration.comm.provider.user.UserId,
     * com.raytheon.uf.viz.collaboration.comm.identity.invite.VenueInvite)
     */
    @Override
    public void sendInvitation(UserId id, VenueInvite invite)
            throws CollaborationException {
        Message msg = createInviteMessage(id, invite);
        String reason = "";
        if (!StringUtils.isBlank(invite.getSubject())) {
            reason = invite.getSubject();
        } else if (!StringUtils.isBlank(invite.getMessage())) {
            reason = invite.getMessage();
        }
        muc.invite(msg, formatInviteAddress(id), reason);
    }

    /**
     * Create message object for session invitation
     * 
     * @param id
     *            recipient of invite
     * @param invite
     * @return
     */
    protected Message createInviteMessage(UserId id, VenueInvite invite) {
        Message msg = new Message();
        msg.setType(Type.normal);
        /*
         * don't set a body on the message. smack will add a packet extension on
         * it that will have the actual invite. openfire gets confused if there
         * is both a body and an extension and drops the invite on the floor.
         */
        return msg;
    }

    /**
     * format invite address for user
     * 
     * @param id
     * @return
     */
    protected String formatInviteAddress(UserId id) {
        return id.getNormalizedId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#sendInvitation
     * (java.util.List,
     * com.raytheon.uf.viz.collaboration.comm.identity.invite.VenueInvite)
     */
    @Override
    public void sendInvitation(List<UserId> ids, VenueInvite invite)
            throws CollaborationException {
        if (ids != null) {
            for (UserId id : ids) {
                sendInvitation(id, invite);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#sendChatMessage
     * (java.lang.String)
     */
    @Override
    public void sendChatMessage(String message) throws CollaborationException {
        // Assume success
        if ((muc != null) && (message != null)) {
            Activator.getDefault().getNetworkStats()
                    .log(Activator.VENUE, message.length(), 0);
            try {
                muc.sendMessage(message);
            } catch (XMPPException e) {
                throw new CollaborationException("Error sending messge", e);
            }
        }
    }

    /**
     * Set up venue configuration and listeners. Must call
     * {@link VenueSession#connectToRoom()} to actually join room
     * 
     * @param venueName
     * @param handle
     * @throws CollaborationException
     */
    public void configureVenue() throws CollaborationException {
        /*
         * the throws statement is for subclasses. if this method ever actually
         * throws an exception, it needs to call closeMuc() to ensure that we
         * remove the listeners
         */
        CollaborationConnection manager = getSessionManager();
        XMPPConnection conn = manager.getXmppConnection();
        this.muc = new MultiUserChat(conn, venueId.getFQName());
        this.venue = new Venue(conn, muc);
        createListeners();
        setHandle(manager, handle);
    }

    /**
     * Create room and connect to it
     * 
     * @param data
     * @throws CollaborationException
     */
    public void createVenue(CreateSessionData data)
            throws CollaborationException {
        try {
            CollaborationConnection manager = getSessionManager();
            XMPPConnection conn = manager.getXmppConnection();
            String roomId = venueId.getFQName();
            if (roomExistsOnServer(conn, roomId)) {
                throw new CollaborationException("Session name already in use");
            }
            this.muc = new MultiUserChat(conn, roomId);
            createListeners();
            setHandle(manager, data.getHandle());
            muc.create(this.handle);
            muc.sendConfigurationForm(getRoomConfig());
            muc.changeSubject(data.getSubject());
            this.venue = new Venue(conn, muc);
            sendPresence(CollaborationConnection.getConnection().getPresence());
            admin = true;
        } catch (XMPPException e) {
            XMPPError xmppError = e.getXMPPError();
            String msg;
            if (xmppError != null) {
                int code = xmppError.getCode();
                if (code == 409 || code == 407) {
                    // 409: room already exists, can't join due to name conflict
                    // 407: room already exists, can't join since it is private
                    msg = "Session already exists. Pick a different name.";
                } else {
                    msg = xmppError.getCondition();
                }
            } else {
                msg = "Error creating venue " + data.getVenueId();
            }
            closeMuc();
            throw new CollaborationException(msg, e);
        }
    }

    /**
     * Change handle used in room. If handle is blank or null, the account's
     * username is used.
     * 
     * @param manager
     * @param handle
     */
    private void setHandle(CollaborationConnection manager, String handle) {
        if (StringUtils.isBlank(handle)) {
            UserId user = manager.getUser();
            handle = user.getName();
        }
        this.handle = handle;
    }

    /**
     * Get filled out configuration form for room creation
     * 
     * @return
     * @throws CollaborationException
     */
    protected Form getRoomConfig() throws CollaborationException {
        Form form;
        try {
            form = muc.getConfigurationForm();
        } catch (XMPPException e) {
            throw new CollaborationException(
                    "Unable to create room configuration form", e);
        }
        Form submitForm = form.createAnswerForm();
        // Add default answers to the form to submit
        for (Iterator<FormField> fields = form.getFields(); fields.hasNext();) {
            FormField field = fields.next();
            if (!FormField.TYPE_HIDDEN.equals(field.getType())
                    && field.getVariable() != null) {
                // Sets the default value as the answer
                submitForm.setDefaultAnswer(field.getVariable());
            }
        }
        submitForm.setAnswer("muc#roomconfig_roomname", venueId.getName());
        submitForm.setAnswer("muc#roomconfig_roomdesc", venueId.getName());
        submitForm.setAnswer("muc#roomconfig_publicroom", false);
        submitForm.setAnswer("muc#roomconfig_membersonly", true);
        submitForm.setAnswer("muc#roomconfig_allowinvites", true);
        submitForm.setAnswer("muc#roomconfig_whois",
                Arrays.asList("moderators"));
        return submitForm;
    }

    /**
     * @param roomName
     * @return true if room exists on server
     * @throws XMPPException
     */
    public static boolean roomExistsOnServer(String subdomain, String roomName)
            throws XMPPException {
        CollaborationConnection conn = CollaborationConnection.getConnection();
        XMPPConnection xmpp = conn.getXmppConnection();
        String id = new VenueId(subdomain, xmpp.getServiceName(), roomName)
                .getFQName();
        return roomExistsOnServer(conn.getXmppConnection(), id);
    }

    /**
     * @param conn
     * @param roomId
     * @return true if room exists on server
     * @throws XMPPException
     */
    public static boolean roomExistsOnServer(XMPPConnection conn, String roomId)
            throws XMPPException {
        String host = Tools.parseHost(roomId);

        /* check for public rooms */
        ServiceDiscoveryManager serviceDiscoveryManager = new ServiceDiscoveryManager(
                conn);
        DiscoverItems result = serviceDiscoveryManager.discoverItems(host);
        for (Iterator<DiscoverItems.Item> items = result.getItems(); items
                .hasNext();) {
            DiscoverItems.Item item = items.next();
            if (roomId.equals(item.getEntityID())) {
                return true;
            }
        }

        /* check for private rooms that we have access to */
        try {
            MultiUserChat.getRoomInfo(conn, roomId);
            /* getRoomInfo only returns if the room was found */
            return true;
        } catch (XMPPException e) {
            /*
             * getRoomInfo throws a 404 if the room does exist or is private and
             * we don't have access. In either case, we can't say that the room
             * exists
             */
        }
        return false;
    }

    /**
     * register chat room listeners with muc
     */
    private void createListeners() {
        muc.addParticipantStatusListener(new ParticipantStatusListener() {

            @Override
            public void voiceRevoked(String participant) {
                sendParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is no longer allowed to chat.");
            }

            @Override
            public void voiceGranted(String participant) {
                sendParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is now allowed to chat.");
            }

            @Override
            public void ownershipRevoked(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is no longer a room owner.");
            }

            @Override
            public void ownershipGranted(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is now a room owner.");
            }

            @Override
            public void nicknameChanged(String participant, String newNickname) {
                VenueParticipant user = IDConverter.convertFromRoom(muc,
                        participant);
                postEvent(new UserNicknameChangedEvent(user, newNickname));
            }

            @Override
            public void moderatorRevoked(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is no longer a moderator.");
            }

            @Override
            public void moderatorGranted(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is now a moderator.");
            }

            @Override
            public void membershipRevoked(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is no longer a member of the room.");
            }

            @Override
            public void membershipGranted(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is now a member of the room.");
            }

            @Override
            public void left(String participant) {
                sendParticipantEvent(participant,
                        ParticipantEventType.DEPARTED, "has left the room.");
            }

            @Override
            public void kicked(String participant, String actor, String reason) {
                // no period since formatter adds it
                sendParticipantEvent(participant,
                        ParticipantEventType.DEPARTED,
                        formatEjectionString("has been kicked", actor, reason));
            }

            @Override
            public void joined(String participant) {
                sendParticipantEvent(participant, ParticipantEventType.ARRIVED,
                        "has entered the room.");
            }

            @Override
            public void banned(String participant, String actor, String reason) {
                // no period since formatter adds it
                sendParticipantEvent(participant,
                        ParticipantEventType.DEPARTED,
                        formatEjectionString("has been banned", actor, reason));
            }

            @Override
            public void adminRevoked(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is no longer an admin.");
            }

            @Override
            public void adminGranted(String participant) {
                logParticipantEvent(participant, ParticipantEventType.UPDATED,
                        "is now an admin.");
            }

            private void logParticipantEvent(String participant,
                    ParticipantEventType type, String desciption) {
                StringBuilder builder = new StringBuilder();
                builder.append("In session '").append(getVenueName())
                        .append("': ");
                builder.append(participant).append(" ").append(desciption);
                log.debug(builder.toString());
            }

            private void sendParticipantEvent(String participant,
                    ParticipantEventType type, String desciption) {
                if (type.equals(ParticipantEventType.ARRIVED)) {
                    otherParticipants = true;
                } else if (type.equals(ParticipantEventType.DEPARTED)) {
                    otherParticipants = venue.hasOtherParticipants();
                }
                VenueParticipant user = IDConverter.convertFromRoom(muc,
                        participant);
                VenueParticipantEvent event = new VenueParticipantEvent(user,
                        type);
                event.setEventDescription(desciption);

                postEvent(event);
            }

        });
        // presence listener
        muc.addParticipantListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                if (packet instanceof Presence) {
                    Presence p = (Presence) packet;
                    String fromID = p.getFrom();
                    VenueParticipant user = IDConverter.convertFromRoom(muc,
                            fromID);
                    postEvent(new VenueParticipantEvent(user, p,
                            ParticipantEventType.PRESENCE_UPDATED));
                }
            }
        });
        // message listener
        this.muc.addMessageListener(new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                if (packet instanceof Message) {
                    Message m = (Message) packet;
                    Activator.getDefault().getNetworkStats()
                            .log(Activator.VENUE, 0, m.getBody().length());
                    String fromStr = m.getFrom();
                    IUser from;
                    if (IDConverter.isRoomSystemMessage(fromStr)) {
                        postEvent(new VenueUserEvent(m.getBody()));
                    } else {
                        from = IDConverter.convertFromRoom(muc, fromStr);
                        if (accept(m, from)) {
                            distributeMessage(convertMessage(m, from));
                        }
                    }
                }

            }
        });
        // listens for our own status changes
        this.muc.addUserStatusListener(new UserStatusListener() {

            @Override
            public void voiceRevoked() {
                sendUserEvent("Your chat privileges have been revoked.");
            }

            @Override
            public void voiceGranted() {
                sendUserEvent("Your chat privileges have been granted.");
            }

            @Override
            public void ownershipRevoked() {
                logUserEvent("You are no longer an owner of this room.");
                admin = false;
            }

            @Override
            public void ownershipGranted() {
                logUserEvent("You are now an owner of this room.");
                admin = true;
            }

            @Override
            public void moderatorRevoked() {
                logUserEvent("You are no longer a moderator of this room.");
            }

            @Override
            public void moderatorGranted() {
                logUserEvent("You are now the moderator of this room.");
            }

            @Override
            public void membershipRevoked() {
                logUserEvent("You are no longer a member of this room.");
            }

            @Override
            public void membershipGranted() {
                logUserEvent("You are now a member of this room.");
            }

            @Override
            public void kicked(String actor, String reason) {
                // no period since formatter adds it
                sendUserEvent(formatEjectionString("You have had been kicked",
                        actor, reason));
                // TODO disable window?
            }

            @Override
            public void banned(String actor, String reason) {
                // no period since formatter adds it
                sendUserEvent(formatEjectionString("You have been banned",
                        actor, reason));
                // TODO disable window?
            }

            @Override
            public void adminRevoked() {
                logUserEvent("You have had admin privileges revoked.");
                admin = false;
            }

            @Override
            public void adminGranted() {
                logUserEvent("You have had admin privileges granted.");
                admin = true;
            }

            private void sendUserEvent(String message) {
                postEvent(new VenueUserEvent(message));
            }

            private void logUserEvent(String message) {
                StringBuilder builder = new StringBuilder();
                builder.append("In session '").append(getVenueName())
                        .append("': ");
                builder.append(message);
                log.info(builder.toString());
            }

        });
    }

    /**
     * Format reason for being kicked/banned from venue. Actor and reason will
     * be appended to base if not null or empty. Formatter will add period at
     * end of string.
     * 
     * @param base
     * @param actor
     * @param reason
     * @return
     */
    private String formatEjectionString(String base, String actor, String reason) {
        StringBuilder rval = new StringBuilder(base);
        if (!StringUtils.isBlank(actor)) {
            rval.append(" by ").append(actor);
        }
        if (!StringUtils.isBlank(reason)) {
            rval.append(" with reason '").append(reason).append("'");
        } else {
            rval.append(" with no reason given");
        }
        rval.append(".");
        return rval.toString();
    }

    /**
     * Allows users to connect after the fact so that they do not miss any
     * messages coming from the room (after the dialog/view has been
     * instantiated)
     * 
     * @throws CollaborationException
     */
    public void connectToRoom() throws CollaborationException {
        if (this.muc.isJoined()) {
            return;
        }
        try {
            this.muc.join(handle);
            sendPresence(CollaborationConnection.getConnection().getPresence());
            /*
             * can't rely on having received any presence updates yet (used in
             * venue.getParticipantCount()). This code should only happen when
             * the client is joining an existing room, so error on the side of
             * caution and assume that the room isn't empty
             */
            otherParticipants = true;
        } catch (XMPPException e) {
            XMPPError xmppError = e.getXMPPError();
            String msg;
            if (xmppError != null) {
                int code = xmppError.getCode();
                if (code == 409) {
                    // 409: can't join due to handle conflict
                    msg = "Handle '"
                            + handle
                            + "' already in use. Please enter a different handle.";
                } else {
                    msg = xmppError.getCondition();
                }
            } else {
                msg = "Error joining venue " + muc.getRoom();
            }
            throw new CollaborationException(msg, e);
        }
    }

    /**
     * Examine the incoming message to determine if it should be forwarded. The
     * method looks at the from identifier to determine who sent the message.
     * 
     * @param message
     *            A message to accept.
     * @param from
     *            user that the message is from
     * @return Should the message be accepted.
     */
    private boolean accept(Message message, IUser from) {
        if (this.muc == null) {
            // we don't seem to be in a room
            return false;
        }

        String body = message.getBody();

        if (!body.startsWith(SEND_HISTORY) && from.equals(getUserID())) {
            // ignore from ourselves except for history
            return false;
        } else if (body.startsWith(Tools.CMD_PREAMBLE)
                || body.startsWith(Tools.CONFIG_PREAMBLE)) {
            return false;
        }

        return true;
    }

    /**
     * Process message and post event to bus
     * 
     * @param message
     */
    private void distributeMessage(IMessage message) {
        if (message != null) {

            String body = message.getBody();
            if (body != null) {
                if (body.startsWith(SEND_HISTORY)) {
                    String[] vars = body.split("\\|");
                    String timeString = vars[0]
                            .substring(SEND_HISTORY.length());
                    long time = Long.parseLong(timeString);
                    String msgHandle = vars[1];
                    String site = vars[2];
                    // add the SEND_HISTORY tag length, and the timestamp
                    // length, username length, and the site length plus the
                    // three pipe characters
                    String moddedBody = body.substring(SEND_HISTORY.length()
                            + timeString.length() + msgHandle.length()
                            + site.length() + 3);
                    message.setBody(moddedBody);
                    TextMessage msg = new TextMessage(message.getFrom(),
                            message.getBody());
                    msg.setFrom(new VenueParticipant(this.getVenueName(),
                            venueId.getHost(), msgHandle));
                    msg.setTimeStamp(time);
                    msg.setSubject(site);
                    msg.setStatus(SEND_HISTORY);
                    this.postEvent(msg);
                } else {
                    message.setBody(body);
                    TextMessage msg = new TextMessage(message.getTo(),
                            message.getBody());
                    msg.setFrom(message.getFrom());

                    this.postEvent(msg);
                }
            }
        }
    }

    /**
     * Convert from an chat room message to an IMessage instance.
     * 
     * @param msg
     *            The chat room message to convert.
     * @param from
     *            user that the message is from
     * @return The converted message.
     */
    private IMessage convertMessage(Message msg, IUser from) {
        IMessage message = null;

        String body = msg.getBody();
        if (body != null) {
            if (body.startsWith(SEND_TXT)) {
                body = body.substring(SEND_TXT.length());
            }
            message = new TextMessage(null, body);
            message.setFrom(from);
        }
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#sendPresence
     * (org.jivesoftware.smack.packet.Presence)
     */
    @Override
    public void sendPresence(Presence presence) throws CollaborationException {
        presence.setTo(venue.getId());
        XMPPConnection conn = getConnection().getXmppConnection();
        conn.sendPacket(presence);
    }

    /**
     * @return the handle
     */
    public String getHandle() {
        return handle;
    }

    /**
     * @return userid of this account
     */
    public UserId getAccount() {
        return getConnection().getUser();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.provider.session.BaseSession#getUserID
     * ()
     */
    @Override
    public VenueParticipant getUserID() {
        UserId account = getAccount();
        return new VenueParticipant(this.getVenueName(), venueId.getHost(),
                handle, account);
    }

    /**
     * @param p
     * @return true if participant is an owner of the chat room
     * @throws XMPPException
     */
    protected boolean isRoomOwner(VenueParticipant p) throws XMPPException {
        boolean rval = false;
        for (Affiliate aff : muc.getOwners()) {
            if (aff.getNick().equals(p.getHandle())) {
                rval = true;
                break;
            }
        }
        return rval;
    }

    /**
     * the return value is only accurate if the actual userIDs of all
     * participants can be seen by this client
     * 
     * @param user
     * @return true if the user is in the room under multiple handles
     */
    protected boolean hasMultipleHandles(UserId user) {
        int count = 0;
        IVenue v = getVenue();
        for (VenueParticipant p : v.getParticipants()) {
            UserId other = v.getParticipantUserid(p);
            if (other != null && user.isSameUser(other)) {
                count += 1;
            }
        }
        return count > 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#isAdmin()
     */
    @Override
    public boolean isAdmin() {
        return admin;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#getVenueName
     * ()
     */
    @Override
    public String getVenueName() {
        return venueId.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#
     * hasOtherParticipants()
     */
    @Override
    public boolean hasOtherParticipants() {
        return otherParticipants;
    }

    /**
     * Create a venue ID with the default subdomain on the currently connected
     * server
     * 
     * @param venueName
     * @return
     */
    public static VenueId createVenueId(String venueName) {
        CollaborationConnection conn = CollaborationConnection.getConnection();
        XMPPConnection xmpp = conn.getXmppConnection();
        return new VenueId(VenueId.DEFAULT_SUBDOMAIN, xmpp.getServiceName(),
                venueName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession#getVenueId
     * ()
     */
    @Override
    public VenueId getVenueId() {
        return venueId;
    }

}
