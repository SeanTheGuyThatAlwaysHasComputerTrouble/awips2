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
package com.raytheon.uf.viz.collaboration.comm.identity;

import java.util.List;

import org.jivesoftware.smack.packet.Presence;

import com.raytheon.uf.viz.collaboration.comm.identity.info.IVenue;
import com.raytheon.uf.viz.collaboration.comm.identity.invite.VenueInvite;
import com.raytheon.uf.viz.collaboration.comm.provider.user.UserId;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueId;
import com.raytheon.uf.viz.collaboration.comm.provider.user.VenueParticipant;

/**
 * Interface for multi user sessions
 * 
 * <ul>
 * <li>EventBus subscription events. Implementors are required to post the
 * following events.</li>
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
 * Mar 05, 2012            jkorman     Initial creation
 * Jan 30, 2014 2698       bclement    changed UserId to VenueParticipant
 * Mar 06, 2014 2751       bclement    added isAdmin()
 * Mar 07, 2014 2848       bclement    added getVenueName() and hasOtherParticipants()
 * Oct 08, 2014 3705       bclement    added getVenueId()
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.0
 */

public interface IVenueSession extends ISession {

    /**
     * Returns information about a venue.
     * 
     * @return Information about a venue
     */
    public IVenue getVenue();

    /**
     * @return name of chat room serving as venue
     */
    public String getVenueName();

    /**
     * @return qualified id of venue on server
     */
    public VenueId getVenueId();

    /**
     * Send a chat message.
     * 
     * @param message
     *            The message to send.
     */
    public void sendChatMessage(String message) throws CollaborationException;

    /**
     * Send an invitation from this venue to another user.
     * 
     * @param id
     *            The target user for this invitation.
     * @param invite
     * @throws CollaborationException
     */
    public void sendInvitation(UserId id, VenueInvite invite)
            throws CollaborationException;

    /**
     * Send an invitation from this venue to a list of users.
     * 
     * @param ids
     *            A list of target users for this invitation.
     * @param invite
     * @throws CollaborationException
     */
    public void sendInvitation(List<UserId> ids, VenueInvite invite)
            throws CollaborationException;

    /**
     * Send presence to a venue.
     * 
     * @param presence
     */
    public void sendPresence(Presence presence) throws CollaborationException;

    /**
     * @return participant id of current user
     */
    public VenueParticipant getUserID();

    /**
     * @return true if current user has admin privileges in venue
     */
    public boolean isAdmin();

    /**
     * @return false if current user is the only participant in the session
     */
    public boolean hasOtherParticipants();
}
