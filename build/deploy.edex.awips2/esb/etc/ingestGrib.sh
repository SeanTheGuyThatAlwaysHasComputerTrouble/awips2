#!/bin/bash
##
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
##
numCores=`grep -c ^processor /proc/cpuinfo`

let "GRIB_DECODE_THREADS=numCores / 2"

if [ $GRIB_DECODE_THREADS -gt 12 ]; then
    GRIB_DECODE_THREADS=12
elif [ $GRIB_DECODE_THREADS -lt 4 ]; then
    GRIB_DECODE_THREADS=4
fi

# sets bounds based on # of threads available
let "MAX_MEM = GRIB_DECODE_THREADS * 128" # in Meg
let "GRIB_MAX_GRID_POINTS = GRIB_DECODE_THREADS * 2000000"
let "GRIB_PERSIST_THREADS = GRIB_DECODE_THREADS / 2"
let "GRIB_MAX_PERSIST_MEMORY_IN_MB = GRIB_PERSIST_THREADS * 50"

export INIT_MEM=128 # in Meg
export MAX_MEM
export GRIB_MAX_GRID_POINTS
export GRIB_PERSIST_THREADS
export GRIB_MAX_PERSIST_MEMORY_IN_MB
export GRIB_DECODE_THREADS

export METADATA_POOL_MAX=10
export EDEX_DEBUG_PORT=5007
export EDEX_JMX_PORT=1618
export MGMT_PORT=9603

