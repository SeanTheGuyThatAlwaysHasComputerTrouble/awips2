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
package com.raytheon.uf.common.dataplugin.level;

import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import javax.measure.converter.ConversionException;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.xml.bind.JAXB;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.uf.common.dataplugin.level.request.GetLevelByIdRequest;
import com.raytheon.uf.common.dataplugin.level.request.GetLevelRequest;
import com.raytheon.uf.common.dataplugin.level.request.GetMasterLevelRequest;
import com.raytheon.uf.common.dataplugin.level.request.ILevelRetrievalAdapter;
import com.raytheon.uf.common.dataplugin.level.xml.LevelAlias;
import com.raytheon.uf.common.dataplugin.level.xml.LevelAliasList;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Level Factory
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 03, 2009            rjpeter     Initial creation.
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class LevelFactory {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LevelFactory.class);

    public static final String UNKNOWN_LEVEL = "UNKNOWN";

    private static final String LEVEL_FILENAME = "level/LevelAliases.xml";

    private static final String MASTER_LEVEL_FILENAME = "/level/masterLevels.xml";

    private static LevelFactory instance = new LevelFactory();

    private Log logger = LogFactory.getLog(LevelFactory.class);

    // contains the master levels
    private Map<String, MasterLevel> masterLevelCache = new HashMap<String, MasterLevel>();

    // level stub to its full level
    private Map<Level, Level> levelCache = new HashMap<Level, Level>();

    // level id to its full level
    private Map<Long, Level> levelCacheById = new HashMap<Long, Level>();

    // level id String to its full level
    private Map<String, Level> levelCacheByIdAsString = new HashMap<String, Level>();

    // lists all aliases and their associated master level name
    private Map<String, String> levelAliasMap = new HashMap<String, String>();

    private ILevelRetrievalAdapter retrievalAdapter = null;

    private static final double INVALID_LEVEL = Level.getInvalidLevelValue();

    public static LevelFactory getInstance() {
        return instance;
    }

    private LevelFactory() {
        ServiceLoader<ILevelRetrievalAdapter> loader = ServiceLoader.load(
                ILevelRetrievalAdapter.class, this.getClass().getClassLoader());

        Iterator<ILevelRetrievalAdapter> iter = loader.iterator();
        try {
            if (iter.hasNext()) {
                retrievalAdapter = iter.next();
            }
        } catch (ServiceConfigurationError e) {
            // TODO Log error
            e.printStackTrace();
        }

        loadLevelAliases();
        LoadAllMasterLevels();
        loadMasterLevelFiles();
        loadAllLevels();
    }

    public void checkMasterLevel(MasterLevel level) {
        loadMasterLevel(level, true);
    }

    public MasterLevel getMasterLevel(String name) {
        MasterLevel request = new MasterLevel(name);
        return loadMasterLevel(request, false);
    }

    public Level getLevel(long id) {
        return loadLevel(id);
    }

    public Level getLevel(String name) {
        return loadLevel(name);
    }

    public Level getLevel(String name, double levelOneValue) {
        return getLevel(name, levelOneValue, INVALID_LEVEL, null);
    }

    public Level getLevel(String name, double levelOneValue, String unit) {
        return getLevel(name, levelOneValue, INVALID_LEVEL, unit);
    }

    public Level getLevel(String name, double levelOneValue,
            double levelTwoValue) {
        return getLevel(name, levelOneValue, levelTwoValue, null);
    }

    public Level getLevel(String name, double levelOneValue,
            double levelTwoValue, String unit) {
        Level rval = null;

        // lookup master level
        MasterLevel masterLevel = getMasterLevel(name);

        if (masterLevel != null) {
            try {
                Level requestedLevel = new Level();
                requestedLevel.setMasterLevel(masterLevel);

                // check units, if units is null assume default units or level
                // has no units
                if ((unit != null)
                        && (unit.trim().length() > 0)
                        && ((levelOneValue != INVALID_LEVEL) || (levelTwoValue != INVALID_LEVEL))) {
                    Unit<?> masterUnit = masterLevel.getUnit();
                    if (masterUnit != null) {
                        Unit<?> incomingUnit = (Unit<?>) UnitFormat
                                .getUCUMInstance().parseObject(unit);
                        UnitConverter converter = incomingUnit
                                .getConverterTo(masterUnit);
                        if (levelOneValue != INVALID_LEVEL) {
                            levelOneValue = converter.convert(levelOneValue);
                        }
                        if (levelTwoValue != INVALID_LEVEL) {
                            levelTwoValue = converter.convert(levelTwoValue);
                        }
                    }
                }

                requestedLevel.setLevelonevalue(levelOneValue);
                requestedLevel.setLeveltwovalue(levelTwoValue);

                rval = loadLevel(requestedLevel);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (ConversionException e) {
                e.printStackTrace();
            }
        } else {
            logger.warn("Requested level name [" + name
                    + "] does not map to a defined level");
            // TODO throw level exception
        }

        return rval;
    }

    private MasterLevel loadMasterLevel(MasterLevel level, boolean createFlag) {
        MasterLevel rval = null;
        String levelName = level.getName();

        // check aliasing
        if (levelAliasMap.containsKey(levelName)) {
            levelName = levelAliasMap.get(levelName);
        }

        if (masterLevelCache.containsKey(levelName)) {
            rval = masterLevelCache.get(levelName);
        } else if (retrievalAdapter != null) {
            // create new requested level, so the incoming is not mangled
            MasterLevel requestedLevel = new MasterLevel(levelName);

            // if the create flag is set fill in the rest of the fields on the
            // requestedLevel obj
            if (createFlag) {
                requestedLevel.setDescription(level.getDescription());
                requestedLevel.setType(level.getType());
                requestedLevel.setUnitString(level.getUnitString());
            }

            GetMasterLevelRequest request = new GetMasterLevelRequest(
                    requestedLevel, createFlag);

            rval = retrievalAdapter.getMasterLevel(request);

            // if level was retrieved, post process it
            if (rval != null) {
                cacheMasterLevel(rval);
            }
        } else {
            logger.error("No level retrieval adapter defined");
        }

        return rval;
    }

    private Level loadLevel(Level level) {
        // limit precision to 3 places past the decimal
        double levelone = ((int) (level.getLevelonevalue() * 1000)) / 1000.0;
        double leveltwo = ((int) (level.getLeveltwovalue() * 1000)) / 1000.0;
        if (level.isRangeLevel()) {
            Progression prog = level.getMasterLevel().getProgression();
            if ((prog == Progression.INC && leveltwo < levelone)
                    || (prog == Progression.DEC && leveltwo > levelone)) {
                // Swap the values so they are in the correct order
                double tmp = levelone;
                levelone = leveltwo;
                leveltwo = tmp;
            }
        }
        level.setLevelonevalue(levelone);
        level.setLeveltwovalue(leveltwo);
        // check if we have already loaded level
        Level rval = levelCache.get(level);

        if ((rval == null) && (retrievalAdapter != null)) {
            GetLevelRequest request = new GetLevelRequest();
            request.setLevel(level);

            rval = retrievalAdapter.getLevel(request);

            if (rval != null) {
                // replace MasterLevel with cached master level
                rval.setMasterLevel(getMasterLevel(rval.getMasterLevel()
                        .getName()));

                cacheLevel(rval);
            }
        }

        return rval;
    }

    private Level loadLevel(long id) {
        // check if we have already loaded level
        Level rval = levelCacheById.get(id);

        if ((rval == null) && (retrievalAdapter != null)) {
            GetLevelByIdRequest request = new GetLevelByIdRequest();
            request.setId(id);

            rval = retrievalAdapter.getLevel(request);

            if (rval != null) {
                cacheLevel(rval);
            }
        }

        return rval;
    }

    private Level loadLevel(String id) {
        // check if we have already loaded level
        Level rval = levelCacheByIdAsString.get(id);

        if ((rval == null) && (retrievalAdapter != null)) {
            GetLevelByIdRequest request = new GetLevelByIdRequest();
            try {
                request.setId(Long.parseLong(id));

                rval = retrievalAdapter.getLevel(request);

                if (rval != null) {
                    cacheLevel(rval);
                }
            } catch (NumberFormatException e) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "Error occurred trying to lookup level information, received level id that was not a number.",
                                e);
            }
        }

        return rval;
    }

    private void cacheMasterLevel(MasterLevel levelToCache) {
        masterLevelCache.put(levelToCache.getName(), levelToCache);
    }

    private void cacheLevel(Level levelToCache) {
        levelCache.put(levelToCache, levelToCache);
        levelCacheById.put(levelToCache.getId(), levelToCache);
        levelCacheByIdAsString.put("" + levelToCache.getId(), levelToCache);
    }

    private void loadLevelAliases() {
        Map<String, String> aliasMap = new HashMap<String, String>();

        IPathManager pathMgr = PathManagerFactory.getPathManager();
        Map<LocalizationLevel, LocalizationFile> tieredMap = pathMgr
                .getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        LEVEL_FILENAME);

        LocalizationLevel[] levels = pathMgr.getAvailableLevels();
        for (LocalizationLevel level : levels) {
            LocalizationFile file = tieredMap.get(level);
            if (file != null) {
                loadLevelAliasFile(file.getFile(), aliasMap);
            }
        }

        // set to master file
        levelAliasMap = aliasMap;
    }

    private void loadLevelAliasFile(File file, Map<String, String> aliasMap) {
        if ((file != null) && file.exists()) {
            LevelAliasList defList = null;

            // use jaxb to deserialize file
            try {
                Object xmlObj = SerializationUtil.jaxbUnmarshalFromXmlFile(file
                        .getAbsolutePath());

                if (xmlObj instanceof LevelAliasList) {
                    defList = (LevelAliasList) xmlObj;
                } else {
                    logger.error("Malformed alias file, expected ["
                            + LevelAliasList.class.getName() + "], received ["
                            + xmlObj.getClass().getName() + "]");
                }
            } catch (Exception e) {
                logger.error("Caught exception parsing level alias file", e);
            }

            if ((defList != null) && (defList.getLevels() != null)) {
                for (LevelAlias level : defList.getLevels()) {
                    String[] aliases = level.getAliases();
                    if (aliases != null) {
                        for (String alias : aliases) {
                            aliasMap.put(alias, level.getName());
                        }
                    }
                }
            }
        }
    }

    private void loadAllLevels() {
        if (retrievalAdapter != null) {
            LevelContainer container = retrievalAdapter.getAllLevels();
            if (container != null) {
                List<Level> levels = container.getLevels();

                if (levels != null && levels.size() > 0) {
                    for (Level lvl : levels) {
                        MasterLevel mLvl = lvl.getMasterLevel();

                        // use a single master level object for common levels
                        if (masterLevelCache.containsKey(mLvl.getName())) {
                            mLvl = masterLevelCache.get(mLvl.getName());
                            lvl.setMasterLevel(mLvl);
                        } else {
                            cacheMasterLevel(mLvl);
                        }

                        cacheLevel(lvl);
                    }
                }
            }
        }
    }

    private void LoadAllMasterLevels() {
        if (retrievalAdapter != null) {
            MasterLevelContainer container = retrievalAdapter
                    .getAllMasterLevels();
            if (container != null) {
                List<MasterLevel> levels = container.getMasterLevels();

                if (levels != null && levels.size() > 0) {
                    for (MasterLevel lvl : levels) {
                        if (!masterLevelCache.containsKey(lvl.getName())) {
                            cacheMasterLevel(lvl);
                        }
                    }
                }
            }
        }
    }

    private void loadMasterLevelFiles() {
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        Map<LocalizationLevel, LocalizationFile> tieredMap = pathMgr
                .getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        MASTER_LEVEL_FILENAME);

        LocalizationLevel[] levels = pathMgr.getAvailableLevels();
        for (LocalizationLevel level : levels) {
            LocalizationFile file = tieredMap.get(level);
            if (file != null) {
                try {
                    loadMasterLevelFile(file.getFile());
                } catch (Throwable e) {
                    statusHandler.error("Unable to load site masterLevels"
                            + file.getFile().getAbsolutePath(), e);
                }
            }
        }

    }

    private void loadMasterLevelFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        MasterLevelContainer list = JAXB.unmarshal(file,
                MasterLevelContainer.class);
        for (MasterLevel ml : list.getMasterLevels()) {
            checkMasterLevel(ml);
        }
        statusHandler.debug("Successfully loaded master levels from "
                + file.getAbsolutePath());
    }
}
