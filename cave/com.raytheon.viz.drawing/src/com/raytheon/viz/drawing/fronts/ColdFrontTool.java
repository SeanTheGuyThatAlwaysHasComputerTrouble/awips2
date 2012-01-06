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

package com.raytheon.viz.drawing.fronts;

import fsl.tools.glyph.FreehandPath;
import fsl.tools.glyph.Glyph;
import fsl.tools.scribble.ColdFront;
import fsl.tools.scribble.Front;

/**
 * 
 * Draw a Cold Front
 * 
 * <pre>
 *  
 *   SOFTWARE HISTORY
 *  
 *   Date         Ticket#     Engineer    Description
 *   ------------ ----------  ----------- --------------------------
 *   Oct 26, 2006 66          chammack    Initial Creation.
 *   
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class ColdFrontTool extends AbstractFrontTool {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.raytheon.viz.drawing.fronts.AbstractFrontTool#createFront()
	 */
	@Override
	public Front createFront() {
		return new ColdFront(Glyph.ALL_FRAMES, 1,
				FreehandPath.SMOOTHING_AVERAGE, 24.0, 12.0, false, false);
	}

}
