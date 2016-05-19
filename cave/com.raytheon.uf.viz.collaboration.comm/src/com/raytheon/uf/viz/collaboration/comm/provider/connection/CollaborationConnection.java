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
package com.raytheon.uf.viz.collaboration.comm.provider.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.bookmark.BookmarkManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider;

import com.google.common.eventbus.EventBus;
import com.google.common.net.HostAndPort;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.xmpp.PacketConstants;
import com.raytheon.uf.common.xmpp.iq.AuthInfo;
import com.raytheon.uf.common.xmpp.iq.AuthInfoProvider;
import com.raytheon.uf.common.xmpp.iq.FeedVenueConfig;
import com.raytheon.uf.common.xmpp.iq.FeedVenueConfigProvider;
import com.raytheon.uf.viz.collaboration.comm.Activator;
import com.raytheon.uf.viz.collaboration.comm.identity.CollaborationException;
import com.raytheon.uf.viz.collaboration.comm.identity.IAccountManager;
import com.raytheon.uf.viz.collaboration.comm.identity.ISession;
import com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession;
import com.raytheon.uf.viz.collaboration.comm.identity.event.IEventPublisher;
import com.raytheon.uf.viz.collaboration.comm.identity.event.IVenueInvitationEvent;
import com.raytheon.uf.viz.collaboration.comm.identity.invite.SharedDisplayVenueInvite;
import com.raytheon.uf.viz.collaboration.comm.packet.SessionPayload;
import com.raytheon.uf.viz.collaboration.comm.packet.SessionPayloadProvider;
import com.raytheon.uf.viz.collaboration.comm.provider.account.AccountManager;
import com.raytheon.uf.viz.collaboration.comm.provider.account.ClientAuthManager;
import com.raytheon.uf.viz.collaboration.comm.provider.event.BookmarkEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.event.BookmarkEvent.Type;
import com.raytheon.uf.viz.collaboration.comm.provider.event.VenueUserEvent;
import com.raytheon.uf.viz.collaboration.comm.provider.session.CreateSessionData;
import com.raytheon.uf.viz.collaboration.comm.provider.session.FeedVenueConfigManager;
import com.raytheon.uf.viz.collaboration.comm.provider.session.PeerToPeerChat;
import com.raytheon.uf.viz.collaboration.comm.provider.session.SharedDisplaySession;
import com.raytheon.uf.viz.collaboration.comm.provider.session.VenueSession;
import com.raytheon.uf.viz.collaboration.comm.provider.user.ContactsManager;
import com.raytheon.uf.viz.collaboration.comm.provider.user.UserId;
import com.raytheon.uf.viz.collaboration.comm.provider.user.UserSearch;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueId;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueParticipant;

