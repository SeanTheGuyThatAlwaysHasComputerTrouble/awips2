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
package com.raytheon.uf.viz.collaboration.comm.identity.event;

import com.raytheon.uf.viz.collaboration.comm.provider.TextMessage;

/**
 * Event indicating that a new message has been received
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 27, 2012            jkorman     Initial creation
 * Jun 20, 2014 3281       bclement    added hasError() getError()
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.0
 */

public interface ITextMessageEvent {
    
    /**
     * @return the message
     */
    public TextMessage getMessage();

    /**
     * @return true if error is set
     */
    public boolean hasError();

    /**
     * @return error message
     */
    public String getError();
}
