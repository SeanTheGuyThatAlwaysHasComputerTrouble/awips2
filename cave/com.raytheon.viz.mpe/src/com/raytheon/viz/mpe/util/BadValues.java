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
package com.raytheon.viz.mpe.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import com.raytheon.uf.common.ohd.AppsDefaults;
import com.raytheon.viz.mpe.util.DailyQcUtils.Bad_Daily_Values;
import com.raytheon.viz.mpe.util.DailyQcUtils.Station;

/**
 * TODO Add Description
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 9, 2009            snaples     Initial creation
 * Jun 17, 2015  17388    ptilles     added check on mpe_dqc_6hr_24hr_flag and
 *                                      changed definition of max_stations variable
 * 
 * </pre>
 * 
 * @author snaples
 * @version 1.0
 */

public class BadValues {
    BufferedReader in = null;
    DailyQcUtils dqc = DailyQcUtils.getInstance();
    ReadPrecipStationList rp = new ReadPrecipStationList();
    //private int max_stations = rp.getNumPstations();
    int max_stations = DailyQcUtils.precip_stations.size();
    
    static int mpe_dqc_6hr_24hr_flag = 1;
    
    static
    {
    	//token name:  mpe_dqc_6hr_24hr_set_bad
    	// token value = OFF
    	//   mpe_dqc_6hr_24hr_flag = 0
    	//   if user sets 6hr value to Bad, then 24hr value is unaffected

    	// token value = ON
    	//   mpe_dqc_6hr_24hr_flag = 1
    	//   if user sets 6hr value to Bad, then 24hr value is set to Bad

    	String mpe_dqc_6hr_24hr_string = AppsDefaults.getInstance().getToken(
    			"mpe_dqc_6hr_24hr_set_bad", "ON");

    	if (mpe_dqc_6hr_24hr_string.equalsIgnoreCase("OFF"))
    	{
    		mpe_dqc_6hr_24hr_flag = 0;
    	}
    }

    
    public void read_bad_values(String precd, int m) {

        Bad_Daily_Values bad_values[] = dqc.bad_values;
        int i;
        Scanner s = null;

        try {

            in = new BufferedReader(new FileReader(precd));
            for (i = 0; i < 6000; i++) {

                if (bad_values[i].used == 1) {
                    continue;
                }

                String vals = in.readLine();

                if (vals == null) {
                    break;
                }

                s = new Scanner(vals);

                bad_values[i].used = 1;
                bad_values[i].hb5 = s.next();
                bad_values[i].parm = s.next();
                bad_values[i].day = m;
                bad_values[i].quart = (int) s.nextDouble();
                bad_values[i].fvalue = s.nextFloat();
            }
            in.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            System.out.println("File not found " + precd);
            return;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int get_bad_values(int iday, int iquart)

    {
        Bad_Daily_Values bad_values[] = dqc.bad_values;
        int i, j, h;

        for (i = 0; i < 6000; i++) {

            if (bad_values[i].used == 0) {
                continue;
            }

            if (bad_values[i].day != iday || bad_values[i].quart != iquart) {
                continue;
            }

            for (j = 0; j < max_stations; j++) {

                if (bad_values[i].hb5.equals(dqc.precip_stations
                        .get(j).hb5)
                        && bad_values[i].parm.charAt(4) == dqc.precip_stations
                                .get(j).parm.charAt(4)) {

                    if (dqc.pdata[iday].stn[j].frain[iquart].qual == 5
                            && bad_values[i].fvalue != dqc.pdata[iday].stn[j].rrain[iquart].data
                            && dqc.pdata[iday].stn[j].rrain[iquart].data >= 0) {

                        /* eliminate all bad values for current month */
                        for (h = 0; h < 6000; h++) {

                            if (bad_values[i].used == 0) {
                                continue;
                            }

                            if (bad_values[i].day != iday
                                    || bad_values[i].quart != iquart) {
                                continue;
                            }

                            bad_values[h].used = 0;

                        }

                        /* swap in level 1 data */

                        return (1);

                    }

                    else {

                        dqc.pdata[iday].stn[j].frain[iquart].qual = 1;
                        dqc.pdata[iday].stn[j].frain[iquart].data = bad_values[i].fvalue;
                    }
                }
            }
        }
        return 0;
    }

    public void write_bad_values(String fname, int iday) {

        String ibuf;
        int i;
        File bfile = new File(fname);
        BufferedWriter out = null;
        Bad_Daily_Values bad_values[] = dqc.bad_values;

        try {
            out = new BufferedWriter(new FileWriter(bfile));

            for (i = 0; i < 6000; i++) {

                if (bad_values[i].used == 0) {
                    continue;
                }

                if (iday == bad_values[i].day)
                {

                    if (bad_values[i].fvalue < 0)
                    {

                        System.out.println("Attempt to write value < 0\n");
                        continue;
                    }

                    System.out.println("In write_bad_values method:" + bad_values[i].hb5 + bad_values[i].parm + 
                    		bad_values[i].quart + bad_values[i].fvalue);
                    ibuf = String.format("%s %s %d %f", bad_values[i].hb5,
                            bad_values[i].parm, bad_values[i].quart,
                            bad_values[i].fvalue);
                    out.write(ibuf);
                    out.newLine();

                }

            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not open file: " + bfile);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return;
    }

    public void update_bad_values(int iday) {

        int i, j, h, k;
        Bad_Daily_Values bad_values[] = dqc.bad_values;
        ArrayList<Station> station = dqc.precip_stations;

        for (i = 0; i < 6000; i++)
        {

            if (bad_values[i].used == 0) {
                continue;
            }

            if (bad_values[i].day != iday) {
                continue;
            }

            bad_values[i].used = 0;

        }

        for (j = 0; j < max_stations; j++)
        {

            for (k = 0; k < 5; k++)
            {

                if (dqc.pdata[iday].stn[j].frain[k].qual != 1)
                {
                    continue;
                }
                
                for (h = 0; h < 6000; h++)
                {

                    if (bad_values[h].used != 0)
                    {
                        continue;
                    }

                    bad_values[h].used = 1;

                    /*
                     * since allow TD and partial gage set as bad, then missing
                     * level 1 data will be displayed as -1 on DQC, now need to
                     * retain the original level 2 value on DQC when set as bad
                     */

                    bad_values[h].fvalue = dqc.pdata[iday].stn[j].frain[k].data;

                    bad_values[h].hb5 = station.get(j).hb5;
                    bad_values[h].parm = station.get(j).parm;
                    bad_values[h].day = iday;
                    bad_values[h].quart = k;

                    break;

                }
            }

        }

        return;

    }

    public void restore_bad_values(int iday, ArrayList<Station> precip_stations, int max_stations)
    {

        int i, j, k;

        for (k = 0; k < 5; k++)
        {

            for (i = 0; i < 6000; i++)
            {

                if (dqc.bad_values[i].used == 0) {
                    continue;
                }

                if (dqc.bad_values[i].day != iday || dqc.bad_values[i].quart != k) {
                    continue;
                }

                for (j = 0; j < max_stations; j++)
                {

                    if ((dqc.bad_values[i].hb5.equalsIgnoreCase(precip_stations.get(j).hb5))
                            && dqc.bad_values[i].parm.charAt(4) == precip_stations.get(j).parm.charAt(4))
                    {

                        dqc.pdata[iday].stn[j].frain[k].data = dqc.bad_values[i].fvalue;
                        dqc.pdata[iday].stn[j].frain[k].qual = 1;

                        // 6hr qual code bad - check how to set 24hr qual code
                        // added for DR 17388
                        
                        if(mpe_dqc_6hr_24hr_flag == 1)
                        {

                        	if (k >= 0 && k <= 3 && dqc.pdata[iday].stn[j].rrain[4].data >= 0)
                        	{

                        		dqc.pdata[iday].stn[j].frain[4].qual = 1;

                        	}
                        }

                        break;

                    }

                }
            }

        }

        return;

    }

    public int is_bad(int iday, int iquart, String hb5, String parm)

    {
        int i, j;

        for (i = 0; i < 6000; i++) {

            if (dqc.bad_values[i].used == 0) {
                continue;
            }

            if (dqc.bad_values[i].day != iday || dqc.bad_values[i].quart != iquart) {
                continue;
            }

            for (j = 0; j < max_stations; j++) {

                if (dqc.bad_values[i].hb5.equals(hb5)
                        && dqc.bad_values[i].parm.charAt(4) == parm.charAt(4)) {
                    return 1;
                }

            }
        }

        return 0;

    }

    public void post_bad_values(int iday) {

        int i, j, k;
//        Bad_Daily_Values bad_values[] = dqc.bad_values;

        for (k = 0; k < 5; k++) {

            for (i = 0; i < 6000; i++) {

                if (dqc.bad_values[i].used == 0) {
                    continue;
                }

                if (dqc.bad_values[i].day != iday || dqc.bad_values[i].quart != k) {
                    continue;
                }

                for (j = 0; j < max_stations; j++) {

                    if ((dqc.bad_values[i].hb5.equals(dqc.precip_stations
                            .get(j).hb5))
                            && dqc.bad_values[i].parm.charAt(4) == dqc.precip_stations
                                    .get(j).parm.charAt(4)) {

                        if (dqc.pdata[iday].stn[j].frain[k].data == dqc.bad_values[i].fvalue) {

                            dqc.pdata[iday].stn[j].frain[k].data = dqc.bad_values[i].fvalue;

                            dqc.pdata[iday].stn[j].frain[k].qual = 1;

                        }

                        break;

                    }

                }
            }

        }
        return;
    }

}
