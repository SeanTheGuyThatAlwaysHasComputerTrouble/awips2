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
package com.raytheon.edex.plugin.shef;

import java.util.Date;

import com.raytheon.edex.esb.Headers;
import com.raytheon.edex.plugin.shef.ShefSeparator.ShefDecoderInput;
import com.raytheon.edex.plugin.shef.data.ShefRecord;
import com.raytheon.edex.plugin.shef.database.PostShef;
import com.raytheon.edex.plugin.shef.database.PurgeText;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.ohd.AppsDefaults;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.decodertools.core.DecoderTools;

/**
 * Decoder implementation for SHEF data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 03/19/2008   387         M. Duff     Initial creation.  
 * 06/02/2008   1166        M. Duff     Added checks for null data objects.
 * 10/16/2008   1548        jelkins     Integrated ParameterCode Types
 * 12/3/2008                chammack    Camel refactored
 * 12/10/2008   1786        J. Sanchez  Handle exceptions thrown by SHEF data.
 *                                      Updated decodeAValue to include SNOW_NEW_SNOWFALL
 *                                      to handle T value for precipitation.
 * 12/18/2008   1722        J. Sanchez  Update processComments to correctly handle E Records      
 * 01/08/2009   1846        J. Sanchez  Update parse method to handle of B Records.      
 * 01/14/2009   1864        J. Sanchez  Update parse method to handle missing record identifier.    
 * 01/15/2009   1892        J. Sanchez  Update parse method, set obsTimeFlag to false when done.
 * 12/--/2009               jkorman     Major refactor - split into ShefDecoder/SHEFParser
 * 03/07/2013   15071       W. Kwock    Skip empty data files.
 * 04/28/2014   3088        mpduff      Use UFStatus logging, various cleanup.
 * </pre>
 */
public class ShefDecoder {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(ShefDecoder.class);

    // SHEF never returns real data to edex, so create an empty data array
    // here.
    private PluginDataObject[] records = new PluginDataObject[0];

    /**
     * Constructor
     */
    public ShefDecoder() {
        this("shef");
    }

    /**
     * Constructor
     * 
     * @param name
     */
    public ShefDecoder(String name) {
    }

    /**
     * Decode.
     * 
     * @param data
     *            Data to decode
     * @param headers
     *            The headers for the data
     * @return PluginDataObject[] of decoded data
     */
    public PluginDataObject[] decode(byte[] data, Headers headers) {
        boolean archiveMode = AppsDefaults.getInstance().getBoolean(
                "ALLOW_ARCHIVE_DATA", false);

        String traceId = null;

        if (data == null || data.length == 0) {
            return null;
        }

        if (headers != null) {
            traceId = (String) headers.get(DecoderTools.INGEST_FILE_NAME);
        }

        ShefSeparator separator = null;
        try {
            separator = ShefSeparator.separate(data, headers);
        } catch (Exception e) {
            logger.error("Could not separate " + traceId, e);
            separator = null;
        }
        if (separator != null) {

            long startTime = System.currentTimeMillis();

            Date postDate = null;
            if (archiveMode) {
                postDate = getPostTime(separator.getWmoHeader().getHeaderDate()
                        .getTimeInMillis());
            } else {
                postDate = getPostTime(startTime);
            }

            PostShef postShef = new PostShef(postDate);
            if (separator.hasNext()) {
                PurgeText pText = new PurgeText(postDate);
                pText.storeTextProduct(separator);
            }

            doDecode(separator, traceId, postShef);
            logger.info(traceId + "- Decode complete in "
                    + (System.currentTimeMillis() - startTime)
                    + " milliSeconds");
        }

        return records;
    }

    /**
     * 
     * @param data
     * @param headers
     * @return
     */
    public PluginDataObject[] decodeNoWMOHeader(byte[] data, Headers headers) {
        String traceId = null;

        if (headers != null) {
            traceId = (String) headers.get(DecoderTools.INGEST_FILE_NAME);
        }
        if (traceId != null) {
            logger.info("Separating " + traceId);
        }

        ShefSeparator separator = null;
        try {
            separator = ShefSeparator.separate(data, headers);

        } catch (Exception e) {
            logger.error("Could not separate " + traceId, e);
            separator = null;
        }

        if (separator != null) {
            long startTime = System.currentTimeMillis();
            Date postDate = getPostTime(startTime);

            PostShef postShef = null;
            try {
                postShef = new PostShef(postDate);
            } catch (Exception e) {
                logger.error("Could not create PostShef", e);
            }
            if (postShef != null) {
                try {
                    doDecode(separator, traceId, postShef);
                    logger.info(traceId + "- Decode complete in "
                            + (System.currentTimeMillis() - startTime)
                            + " milliSeconds");
                } catch (Exception e) {
                    logger.error("ShefDecoder.decode failed", e);
                }
            }
        }
        return records;
    }

    private void doDecode(ShefSeparator separator, String traceId,
            PostShef postShef) {
        long startTime = System.currentTimeMillis();
        try {
            AppsDefaults appDefaults = AppsDefaults.getInstance();
            boolean logSHEFOut = appDefaults.getBoolean("shef_out", false);

            // Check to see if the separator has data to be processed.
            boolean dataProcessed = separator.hasNext();
            while (separator.hasNext()) {
                ShefDecoderInput sdi = separator.next();
                try {
                    SHEFParser parser = new SHEFParser(sdi);
                    ShefRecord shefRecord = parser.decode();
                    if (shefRecord != null) {
                        if (shefRecord.getDataValues() != null) {
                            try {
                                if (logSHEFOut) {
                                    logger.info(traceId + " > " + shefRecord);
                                }
                                postShef.post(shefRecord);
                            } catch (Throwable tt) {
                                logger.error(traceId
                                        + "- Could not post record.", tt);
                            }
                        } else {
                            logger.info(traceId + "- No data records in file.");
                        }
                    } else {
                        logger.info(traceId + "- No records in file.");
                    }
                } catch (Exception ee) {
                    logger.error(traceId + "- Could not parse SHEF report.", ee);
                }
            } // while()
            if (dataProcessed) {
                postShef.logStats(traceId, System.currentTimeMillis()
                        - startTime);
            }
        } finally {
            postShef.close();
        }
    }

    /**
     * 
     * @param startTime
     * @return
     */
    private static Date getPostTime(long startTime) {
        // Force time to nearest second.
        return new Date(startTime - (startTime % 1000));
    }

    /*
     * 
     */
    public static final void main(String[] args) {

        long t = System.currentTimeMillis();
        Date postDateA = new Date(t);
        t = t - (t % 1000);
        Date postDateB = new Date(t);

        System.out.println(postDateA.getTime());
        System.out.println(postDateB.getTime());
    }
}
