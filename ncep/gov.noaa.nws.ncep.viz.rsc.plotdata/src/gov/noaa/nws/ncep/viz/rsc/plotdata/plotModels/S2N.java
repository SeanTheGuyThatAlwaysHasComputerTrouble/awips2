package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.viz.localization.impl.LocalizationManager;
import gov.noaa.nws.ncep.viz.localization.impl.LocalizationResourcePathConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.raytheon.uf.common.localization.PathManagerFactory;

public class S2N {

    private static final String plotmodelDir = "plotModels";

    private final String filename;

    private final HashMap<String, Integer> lookup;

    private int bestField;

    protected S2N(String filename) {
        this.filename = filename;
        this.lookup = new HashMap<String, Integer>();
    }

    public String getFilename() {
        return this.filename;
    }

    public String getRankedField(String[] fields) {
        int bestField = this.bestField;
        String returnString = null;
        for (String field : fields) {
            if (this.lookup.containsKey(field)) {
                if (this.lookup.get(field) <= bestField) {
                    bestField = this.lookup.get(field);
                    returnString = field;
                }
            }
        }
        return returnString;
    }

    public void setLookupEntry(String field, int ranking) {
        this.lookup.put(field, ranking);
    }

    public void setHighestValue(int value) {
        this.bestField = value;
    }

    public static S2N readS2NFile(String s2nFilename) {
        BufferedReader input = null;
        S2N lookup = new S2N(s2nFilename);
//      File s2nFile = PathManagerFactory.getPathManager().getStaticFile(  S2N.plotmodelDir + File.separator + s2nFilename);        
        File s2nFile = LocalizationManager.getInstance().getLocalizationFileDirectly(	LocalizationResourcePathConstants.PLOTMODELPARAMETERS_DIR,	s2nFilename);

        int counter = 0;
        int highestValue = -1;
        try {
            input = new BufferedReader(new FileReader(s2nFile));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.matches("s2n") && counter == 0) {
                    counter++;
                    continue;
                }
                String[] lookupValues = line.split("\\s");
                if (lookupValues.length == 2) {
                    int fieldValue = Integer.parseInt(lookupValues[1]);
                    lookup.setLookupEntry(lookupValues[0], fieldValue);
                    if (counter == 1) {
                        highestValue = fieldValue;
                    } else {
                        if (fieldValue > highestValue) {
                            highestValue = fieldValue;
                        }
                    }
                }
            }
            if (highestValue != -1) {
                lookup.setHighestValue(highestValue);
            }
            input.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return lookup;
    }
}
