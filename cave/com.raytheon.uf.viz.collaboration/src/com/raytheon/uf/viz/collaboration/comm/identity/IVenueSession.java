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

import java.util.Collection;
import java.util.List;

import com.raytheon.uf.viz.collaboration.comm.identity.event.IEventPublisher;
import com.raytheon.uf.viz.collaboration.comm.identity.info.IVenue;
import com.raytheon.uf.viz.collaboration.comm.identity.listener.IInvitation;
import com.raytheon.uf.viz.collaboration.comm.identity.listener.IMessageFilter;
import com.raytheon.uf.viz.collaboration.comm.identity.listener.IMessageListener;
import com.raytheon.uf.viz.collaboration.comm.identity.listener.IVenueParticipantListener;
import com.raytheon.uf.viz.collaboration.comm.identity.user.ParticipantRole;

/**
 *
 * 
 * <ul>
 * <li>EventBus subscription events. Implementors are required to post the following events.</li>
 * <ul>
 * <li><strong>IVenueParticipantEvent</strong> : This event is posted when a venue participant enters, leaves a venue, or
 * updates their status in the venue.</li>
 * <li><strong>TextMessage</strong> : Text messages send between users. Meant to be displayed as conversation.</li>
 * <li><strong>CollaborationMessage</strong> : These messages are CAVE to CAVE command messages.</li>
 * </ul>
 * </ul>
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 5, 2012            jkorman     Initial creation
 *
 * </pre>
 *
 * @author jkorman
 * @version 1.0	
 */

public interface IVenueSession extends ISession, IEventPublisher {

    /**
     * Return this session as an ISharedDisplaySession if it is supported. If
     * the interface is not supported the method must return a null reference.
     * @return 
     */
    ISharedDisplaySession spawnSharedDisplaySession();
    
    /**
     * Returns information about a venue. 
     * @return Information about a venue
     */
    IVenue getVenue();
    
    /**
     * 
     * @param role
     * @return
     */
    boolean hasRole(ParticipantRole role);

    /**
     * Send a Collaboration message.
     * @param message The message to send.
     */
    void sendTextMessage(String message) throws CollaborationException;

    /**
     * Send an invitation from this venue to another user.
     * @param invitation An invitation
     * @return
     */
    int sendInvitation(IInvitation invitation);
    
    /**
     * Send an invitation from this venue to another user.
     * @param room The target venue for this invitation.
     * @param id The target user for this invitation.
     * @param subject The intended subject of the venue conversation.
     * @param body Any text that the user may wish to include.
     * @return
     */
    int sendInvitation(String room, String id, String subject, String body);
    
    /**
     * Send an invitation from this venue to another user.
     * @param room The target venue for this invitation.
     * @param ids A list of target users for this invitation.
     * @param subject The intended subject of the venue conversation.
     * @param body Any text that the user may wish to include.
     * @return
     */
    int sendInvitation(String room, List<String> ids, String subject, String body);
    
}
