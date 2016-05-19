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
package com.raytheon.uf.viz.remote.graphics.events.rendering;

import com.raytheon.uf.viz.remote.graphics.events.AbstractRemoteGraphicsEvent;

/**
 * Abstract class for non-object based rendering events
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 1, 2012            mschenke     Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public abstract class AbstractRemoteGraphicsRenderEvent extends
        AbstractRemoteGraphicsEvent implements IRenderEvent {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.remote.graphics.events.IRenderEvent#createDiffObject
     * (com.raytheon.uf.viz.remote.graphics.events.IRenderEvent)
     */
    @Override
    public IRenderEvent createDiffObject(IRenderEvent event) {
        return event;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() {
        try {
            AbstractRemoteGraphicsRenderEvent newInstance = getClass()
                    .newInstance();
            newInstance.applyDiffObject(this);
            newInstance.setDisplayId(getDisplayId());
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException("Error cloning render event", e);
        }
    }

    @Override
    public abstract boolean equals(Object obj);

    protected static boolean equals(Object one, Object two) {
        if (one == two) {
            return true;
        } else if (one != null && one.equals(two)) {
            return true;
        }
        return false;
    }
}