/**
 * 
 * <ul>
 * <li>EventBus subscription events.</li>
 * <ul>
 * <li><strong>IVenueInvitationEvent</strong> : This event is posted when the
 * SessionManager receives a venue invitation requesting that the user join some
 * particular collaboration session.</li>
 * <li><strong>IConnectionStatusEvent</strong> : This event is posted when the
 * state of the underlying connection changes, reconnecting, connecting,
 * disconnected, for example.</li>
 * <li><strong>IRosterChangeEvent</strong> : This event is posted when roster
 * changes have occurred.</li>
 * <li><strong>---------------</strong> : ---------------.</li>
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
 * Apr 18, 2012            njensen      Major cleanup
 * Dec  6, 2013 2561       bclement    removed ECF
 * Dec 18, 2013 2562       bclement    added smack compression, fixed invite parsing
 * Dec 19, 2013 2563       bclement    added connection listener, 
 *                                     added better error message on failed connection
 * Jan 07, 2013 2563       bclement    use getServiceName instead of getHost when creating room id
 * Jan 08, 2014 2563       bclement    fixed custom port and service name in user id
 * Jan 15, 2014 2630       bclement    connection data stores status as Mode object
 * Jan 24, 2014 2701       bclement    removed roster manager
 * Jan 28, 2014 2698       bclement    fixed compression default
 *                                     cleaned up createCollaborationVenue, removed getVenueInfo
 * Jan 30, 2014 2698       bclement    changed arguments to create sessions, moved room connection from SessionView
 * Feb  3, 2014 2699       bclement    removed unneeded catch in joinTextOnlyVenue
 * Feb 13, 2014 2751       bclement    better types for venueid and invitor
 * Feb 18, 2014 2793       bclement    improved disconnection notification and handling
 * Feb 24, 2014 2632       mpduff      Fix roster change type for presence change.
 * Feb 28, 2014 2756       bclement    added authManager
 * Mar 07, 2014 2848       bclement    removed join*Venue methods, now only creates venue objects
 *                                      changed session map to a concurrent hash map
 * Apr 07, 2014 2785       mpduff      Changed the order of startup, sets up listeners before actually connecting.
 * Apr 09, 2014 2785       mpduff      Throw error when not connected and the connection should exist.
 * Apr 14, 2014 2903       bclement    moved from session subpackage to connection, removed password from memory, 
 *                                      moved listeners to own classes, reworked connect/register listeners/login order
 * Apr 15, 2014 2822       bclement    added pubsub owner subscriptions provider registration
 * Apr 23, 2014 2822       bclement    added resource name and getCollaborationVersion()
 * May 09, 2014 3107       bclement    added ability for packet timeout to be set via system properties
 * May 19, 2014 3180       bclement    added getJoinedVenueSessions()
 * Jun 16, 2014 3288       bclement    feed venue configuration changes
 * Oct 08, 2014 3705       bclement    added bookmarks, joinTextOnlyVenue() and getPublicRooms()
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.0
 * @see com.raytheon.uf.viz.collaboration.comm.identity.event.IVenueInvitationEvent
 */

public class CollaborationConnection implements IEventPublisher {

    static {
        ProviderManager pm = ProviderManager.getInstance();
        pm.addExtensionProvider(SessionPayload.ELEMENT_NAME,
                PacketConstants.COLLAB_XMLNS, new SessionPayloadProvider());
        pm.addIQProvider(PacketConstants.QUERY_ELEMENT_NAME,
                AuthInfo.AUTH_QUERY_XMLNS, new AuthInfoProvider());
        pm.addIQProvider(PacketConstants.QUERY_ELEMENT_NAME,
                FeedVenueConfig.FEED_QUERY_XMLNS, new FeedVenueConfigProvider());
        /*
         * smack doesn't support some of the OWNER operations such as getting
         * all subscriptions on a node. PubSubOperations creates the request
         * objects for these operations, but the response needs to be parsed.
         * Here we register the existing smack parsers using the OWNER
         * namespace.
         */
        pm.addExtensionProvider(
                PubSubElementType.SUBSCRIPTION.getElementName(),
                PubSubNamespace.OWNER.getXmlns(), new SubscriptionProvider());
        pm.addExtensionProvider(
                PubSubElementType.SUBSCRIPTIONS.getElementName(),
                PubSubNamespace.OWNER.getXmlns(), new SubscriptionsProvider());
    }

    private static final String RESOURCE_BASENAME = "CAVE";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(CollaborationConnection.class);

    private static volatile CollaborationConnection instance = null;

    private final Map<String, ISession> sessions;

    private final UserId user;

    private Presence userPresence;

    private PeerToPeerChat chatInstance = null;

    private IAccountManager accountManager = null;

    private final EventBus eventBus;

    private ContactsManager contactsMgr;

    private final CollaborationConnectionData connectionData;

    private XMPPConnection connection;

    private final ConnectionConfiguration smackConfig;

    private ClientAuthManager authManager;

    private static boolean COMPRESS = true;
    
