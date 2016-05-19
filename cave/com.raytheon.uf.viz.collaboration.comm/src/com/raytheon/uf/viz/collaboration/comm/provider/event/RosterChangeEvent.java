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
package com.raytheon.uf.viz.collaboration.comm.provider.event;

import org.jivesoftware.smack.packet.Presence;

import com.raytheon.uf.viz.collaboration.comm.identity.event.IRosterChangeEvent;
import com.raytheon.uf.viz.collaboration.comm.identity.event.RosterChangeType;
import com.raytheon.uf.viz.collaboration.comm.provider.user.UserId;

/**
 * Event posted when a roster entry needs to be updated
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 11, 2012            jkorman     Initial creation
 * Feb 24, 2014    2632    mpduff      Added getPresence, changed getItem to getEntry.
 * Apr 24, 2014    3070    bclement    getEntry() returns UserId
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.0
 */

public class RosterChangeEvent implements IRosterChangeEvent {

    private final RosterChangeType type;

    private final UserId entry;

    /** The presence object */
    private Presence presence;

    /**
     * Create an instance of this event using the given type and entry.
     * 
     * @param type
     *            The event type.
     * @param entry
     *            The changed entry.
     */
    public RosterChangeEvent(RosterChangeType type, UserId entry) {
        this.type = type;
        this.entry = entry;
    }

    /**
     * Create an instance of this event using the given type, entry, and
     * presence.
     * 
     * @param type
     *            The event type.
     * @param entry
     *            The changed entry.
     * @param presence
     *            The presence object
     */
    public RosterChangeEvent(RosterChangeType type, UserId entry,
            Presence presence) {
        this.type = type;
        this.entry = entry;
        this.presence = presence;

    }

    /**
     * Get the event type.
     * 
     * @return The event type.
     * @see com.raytheon.uf.viz.collaboration.comm.identity.event.IRosterChangeEvent#getType()
     */
    @Override
    public RosterChangeType getType() {
        return type;
    }

    /**
     * Get the changed entry
     * 
     * @return The changed entry.
     * @see com.raytheon.uf.viz.collaboration.comm.identity.event.IRosterChangeEvent#getEntry()
     */
    @Override
    public UserId getEntry() {
        return entry;
    }

    /**
     * @return the presence
     */
    @Override
    public Presence getPresence() {
        return presence;
    }

    /**
     * @param presence
     *            the presence to set
     */
    public void setPresence(Presence presence) {
        this.presence = presence;
    }

}
