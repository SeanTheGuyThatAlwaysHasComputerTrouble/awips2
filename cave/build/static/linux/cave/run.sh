#!/bin/bash
# CAVE startup script
# Note: CAVE will not run as 'root'

# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract DG133W-05-CQ-1067 with the US Government.
# 
# U.S. EXPORT CONTROLLED TECHNICAL DATA
# This software product contains export-restricted data whose
# export/transfer/disclosure is restricted by U.S. law. Dissemination
# to non-U.S. persons whether in the United States or abroad requires
# an export license or other authorization.
# 
# Contractor Name:        Raytheon Company
# Contractor Address:     6825 Pine Street, Suite 340
#                         Mail Stop B8
#                         Omaha, NE 68106
#                         402.291.0100
# 
# See the AWIPS II Master Rights File ("Master Rights File.pdf") for
# further licensing information.
#
#
# SOFTWARE HISTORY
# Date         Ticket#    Engineer    Description
# ------------ ---------- ----------- --------------------------
# Dec 05, 2013  #2593     rjpeter     Added check for number of running
#                                     cave sessions.
# Dec 05, 2013  #2590     dgilling    Modified so gfeclient.sh can be wrapped
#                                     around this script.
# Jan 24, 2014  #2739     bsteffen    Log exit status
# Jan 30, 2014  #2593     bclement    warns based on memory usage, fixed for INI files with spaces
# Jul 10, 2014  #3363     bclement    logs command used to launch application to console logs
# Oct 10, 2014  #3675     njensen     Logback now does console logging to ensure correct pid 
# Oct 13, 2014  #3675     bclement    startup shutdown log includes both launching pid and placeholder
# Jan 28, 2015  #4018     randerso    Added a productEditor log file to changes in the GFE product editor
# Jun 17, 2015  #4148     rferrel     Logback needs fewer environment variables.
# Jul 23, 2015  ASM#13849 D. Friedman Use a unique Eclipse configuration directory
# Aug 03, 2015  #4694     dlovely     Logback will now add user.home to LOGDIR
#


user=`/usr/bin/whoami`
if [ ${user} == 'root' ];then
   echo "WARNING: CAVE cannot be run as user '${user}'!"
   echo "         change to another user and run again."
   exit 1
fi

# Since, we no longer need to worry about re-location ...
CAVE_INSTALL="/awips2/cave"
JAVA_INSTALL="/awips2/java"
PYTHON_INSTALL="/awips2/python"
export AWIPS_INSTALL_DIR="${CAVE_INSTALL}"

MAX_MEM_PROPORTION="0.85"

source ${CAVE_INSTALL}/caveUtil.sh
RC=$?
if [ ${RC} -ne 0 ]; then
   echo "ERROR: unable to find and/or access ${CAVE_INSTALL}/caveUtil.sh."
   exit 1
fi

# Run monitorThreads?
runMonitorThreads=false

# copy the viz shutdown utility if necessary.
copyVizShutdownUtilIfNecessary

# delete any old disk caches in the background
deleteOldCaveDiskCaches &

# Enable core dumps
ulimit -c unlimited >> /dev/null 2>&1

export LD_LIBRARY_PATH=${JAVA_INSTALL}/lib:${PYTHON_INSTALL}/lib:$LD_LIBRARY_PATH
if [[ -z "$CALLED_EXTEND_LIB_PATH" ]]; then
    extendLibraryPath
fi
export PATH=${JAVA_INSTALL}/bin:${PYTHON_INSTALL}/bin:$PATH
export JAVA_HOME="${JAVA_INSTALL}/jre"

# The user can update this field if they choose to do so.
export HYDRO_APPS_DIR="/awips2/edex/data/share/hydroapps"

export EDEX_HOME=/awips2/edex 
export LOCALIZATION_ROOT=~/caveData/common
export PGSQL_DRIVER_DIR=`ls -1d /awips2/cave/plugins/org.postgres_*` 
if [ $? -ne 0 ]; then
   echo "FATAL: Unable to locate the PostgreSQL JDBC Driver."
   exit 1
fi
export apps_dir=${HYDRO_APPS_DIR} 

SWITCHES=($SWITCHES)
TESTCHECK="$TMCP_HOME/bin/getTestMode"
if [ -x ${TESTCHECK} ]; then
    echo "Calling getTestMode()"
    ${TESTCHECK}
    status=${?}
    if [ $status -eq 11 ]; then
        MODE="TEST"
        SWITCHES+=(-mode TEST)
    elif [ $status -eq 12 ];then
        MODE="PRACTICE"
        SWITCHES+=(-mode PRACTICE)
    elif [ $status -eq 15 ];then
        MODE="OPERATIONAL"
        SWITCHES+=(-mode OPERATIONAL)
    else
        MODE="OPERATIONAL (no response)"
    fi
    echo "getTestMode() returned ${MODE}"