    static {
        try {
            final String compressionProperty = "collaboration.compression";
            if (System.getProperty(compressionProperty) != null) {
                COMPRESS = Boolean.getBoolean(compressionProperty);
            }
            String customTimeout = System
                    .getProperty("collaboration.packet.timeout");
            if (!StringUtils.isBlank(customTimeout)
                    && StringUtils.isNumeric(customTimeout)) {
                SmackConfiguration.setPacketReplyTimeout(Integer
                        .parseInt(customTimeout));
            }
        } catch (Exception e) {
            // must not have permission to access system properties. ignore and
            // use default.
        }
    }

    /**
     * Returns the currently connected connection or null if it's not connected
     * 
     * @return
     */
    public static CollaborationConnection getConnection() {
        return instance;
    }

    /**
     * Create a {@link CollaborationConnection} given the
     * {@link CollaborationConnectionData}
     * 
     * @param userData
     * @return
     * @throws CollaborationException
     */
    public static CollaborationConnection createConnection(
            CollaborationConnectionData userData) throws CollaborationException {
        synchronized (CollaborationConnection.class) {
            if (instance == null) {
                instance = new CollaborationConnection(userData);
            } else {
                throw new CollaborationException("Already connected");
            }
        }

        return getConnection();
    }

    private CollaborationConnection(CollaborationConnectionData connectionData)
            throws CollaborationException {
        this.connectionData = connectionData;
        this.eventBus = new EventBus();
        this.sessions = new ConcurrentHashMap<String, ISession>();
        this.smackConfig = createSmackConfiguration(connectionData);
        /* connects to XMPP server, but doesn't log in */
        this.connection = createXmppConnection(smackConfig);
        /* create managers and listeners before login */
        this.accountManager = new AccountManager(this);
        this.user = new UserId(connectionData.getUserName(),
                connection.getServiceName());
        this.chatInstance = initPeerToPeerSession();
        this.contactsMgr = new ContactsManager(this, this.getXmppConnection());
        registerEventHandler(this.contactsMgr);
        MultiUserChat.addInvitationListener(connection,
                new SessionInviteListener(this));
        PeerToPeerCommHelper helper = new PeerToPeerCommHelper(this);
        this.connection.addPacketListener(helper, new PacketTypeFilter(
                Message.class));
        this.connection.addConnectionListener(new XmppConnectionListener(this));
        /* login called externally */
    }

    /**
     * Create smack xmpp configuration object from collaboration connection
     * options
     * 
     * @param connectionData
     * @return
     */
    private static ConnectionConfiguration createSmackConfiguration(
            CollaborationConnectionData connectionData) {
        ConnectionConfiguration rval;
        HostAndPort hnp = HostAndPort.fromString(connectionData.getServer());
        if (hnp.hasPort()) {
            rval = new ConnectionConfiguration(hnp.getHostText(), hnp.getPort());
        } else {
            rval = new ConnectionConfiguration(hnp.getHostText());
        }

        rval.setCompressionEnabled(COMPRESS);
        rval.setReconnectionAllowed(false);
        return rval;
    }

    /**
     * Create xmpp connection object and connect. This method does not login
     * 
     * @param config
     * @return
     * @throws CollaborationException
     */
    private static XMPPConnection createXmppConnection(
            ConnectionConfiguration config) throws CollaborationException {
        XMPPConnection rval = new XMPPConnection(config);
        try {
            rval.connect();
        } catch (XMPPException e) {
            throw new CollaborationException(
                    "Problem establishing connection to XMPP server", e);
        }
        return rval;
    }

    /**
     * Login to the XMPP server. This needs to be called after the
     * CollaborationConnection has been created and initialized.
     * 
     * @param password
     * @throws CollaborationException
     */
    public void login(String password) throws CollaborationException {
        loginInternal(connectionData.getUserName(), password);
        /* auth needs to be logged in to register public key */
        authManager = new ClientAuthManager(getXmppConnection());
    }

