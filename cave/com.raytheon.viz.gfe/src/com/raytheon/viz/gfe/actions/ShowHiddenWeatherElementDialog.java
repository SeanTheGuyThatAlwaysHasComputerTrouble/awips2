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
package com.raytheon.viz.gfe.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.viz.gfe.core.DataManager;
import com.raytheon.viz.gfe.dialogs.HiddenWeatherElementDialog;

/**
 * TODO Add Description ShowHiddenWeatherElementDialog.java Feb 27, 2008
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 	Feb 27, 2008					Eric Babin Initial Creation
 * 
 * </pre>
 * 
 * @author ebabin
 * @version 1.0
 */

public class ShowHiddenWeatherElementDialog extends AbstractHandler {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
     * .ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        DataManager dataManager = DataManager.getCurrentInstance();

        if (dataManager == null) {
            return null;
        }

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        if (dataManager.getParmManager().getUndisplayedParms().length > 0) {
            HiddenWeatherElementDialog dialog = new HiddenWeatherElementDialog(
                    shell, dataManager);
            dialog.setBlockOnOpen(true);
            dialog.open();

        } else {
            MessageDialog.openInformation(null, "No Hidden Weather Elements",
                    "No Hidden Weather Elements found.");
        }

        return null;
    }
}
