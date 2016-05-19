
import os, shutil, time
from com.raytheon.edex.plugin.gfe.config import IFPServerConfigManager
import siteConfig
import LogStream

def main():
    path = siteConfig.GFESUITE_LOGDIR+"/"
    if(os.path.exists(path)):
        purgeAge = IFPServerConfigManager.getServerConfig(siteConfig.GFESUITE_SITEID).logFilePurgeAfter()
        duration = 86400 * purgeAge
        cutoffTime = time.strftime("%Y%m%d", time.gmtime(time.time() - duration))
        LogStream.logEvent("Purging GFE log files older than", purgeAge, "days")
        dirList = os.listdir(path)

        for fname in dirList:
            if fname < cutoffTime:
                shutil.rmtree(path + fname)