    /**
     * login to XMPP server
     * 
     * @param username
     * @param password
     * @throws CollaborationException
     */
    private void loginInternal(String username, String password)
            throws CollaborationException {
        try {
            connection.login(username, password, getResourceName());
        } catch (XMPPException e) {
            close();
            // get a nice reason for the user
            String msg;
            XMPPError xmppErr = e.getXMPPError();
            if (xmppErr != null) {
                switch (xmppErr.getCode()) {
                case 401:
                    msg = "Bad username or password";
                    break;
                case 403:
                    msg = "User not allowed to connect to server";
                    break;
                case 409:
                    msg = "User account already in use by another client";
                    break;
                default:
                    msg = e.getLocalizedMessage();
                }
            } else {
                msg = e.getLocalizedMessage();
            }
            throw new CollaborationException("Login failed: " + msg, e);
        } finally {
            /* remove password from smack */
            SmackConfigScrubber.scrubConfig(smackConfig);
        }
    }

    /**
     * get name used for CAVE collaboration XMPP resource
     * 
     * @return
     */
    public static String getResourceName() {
        String rval = RESOURCE_BASENAME;
        String version = getCollaborationVersion();
        if (version != null) {
            rval += "-" + version;
        }
        return rval;
    }

    /**
     * @see Activator#getBundleVersion()
     * @return
     */
    public static String getCollaborationVersion() {
        return Activator.getBundleVersion();
    }

    /**
     * @return login data used to create this connection
     */
    public CollaborationConnectionData getConnectionData() {
        return connectionData;
    }

    /**
     * @return
     */
    public UserId getUser() {
        return user;
    }

    /**
     * 
     * @return
     */
    public Presence getPresence() {
        return userPresence;
    }

    /**
     * 
     * @param presence
     */
    public void setPresence(Presence presence) {
        userPresence = presence;
    }

    /**
     * Get the account manager for this connection.
     * 
     * @return The account manager for this connection.
     */
    public IAccountManager getAccountManager() {
        return accountManager;
    }

    /**
     * Is this client currently connected to the XMPP server?
     * 
     * @return
     */
    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    /**
     * close any open sessions and disconnect from XMPP server
     */
    public void close() {
        // Close any created sessions.
        for (Entry<String, ISession> entry : sessions.entrySet()) {
            entry.getValue().close();
        }
        sessions.clear();
        chatInstance = null;
        if (connection != null) {
            // Get rid of the account and roster managers
            if (connection.isConnected()) {
                connection.disconnect();
            }
            connection = null;
        }
        FeedVenueConfigManager.reset();
        PeerToPeerCommHelper.reset();
        synchronized (CollaborationConnection.class) {
            if (this == instance) {
                instance = null;
            }
        }
    }

    /**
     * Get the PeerToPeerChat session instance.
     * 
     * @return
     * @throws CollaborationException
     */
    public ISession getPeerToPeerSession() throws CollaborationException {
        return chatInstance;
    }

    /**
     * Create and initialize peer to peer chat session object.
     * 
     * @return
     * @throws CollaborationException
     */
    private PeerToPeerChat initPeerToPeerSession()
            throws CollaborationException {
        PeerToPeerChat rval = new PeerToPeerChat(eventBus, this);
        sessions.put(rval.getSessionId(), rval);
        postEvent(rval);
        return rval;
    }

    /**
     * Create shared display venue object. This does not create the venue on the
     * server. The session should be unregistered when no longer active using
     * {@link CollaborationConnection#removeSession(ISession)}
     * 
     * @param invitation
     * @param handle
     * @return
     */
    public SharedDisplaySession createCollaborationVenue(
            IVenueInvitationEvent invitation, String handle) {
        SharedDisplayVenueInvite sdvInvite = (SharedDisplayVenueInvite) invitation
                .getInvite();
        String sessionId = invitation.getInvite().getSessionId();
        VenueId venueId = invitation.getRoomId();
        SharedDisplaySession rval = new SharedDisplaySession(eventBus, this,
                venueId, handle, sessionId);
        setupCollaborationVenue(rval, sdvInvite.getSessionLeader(),
                sdvInvite.getDataProvider());
        return rval;
    }

