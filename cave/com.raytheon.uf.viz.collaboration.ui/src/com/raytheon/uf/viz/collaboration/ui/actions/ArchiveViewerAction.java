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
package com.raytheon.uf.viz.collaboration.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.collaboration.comm.identity.IVenueSession;
import com.raytheon.uf.viz.collaboration.comm.provider.connection.CollaborationConnection;
import com.raytheon.uf.viz.collaboration.comm.provider.user.UserId;
import com.raytheon.uf.viz.collaboration.ui.Activator;
import com.raytheon.uf.viz.collaboration.ui.session.SessionMsgArchive;
import com.raytheon.uf.viz.core.icon.IconUtil;
import com.raytheon.viz.ui.views.CaveWorkbenchPageManager;

/**
 * Open the Log View
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul  5, 2012            bsteffen    Initial creation
 * Jan 28, 2014 2698       bclement    changed sessionName to sessionId
 * Mar 31, 2014 2937       bgonzale    Use session name for log retrieval.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class ArchiveViewerAction extends Action {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ArchiveViewerAction.class);

    private final String sessionId;

    public ArchiveViewerAction() {
        super("View Log...", IconUtil.getImageDescriptor(Activator.getDefault()
                .getBundle(), "log.gif"));
        sessionId = null;
        setEnabled(CollaborationConnection.getConnection() != null);
    }

    public ArchiveViewerAction(UserId user) {
        super("View Log...", IconUtil.getImageDescriptor(Activator.getDefault()
                .getBundle(), "log.gif"));
        sessionId = user.getName();
    }

    public ArchiveViewerAction(IVenueSession session) {
        super("View Log...", IconUtil.getImageDescriptor(Activator.getDefault()
                .getBundle(), "log.gif"));
        sessionId = session.getVenue().getName();
    }

    @Override
    public void run() {
        UserId user = CollaborationConnection.getConnection().getUser();
        String logDir = SessionMsgArchive.getLogFilePath(user.getHost(),
                user.getName(), sessionId);

        try {
            CaveWorkbenchPageManager
                    .getActiveInstance()
                    .showView(
                            "com.raytheon.uf.viz.collaboration.ui.session.SessionMsgArchiveView",
                            logDir, IWorkbenchPage.VIEW_ACTIVATE);
        } catch (PartInitException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to open Collaboration Log View", e);
        }
    }
}
