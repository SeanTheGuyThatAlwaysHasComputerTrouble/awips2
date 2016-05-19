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
package com.raytheon.uf.viz.collaboration.comm.provider.user;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;

/**
 * Utility to parse id strings
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 28, 2012            jkorman     Initial creation
 * Dec  6, 2013 2561       bclement    removed ECF
 * Jan 30, 2014 2698       bclement    reworked convertFromRoom for venue participants
 * Feb  3, 2014 2699       bclement    fixed room id parsing when handle has special characters
 * Feb 13, 2014 2751       bclement    VenueParticipant refactor
 * Jan 13, 2015 3709       bclement    added convertFromRoom that doesn't reference MUC
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.0
 */

public class IDConverter {

    private static final String CONF_ID = "conference.";

    public static UserId convertFrom(String id) {
        String name = StringUtils.parseName(id);
        String host = StringUtils.parseServer(id);
        String rsc = StringUtils.parseResource(id);
        UserId uid = new UserId(name, host, rsc);
        return uid;
    }

    public static UserId convertFrom(RosterEntry entry) {
        UserId rval = convertFrom(entry.getUser());
        rval.setAlias(entry.getName());
        return rval;
    }

    /**
     * Parse userId from room id string "room@host/handle". Name field on
     * returned id may be null.
     * 
     * @param room
     * @param id
     * @return
     */
    public static VenueParticipant convertFromRoom(MultiUserChat room, String id) {
        VenueParticipant rval = convertFromRoom(id);
        Occupant occupant;
        if (room != null && (occupant = room.getOccupant(id)) != null) {
            if (occupant.getJid() != null) {
                // get actual user name
                rval.setUserid(convertFrom(occupant.getJid()));
            }
        }
        return rval;
    }

    /**
     * Parse userId from room id string "room@host/handle".
     * 
     * @param id
     * @return
     */
    public static VenueParticipant convertFromRoom(String id) {
        String handle = StringUtils.parseResource(id);
        if (handle == null || handle.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room participant ids must have handle in resource");
        }
        String cleanId = id.substring(0, id.length() - handle.length());
        String host = StringUtils.parseServer(cleanId);
        String roomName = StringUtils.parseName(id);
        VenueParticipant rval = new VenueParticipant(roomName, host, handle);
        return rval;
    }

    public static String normalizeHostname(String hostname) {
        if (hostname.startsWith(CONF_ID)) {
            return hostname.substring(CONF_ID.length());
        }
        return hostname;
    }

    public static boolean isFromRoom(String id) {
        String host = StringUtils.parseServer(id);
        return host.startsWith(CONF_ID);
    }

    public static boolean isRoomSystemMessage(String id) {
        String handle = StringUtils.parseResource(id);
        // system messages look like participant IDs, just without a handle
        return isFromRoom(id)
                && org.apache.commons.lang.StringUtils.isBlank(handle);
    }

}