    /**
     * Configure shared display venue object and register with session map
     * 
     * @param session
     * @param leader
     * @param provider
     */
    private void setupCollaborationVenue(SharedDisplaySession session,
            VenueParticipant leader, VenueParticipant provider) {
        session.setCurrentSessionLeader(leader);
        session.setCurrentDataProvider(provider);
        sessions.put(session.getSessionId(), session);
    }

    /**
     * Create shared display venue object. This does not create the venue on the
     * server. The session should be unregistered when no longer active using
     * {@link CollaborationConnection#removeSession(ISession)}
     * 
     * @param data
     * @return
     */
    public SharedDisplaySession createCollaborationVenue(CreateSessionData data) {
        SharedDisplaySession session = new SharedDisplaySession(eventBus, this,
                data);
        VenueParticipant leader = session.getUserID();
        setupCollaborationVenue(session, leader, leader);
        return session;
    }

    /**
     * Create text only venue object. This does not create the venue on the
     * server. The session should be unregistered when no longer active using
     * {@link CollaborationConnection#removeSession(ISession)}
     * 
     * @param venueId
     * @param handle
     * @return
     */
    public VenueSession createTextOnlyVenue(VenueId venueId, String handle) {
        return createTextOnlyVenue(new CreateSessionData(venueId, handle));
    }

    /**
     * Create text only venue object. This does not create the venue on the
     * server. The session should be unregistered when no longer active using
     * {@link CollaborationConnection#removeSession(ISession)}
     * 
     * @param data
     * @return
     */
    public VenueSession createTextOnlyVenue(CreateSessionData data) {
        VenueSession session = new VenueSession(eventBus, this, data);
        sessions.put(session.getSessionId(), session);
        return session;
    }

    /**
     * Join existing chat room with provided handle
     * 
     * @param venueId
     * @param handle
     * @return
     * @throws CollaborationException
     */
    public VenueSession joinTextOnlyVenue(VenueId venueId, String handle)
            throws CollaborationException {
        VenueSession session = createTextOnlyVenue(venueId, handle);
        try {
            session.configureVenue();
            postEvent(session);
            return session;
        } catch (CollaborationException e) {
            removeSession(session);
            throw e;
        }
    }

    /**
     * Check if venue exists on server
     * 
     * @param subdomain
     * @param venueName
     * @return false on error
     */
    public boolean venueExistsOnServer(String subdomain, String venueName) {
        try {
            return VenueSession.roomExistsOnServer(subdomain, venueName);
        } catch (XMPPException e) {
            statusHandler.error("Unable to check for room on server", e);
            return false;
        }
    }

    /**
     * unregister session with connection and notify UI elements
     * 
     * @param session
     */
    public void removeSession(ISession session) {
        sessions.remove(session.getSessionId());
        postEvent(session);
    }

    /**
     * Get registered session
     * 
     * @param sessionId
     * @return
     */
    public ISession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Register an event handler with this
     * 
     * @see com.raytheon.uf.viz.collaboration.comm.identity.event.IEventPublisher#registerEventHandler(java.lang.Object)
     */
    @Override
    public void registerEventHandler(Object handler) {
        eventBus.register(handler);
    }

    /**
     * @see com.raytheon.uf.viz.collaboration.comm.identity.event.IEventPublisher#unregisterEventHandler(java.lang.Object)
     */
    @Override
    public void unregisterEventHandler(Object handler) {
        eventBus.unregister(handler);
    }

    @Override
    public void postEvent(Object event) {
        if (event != null) {
            eventBus.post(event);
        }
    }

    public ContactsManager getContactsManager() {
        return contactsMgr;
    }

    public Collection<ISession> getSessions() {
        return sessions.values();
    }

    /**
     * @return list of public chat rooms on server
     */
    public Collection<VenueId> getPublicRooms() {
        XMPPConnection conn = getXmppConnection();
        String mucService = VenueId.DEFAULT_SUBDOMAIN + "."
                + conn.getServiceName();
        Collection<HostedRoom> results;
        try {
            results = MultiUserChat.getHostedRooms(conn, mucService);
        } catch (XMPPException e) {
            statusHandler.error("Problem getting public room list from server",
                    e);
            results = Collections.emptyList();
        }
        Collection<VenueId> rval = new TreeSet<>();
        for (HostedRoom room : results) {
            rval.add(new VenueId(room.getJid()));
        }

        return rval;
    }

