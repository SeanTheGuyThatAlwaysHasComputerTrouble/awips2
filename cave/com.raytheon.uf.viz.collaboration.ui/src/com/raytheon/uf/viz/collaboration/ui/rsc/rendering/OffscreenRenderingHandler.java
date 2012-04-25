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
package com.raytheon.uf.viz.collaboration.ui.rsc.rendering;

import java.nio.Buffer;

import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.ext.IOffscreenRenderingExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.remote.graphics.events.offscreen.CreateOffscreenImageEvent;
import com.raytheon.uf.viz.remote.graphics.events.offscreen.RenderOffscreenEvent;
import com.raytheon.uf.viz.remote.graphics.events.offscreen.RenderOnscreenEvent;

/**
 * Handles render events for offscreen rendering
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 16, 2012            mschenke     Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class OffscreenRenderingHandler extends CollaborationRenderingHandler {

    @Subscribe
    public void createOffscreenImage(CreateOffscreenImageEvent event)
            throws VizException {
        IGraphicsTarget target = getTarget();
        try {
            IImage offscreenImage = null;
            IOffscreenRenderingExtension ext = target
                    .getExtension(IOffscreenRenderingExtension.class);
            int[] dims = event.getDimensions();
            if (event.getBufferType() != null) {
                Class<? extends Buffer> bufferType = Class.forName(
                        event.getBufferType()).asSubclass(Buffer.class);
                if (event.getColorMapParamters() != null) {
                    offscreenImage = ext.constructOffscreenImage(bufferType,
                            dims, event.getColorMapParamters()
                                    .getColorMapParameters());
                } else {
                    offscreenImage = ext.constructOffscreenImage(bufferType,
                            dims);
                }
            } else {
                offscreenImage = ext.constructOffscreenImage(dims);
            }
            if (offscreenImage != null) {
                dataManager.putRenderableObject(event.getObjectId(),
                        offscreenImage);
            }
        } catch (ClassNotFoundException e) {
            throw new VizException("Could not find class for buffer type: "
                    + event.getBufferType(), e);
        }
    }

    @Subscribe
    public void renderOffscreen(RenderOffscreenEvent event) throws VizException {
        IGraphicsTarget target = getTarget();
        IImage offscreenImage = dataManager.getRenderableObject(
                event.getObjectId(), IImage.class);
        if (offscreenImage != null) {
            if (event.getExtent() != null) {
                target.getExtension(IOffscreenRenderingExtension.class)
                        .renderOffscreen(offscreenImage, event.getIExtent());
            } else {
                target.getExtension(IOffscreenRenderingExtension.class)
                        .renderOffscreen(offscreenImage);
            }
        }
    }

    @Subscribe
    public void renderOnscreen(RenderOnscreenEvent event) throws VizException {
        IGraphicsTarget target = getTarget();
        target.getExtension(IOffscreenRenderingExtension.class)
                .renderOnscreen();
    }

}
