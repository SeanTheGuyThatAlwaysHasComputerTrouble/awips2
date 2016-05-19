package com.raytheon.viz.gfe.textproduct.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.viz.gfe.Activator;
import com.raytheon.viz.gfe.GFEException;
import com.raytheon.viz.gfe.constants.StatusConstants;
import com.raytheon.viz.gfe.core.script.IScriptUtil;
import com.raytheon.viz.gfe.core.script.IScriptUtil.Overwrite;

/**
 * An action to upgrade a a text product and make it the SITE override script.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 30, 2010            dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */
public class TextProductSiteOverrideAction extends Action {
    private static final transient IUFStatusHandler statusHandler = UFStatus.getHandler(TextProductSiteOverrideAction.class);

    private static final Pattern OVR_PATTERN = Pattern.compile(
            "(.+?)_?overrides", Pattern.CASE_INSENSITIVE);

    String script;

    IScriptUtil util;

    public TextProductSiteOverrideAction(String script, IScriptUtil util) {
        super("Make SITE Override");
        this.script = script;
        this.util = util;
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        String scriptType = util.getScriptType();
        LocalizationFile source = null;
        try {
            source = util.find(script, null);
        } catch (GFEException e) {
            String message = String.format(
                    "An error occurred while finding '%s'", script);
            statusHandler.handle(Priority.PROBLEM, message, e);
            return;
        }

        // Make sure the source exists
        if (source == null) {
            String message = String.format("%s '%s' does not exist",
                    util.getScriptType(), script);
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Does Not Exist", message);
            statusHandler.handle(Priority.PROBLEM, message);
            return;
        }

        LocalizationLevel srcLevel = source.getContext().getLocalizationLevel();

        String dest = script;
        Matcher ovrMatch = OVR_PATTERN.matcher(script);
        boolean isOverride = ovrMatch.matches();

        if (isOverride) {
            // strip "override" or "_override" from destination name
            dest = ovrMatch.group(1);
        }

        if (srcLevel.isSystemLevel()) {
            // source is BASE, we can't downgrade it
            String message = String.format(
                    "%s '%s' cannot be moved to SITE level.", scriptType,
                    script);
            MessageDialog.openInformation(
                    Display.getCurrent().getActiveShell(), "Cannot Move Base",
                    message);
            return;
        }

        if (LocalizationLevel.SITE == srcLevel) {
            // nothing to do, in two flavors:
            String message = null;
            if (isOverride) {
                message = String.format(
                        "%s '%s' is already the site override.", scriptType,
                        script);

            } else {
                message = String.format("'%s' is the SITE %s.\n"
                        + "It cannot be made the SITE override.", script,
                        scriptType);
            }
            MessageDialog.openInformation(
                    Display.getCurrent().getActiveShell(), message, message);
            return;
        }

        dest = dest + "_OVERRIDE";
        LocalizationFile destlf = null;
        try {
            destlf = util.find(dest, LocalizationLevel.SITE);
        } catch (GFEException e1) {
            String message = String.format("Error during search for %s '%s'",
                    scriptType, dest);
            statusHandler.handle(Priority.PROBLEM, message);
            return;
        }

        if (destlf != null) {
            // destination already exists. Confirm overwrite.
            String message = String
                    .format("%s '%s' already exists at SITE level!\n"
                            + "Confirm that you want to overwrite it from %s at %s level:",
                            scriptType, dest, script, srcLevel.toString());
            boolean confirmed = MessageDialog.openConfirm(Display.getCurrent()
                    .getActiveShell(), "Confirm Overwrite", message);
            if (confirmed) {
                try {
                    destlf.delete();
                } catch (Exception e) {
                    String errMessage = String.format("Error deleting %s '%s'",
                            scriptType, dest);
                    statusHandler.handle(Priority.PROBLEM, errMessage, e);
                }
            } else {
                return;
            }
        }

        try {
            util.copy(script, dest, LocalizationLevel.SITE, Overwrite.SAFE);
            source.delete();
            String message = String.format("%s '%s' set as SITE", scriptType,
                    dest);
            statusHandler.handle(Priority.VERBOSE, message);
        } catch (Exception e) {
            String message = String.format("Error ", script);
            statusHandler.handle(Priority.PROBLEM, message, e);
        }
    }
}