    /**
     * @return all IVenueSessions that this user is a participant in
     */
    public Collection<IVenueSession> getJoinedVenueSessions() {
        Collection<ISession> allSessions = getSessions();
        ArrayList<IVenueSession> rval = new ArrayList<IVenueSession>(
                allSessions.size());
        for (ISession session : allSessions) {
            if (session != null && session instanceof IVenueSession) {
                rval.add((IVenueSession) session);
            }
        }
        return rval;
    }

    /**
     * @return Smack connection object
     */
    public XMPPConnection getXmppConnection() {
        return connection;
    }

    /**
     * Construct a new UserSearch object
     * 
     * @return
     */
    public UserSearch createSearch() {
        return new UserSearch(connection);
    }

    /**
     * Post a system message to all venue windows this client has. This is for
     * local system messages, it does not go out to the server.
     * 
     * @param message
     */
    public void postSystemMessageToVenues(String message) {
        for (Entry<String, ISession> entry : sessions.entrySet()) {
            entry.getValue().postEvent(new VenueUserEvent(message));
        }
    }

    /**
     * @return the authManager
     */
    public ClientAuthManager getAuthManager() {
        return authManager;
    }

    /**
     * @return the statusHandler
     */
    protected static IUFStatusHandler getStatusHandler() {
        return statusHandler;
    }

    /**
     * @return collection of chat rooms bookmarked by user
     * @throws CollaborationException
     */
    public Collection<VenueId> getBookmarkedRooms()
            throws CollaborationException {
        Collection<BookmarkedConference> bookmarkedConferences;
        try {
            BookmarkManager bmkManager = BookmarkManager
                    .getBookmarkManager(getXmppConnection());
            bookmarkedConferences = bmkManager.getBookmarkedConferences();
        } catch (XMPPException e) {
            throw new CollaborationException(
                    "Unable to get list of bookmarked rooms from server", e);
        }
        List<VenueId> rval = new ArrayList<>(bookmarkedConferences.size());
        for (BookmarkedConference conf : bookmarkedConferences) {
            rval.add(new VenueId(conf.getJid()));
        }
        return rval;
    }

    /**
     * Bookmark room on server
     * 
     * @param room
     * @throws CollaborationException
     */
    public void bookmarkRoom(VenueId room) throws CollaborationException {
        try {
            BookmarkManager bmkManager = BookmarkManager
                    .getBookmarkManager(getXmppConnection());
            bmkManager.addBookmarkedConference(room.getName(),
                    room.getFQName(), false, null, null);
            postEvent(new BookmarkEvent(Type.ADDED, room));
        } catch (XMPPException e) {
            throw new CollaborationException("Unable to bookmark room: "
                    + room.getFQName(), e);
        }
    }

    /**
     * Remove bookmark for room from server
     * 
     * @param room
     * @throws CollaborationException
     */
    public void removeBookmark(VenueId room) throws CollaborationException {
        try {
            BookmarkManager bmkManager = BookmarkManager
                    .getBookmarkManager(getXmppConnection());
            bmkManager.removeBookmarkedConference(room.getFQName());
            postEvent(new BookmarkEvent(Type.REMOVED, room));
        } catch (XMPPException e) {
            throw new CollaborationException(
                    "Unable to remove bookmark for room: " + room.getFQName(),
                    e);
        }
    }

    /**
     * Find session this user is joined to by venue id
     * 
     * @param room
     * @return null if not joined to room
     */
    public IVenueSession getJoinedVenueSession(VenueId room) {
        IVenueSession rval = null;
        Collection<IVenueSession> joinedRooms = getJoinedVenueSessions();
        for (IVenueSession session : joinedRooms) {
            VenueId vid = session.getVenueId();
            if (room.equals(vid)) {
                rval = session;
                break;
            }
        }
        return rval;
    }
}
