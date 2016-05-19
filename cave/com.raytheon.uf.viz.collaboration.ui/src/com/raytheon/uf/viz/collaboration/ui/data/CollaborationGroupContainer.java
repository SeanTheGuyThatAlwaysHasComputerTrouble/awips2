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
package com.raytheon.uf.viz.collaboration.ui.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.raytheon.uf.viz.collaboration.comm.provider.connection.CollaborationConnection;
import com.raytheon.uf.viz.collaboration.comm.provider.user.ContactsManager;
import com.raytheon.viz.ui.widgets.IFilterInput;

/**
 * Container for collaboration information window. Includes current user,
 * sessions and contacts
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 23, 2012            mnash     Initial creation
 * Dec 20, 2013 2563       bclement  added items from server roster not in groups
 * Jan 24, 2014 2701       bclement  removed local groups, added shared groups
 * Oct 08, 2014 3705       bclement  added public room group
 * Jun 16, 2015 4401       bkowal    updated to implement {@link IFilterInput} to provide
 *                                   data to filter on.
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public class CollaborationGroupContainer implements IFilterInput {

    private final SessionGroupContainer sessionGroup = new SessionGroupContainer();

    private final PublicRoomContainer publicRoomGroup = new PublicRoomContainer();

    public CollaborationGroupContainer() {
    }

    /**
     * Get objects for UI items including current user, sessions and contacts
     * 
     * @return
     */
    public List<Object> getObjects() {
        CollaborationConnection connection = CollaborationConnection
                .getConnection();
        if (connection == null) {
            return Collections.emptyList();
        }
        List<Object> result = new ArrayList<Object>();
        result.add(connection.getUser());
        result.add(sessionGroup);
        result.add(publicRoomGroup);
        ContactsManager contactsManager = connection.getContactsManager();
        result.addAll(contactsManager.getSharedGroups());
        result.addAll(contactsManager.getGroups());
        result.addAll(contactsManager.getNonGroupedContacts());
        return result;
    }

    /**
     * Get container for session UI objects
     * 
     * @return
     */
    public SessionGroupContainer getSessionGroup() {
        return sessionGroup;
    }

    /**
     * @return the publicRoomGroup
     */
    public PublicRoomContainer getPublicRoomGroup() {
        return publicRoomGroup;
    }

}