else
    MODE="UNKNOWN"
    echo "getTestMode() not found - going to use defaults"
fi

export TEXTWS=`hostname | sed -e 's/lx/xt/g'`

hostName=`hostname -s`

#check for gtk-2.0 value
gtkResource=.gtkrc-2.0
includeLine="include \"$HOME/.gtkrc.mine\""
mineFile=.gtkrc.mine
altButtonLine="gtk-alternative-button-order=1"
if [ -f $HOME/$gtkResource ]; then
   if [ -w $HOME/$gtkResource ]; then
      var=`grep "gtkrc.mine" $HOME/$gtkResource`
      if [ '' == "$var" ]; then
         echo $includeLine >> $HOME/$gtkResource
      fi
   fi
else
    touch $HOME/$gtkResource
    echo $includeLine >> $HOME/$gtkResource
fi

if [ -f $HOME/$mineFile ]; then
   if [ -w $HOME/$mineFile ]; then
      var=`grep "alternative-button-order" $HOME/$mineFile`
      if [ '' == "$var" ]; then
         echo $altButtonLine >> $HOME/$mineFile
      fi
   fi
else
   touch $HOME/$mineFile
   echo $altButtonLine >> $HOME/$mineFile
fi

if [[ -z "$PROGRAM_NAME" ]]
then
    PROGRAM_NAME="cave"
fi

BASE_LOGDIR=caveData/logs/consoleLogs
# Logback configuration files will append user.home to LOGDIR.
export LOGDIR=$BASE_LOGDIR/$hostName/
FULL_LOGDIR=$HOME/$LOGDIR

# make sure directory exists
if [ ! -d $FULL_LOGDIR ]; then
 mkdir -p $FULL_LOGDIR
fi

# delete any old disk caches in the background
deleteOldCaveLogs &

curTime=`date +%Y%m%d_%H%M%S`

pid=$!
export LOGFILE_STARTUP_SHUTDOWN="$FULL_LOGDIR/${PROGRAM_NAME}_${pid}_${curTime}_pid_%PID%_startup-shutdown.log"

createEclipseConfigurationDir

# At this point fork so that log files can be set up with the process pid and
# this process can log the exit status of cave.
(
  # can we write to log directory
  if [ -w $FULL_LOGDIR ]; then
    touch ${LOGFILE_STARTUP_SHUTDOWN}
  fi

  # remove "-noredirect" flag from command-line if set so it doesn't confuse any
  # commands we call later.
  redirect="true"
  USER_ARGS=()
  while [[ $1 ]]
  do
    if [[ "$1" == "-noredirect" ]]
    then
      redirect="false"
    else
      USER_ARGS+=("$1")
    fi
    shift
  done

  lookupINI "${USER_ARGS[@]}"

  # Make it easy to determine which process is using the directory
  if [[ -n $eclipseConfigurationDir ]]; then
      echo "$$" > "$eclipseConfigurationDir"/pid
  fi

  if [[ "${runMonitorThreads}" == "true" ]] ; then 
    # nohup to allow tar process to continue after user has logged out
    nohup ${CAVE_INSTALL}/monitorThreads.sh $pid >> /dev/null 2>&1 &
  fi

  echo "Launching cave application using the following command: " >> ${LOGFILE_STARTUP_SHUTDOWN}
  echo "${CAVE_INSTALL}/cave ${CAVE_INI_ARG} ${SWITCHES[@]} ${USER_ARGS[@]}" >> ${LOGFILE_STARTUP_SHUTDOWN}

  if [[ "${redirect}" == "true" ]] ; then
     # send output to /dev/null because the logback CaveConsoleAppender will capture that output 
    exec ${CAVE_INSTALL}/cave ${CAVE_INI_ARG} "${SWITCHES[@]}" "${USER_ARGS[@]}" >> /dev/null 2>&1
  else
    # allow output to print to the console/terminal that launched CAVE
    exec ${CAVE_INSTALL}/cave ${CAVE_INI_ARG} "${SWITCHES[@]}" "${USER_ARGS[@]}" 2>&1
  fi
) &

pid=$!
logExitStatus $pid $LOGFILE_STARTUP_SHUTDOWN

