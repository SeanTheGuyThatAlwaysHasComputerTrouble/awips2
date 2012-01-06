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
package com.raytheon.uf.viz.d2d.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.viz.d2d.ui.dialogs.procedures.ProcedureListDlg;
import com.raytheon.uf.viz.d2d.ui.dialogs.procedures.ProcedureListDlg.Mode;

/**
 * DeleteAWIPSProcedure
 * 
 * Delete an AWIPS procedure
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date         Ticket#     Engineer    Description
 *    ------------ ----------  ----------- --------------------------
 *    Sep 13, 2007             chammack    Initial Creation.
 *    Jul 8, 2008  #1183       chammack    Migrate to new localization
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class DeleteAWIPSProcedure extends AbstractHandler {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
     * .ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ProcedureListDlg listDlg = new ProcedureListDlg("Delete Procedure",
                HandlerUtil.getActiveShell(event), Mode.DELETE);
        listDlg.open();

        LocalizationFile selectedFile = listDlg.getSelectedFile();
        if (selectedFile != null && selectedFile.exists()) {
            try {
                selectedFile.delete();
            } catch (Exception e) {
                throw new ExecutionException("Error deleting procedure: "
                        + selectedFile.getName(), e);
            }
        }

        return null;
    }

}
