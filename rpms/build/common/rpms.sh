#!/bin/bash

function buildOpenfire()
{
   lookupRPM "awips2-openfire"
   if [ $? -ne 0 ]; then
      echo "ERROR: '${1}' is not a recognized AWIPS II RPM."
      exit 1
   fi

   /usr/bin/rpmbuild -ba --target=i386 \
      --define '_topdir %(echo ${AWIPSII_TOP_DIR})' \
      --define '_baseline_workspace %(echo ${WORKSPACE})' \
      --define '_uframe_eclipse %(echo ${UFRAME_ECLIPSE})' \
      --define '_awipscm_share %(echo ${AWIPSCM_SHARE})' \
      --define '_build_root %(echo ${AWIPSII_BUILD_ROOT})' \
      --define '_component_version %(echo ${AWIPSII_VERSION})' \
      --define '_component_release %(echo ${AWIPSII_RELEASE})' \
      --define '_component_build_date %(echo ${COMPONENT_BUILD_DATE})' \
      --define '_component_build_time %(echo ${COMPONENT_BUILD_TIME})' \
      --define '_component_build_system %(echo ${COMPONENT_BUILD_SYSTEM})' \
      --buildroot ${AWIPSII_BUILD_ROOT} \
      ${RPM_SPECIFICATION}/component.spec
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build RPM ${1}."
      exit 1
   fi

   return 0
}

function buildJava()
{
   pushd . > /dev/null 2>&1

   cd ${WORKSPACE}/rpms/awips2.core/Installer.java
   /bin/bash build.sh
   if [ $? -ne 0 ]; then
      return 1
   fi

   popd > /dev/null 2>&1
}

function buildQPID()
{
   # Arguments:
   #   ${1} == optionally -ade

   pushd . > /dev/null 2>&1

   # ensure that the destination rpm directories exist
   if [ ! -d ${AWIPSII_TOP_DIR}/RPMS/noarch ]; then
      mkdir -p ${AWIPSII_TOP_DIR}/RPMS/noarch
      if [ $? -ne 0 ]; then
         exit 1
      fi
   fi

   # ensure that the destination rpm directories exist
   if [ ! -d ${AWIPSII_TOP_DIR}/RPMS/x86_64 ]; then
      mkdir -p ${AWIPSII_TOP_DIR}/RPMS/x86_64
      if [ $? -ne 0 ]; then
         exit 1
      fi
   fi
   
   cd ${WORKSPACE}/installers/RPMs/qpid-lib
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the qpid rpms."
      return 1
   fi

   /bin/bash build.sh
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the qpid rpms."
      return 1
   fi

   #build 
   export AWIPS_II_TOP_DIR
   cd ${WORKSPACE}/installers/RPMs/qpid-java-broker/
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build Qpid Broker"
      echo "could not cd to ${WORKSPACE}/installers/RPMs/qpid-java-broker/"
      return 1
   fi
   /bin/bash build.sh
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build Qpid Broker"
      echo "build.sh failed"
      return 1
   fi

   popd > /dev/null 2>&1

   return 0
}

function buildEDEX()
{
   cd ${WORKSPACE}/rpms/awips2.edex/deploy.builder
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the edex rpms."
      return 1
   fi

   # Determine the build architecture.
   export EDEX_BUILD_ARCH=`uname -i`
   if [ "${EDEX_BUILD_ARCH}" = "i386" ]; then
      export EDEX_BUILD_ARCH="x86"
   fi

   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to determine the architecture."
      return 1
   fi
   /bin/bash build.sh
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the edex rpms."
      return 1
   fi

   return 0
}

function buildCAVE()
{
   cd ${WORKSPACE}/rpms/awips2.cave/deploy.builder
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the cave rpms."
      return 1
   fi

   # Determine the build architecture.
   export CAVE_BUILD_ARCH=`uname -i`
   if [ "${CAVE_BUILD_ARCH}" = "i386" ]; then
      export CAVE_BUILD_ARCH="x86"
   fi

   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to determine the architecture."
      return 1
   fi
   /bin/bash build.sh
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the cave rpms."
      return 1
   fi

   return 0
}

function buildLocalizationRPMs()
{
   awips2_core_directory=${WORKSPACE}/rpms/awips2.core
   installer_localization_directory="${awips2_core_directory}/Installer.localization"
   localization_SPECIFICATION="${installer_localization_directory}/component.spec"

   export LOCALIZATION_DIRECTORY="localization"
   export COMPONENT_NAME="awips2-localization"

   echo "Building localization rpm"

   rpmbuild -ba \
      --define '_topdir %(echo ${AWIPSII_TOP_DIR})' \
      --define '_component_version %(echo ${AWIPSII_VERSION})' \
      --define '_component_release %(echo ${AWIPSII_RELEASE})' \
      --define '_component_name %(echo ${COMPONENT_NAME})' \
      --define '_baseline_workspace %(echo ${WORKSPACE})' \
      --define '_localization_directory %(echo ${LOCALIZATION_DIRECTORY})' \
      --buildroot ${AWIPSII_BUILD_ROOT} \
      ${localization_SPECIFICATION}
   RC=$?
   unset LOCALIZATION_DIRECTORY
   unset COMPONENT_NAME
   if [ ${RC} -ne 0 ]; then
      return 1
   fi

   return 0
}

function unpackHttpdPypies()
{
   echo ""
   echo "unpackHttpdPypies"
   echo ""
   # This function will unpack the httpd-pypies SOURCES
   # into the: ${AWIPSII_TOP_DIR}/SOURCES directory.
   awips2_core_directory=${WORKSPACE}/rpms/awips2.core
   httpd_pypies_directory=${awips2_core_directory}/Installer.httpd-pypies
   echo httpd_pypies_directory=${httpd_pypies_directory}
   httpd_SOURCES=${httpd_pypies_directory}/src/httpd-2.4.6-SOURCES.tar
   #httpd_SOURCES=${httpd_pypies_directory}/src/httpd-2.2.15-SOURCES.tar
   #httpd_SOURCES=${httpd_pypies_directory}/src/httpd-2.2.31.tar
   echo httpd_SOURCES=${httpd_SOURCES}

   /bin/tar -xvf ${httpd_SOURCES} -C ${AWIPSII_TOP_DIR}/SOURCES
   if [ $? -ne 0 ]; then
      return 1
   fi
   cp -vf ${httpd_pypies_directory}/SOURCES/* ${AWIPSII_TOP_DIR}/SOURCES
   if [ $? -ne 0 ]; then
      return 1
   fi

   return 0
}
function buildShapefiles()
{
   cd ${WORKSPACE}/rpms/awips2.shapefiles/deploy.builder
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the edex shapefile rpm."
      return 1
   fi

   # Determine the build architecture.
   export EDEX_BUILD_ARCH=`uname -i`
   if [ "${EDEX_BUILD_ARCH}" = "i386" ]; then
      export EDEX_BUILD_ARCH="x86"
   fi

   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to determine the architecture."
      return 1
   fi
   /bin/bash build.sh
   if [ $? -ne 0 ]; then
      echo "ERROR: Failed to build the edex shapefile rpm."
      return 1
   fi

   return 0
}
