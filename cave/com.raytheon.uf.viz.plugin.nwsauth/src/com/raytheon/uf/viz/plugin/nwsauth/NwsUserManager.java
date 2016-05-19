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
package com.raytheon.uf.viz.plugin.nwsauth;

import java.util.List;

import com.raytheon.uf.common.auth.user.IPermission;
import com.raytheon.uf.common.auth.user.IRole;
import com.raytheon.uf.viz.core.auth.BasicUserManager;
import com.raytheon.uf.viz.core.requests.INotAuthHandler;

/**
 * Implementation of IUserManager
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 27, 2010            rgeorge     Initial creation
 * Jun 07, 2013   1981     mpduff      Add an IUser field.
 * Oct 06, 2014 3398       bclement    now extends BasicUserManager
 * 
 * </pre>
 * 
 * @author rgeorge
 * @version 1.0
 */

public class NwsUserManager extends BasicUserManager {

    private final NwsNotAuthHandler notAuthHandler = new NwsNotAuthHandler();

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.auth.IUserManager#getNotAuthHandler()
     */
    @Override
    public INotAuthHandler getNotAuthHandler() {
        return notAuthHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IPermission> getPermissions(String application) {
        // TODO: Should this pass through to EDEX to get this stuff?
        return NwsRoleDataManager.getInstance().getPermissions(application);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IRole> getRoles(String application) {
        // TODO: Should this pass through to EDEX to get this stuff?
        return NwsRoleDataManager.getInstance().getRoles(application);
    }

}
