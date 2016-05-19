#!/bin/bash

# This is a simple utility script used to determine which specs file should be
# used to build a specified rpm by name.

function lookupRPM()
{
   # Arguments:
   #   ${1} == the name of the rpm.

   # Determine which directory we are running from.
   path_to_script=`readlink -f $0`
   dir=$(dirname $path_to_script)

   rpms_dir=`cd ${dir}/../../../rpms; pwd;`
   if [ $? -ne 0 ]; then
      echo "ERROR: Unable to locate the rpm projects directory."
      exit 1
   fi
   
   export RPM_SPECIFICATION=
   export RPM_PROJECT_DIR=

   awips2_ade_dir="${rpms_dir}/awips2.ade"
   awips2_cave_dir="${rpms_dir}/awips2.cave"
   awips2_core_dir="${rpms_dir}/awips2.core"
   awips2_edex_dir="${rpms_dir}/awips2.edex"
   awips2_qpid_dir="${rpms_dir}/awips2.qpid"
   awips2_upc_dir="${rpms_dir}/awips2.upc"
   python_site__dir="${rpms_dir}/python.site-packages"

   installer_dir="${rpms_dir}/../installers/RPMs"

   # lookup the rpm.
   if [ "${1}" = "awips2-edex-shapefiles" ]; then
      export RPM_SPECIFICATION="${awips2_edex_dir=}/Installer.edex-shapefiles"
      return 0
   fi
   # foss rpms -> python rpms.
   if [ "${1}" = "awips2-maven" ]; then
      export RPM_SPECIFICATION="${installer_dir}/maven-3.2.3/"
      return 0
   fi
   if [ "${1}" = "awips2-python" ]; then
      export RPM_SPECIFICATION="${installer_dir}/python-2.7.9/"
      return 0
   fi
   if [ "${1}" = "awips2-python-cherrypy" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.cherrypy"
      return 0
   fi
   if [ "${1}" = "awips2-python-h5py" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.h5py/"
      return 0
   fi
   if [ "${1}" = "awips2-python-jimporter" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.jimporter"
      return 0
   fi
   if [ "${1}" = "awips2-python-gfe" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.gfe"
      return 0
   fi
   if [ "${1}" = "awips2-python-matplotlib" ]; then
      export RPM_SPECIFICATION="${installer_dir}/matplotlib"
      return 0
   fi
   if [ "${1}" = "awips2-python-nose" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.nose"
      return 0
   fi
   if [ "${1}" = "awips2-python-numpy" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.numpy"

      return 0
   fi
   if [ "${1}" = "awips2-python-jep" ]; then
      export RPM_SPECIFICATION="${installer_dir}/jep"
      return 0
   fi
   if [ "${1}" = "awips2-python-pil" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.pil"
      return 0
   fi
   if [ "${1}" = "awips2-python-pmw" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.pmw"
      return 0
   fi
   if [ "${1}" = "awips2-python-pupynere" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.pupynere"
      return 0
   fi
   if [ "${1}" = "awips2-python-qpid" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.qpid"
      return 0
   fi
   if [ "${1}" = "awips2-python-scientific" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.scientific"
      return 0
   fi
   if [ "${1}" = "awips2-python-scipy" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.scipy"
      return 0
   fi
   if [ "${1}" = "awips2-python-pyparsing" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.pyparsing"
      return 0
   fi
   if [ "${1}" = "awips2-python-basemap" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.basemap"
      return 0
   fi
   if [ "${1}" = "awips2-python-metpy" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.metpy"
      return 0
   fi
   if [ "${1}" = "awips2-python-cartopy" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.cartopy"
      return 0
   fi
   if [ "${1}" = "awips2-python-pint" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.pint"
      return 0
   fi
   if [ "${1}" = "awips2-python-six" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.six"
      return 0
   fi
   if [ "${1}" = "awips2-python-cython" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.cython"
      return 0
   fi
   if [ "${1}" = "awips2-python-cycler" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.cycler"
      return 0
   fi
   if [ "${1}" = "awips2-python-dateutil" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.dateutil"
      return 0
   fi


   if [ "${1}" = "awips2-python-tables" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.tables"
      return 0
   fi
   if [ "${1}" = "awips2-python-tpg" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.tpg"
      return 0
   fi
   if [ "${1}" = "awips2-python-awips" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.python-awips"
      return 0
   fi
   if [ "${1}" = "awips2-python-werkzeug" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.werkzeug"
      return 0
   fi
   if [ "${1}" = "awips2-python-pygtk" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.pygtk"
      return 0
   fi
   if [ "${1}" = "awips2-python-pycairo" ]; then
      export RPM_SPECIFICATION="${python_site__dir}/Installer.pycairo"
      return 0
   fi
   if [ "${1}" = "awips2-python-shapely" ]; then
      export RPM_SPECIFICATION="${installer_dir}/shapely"
      return 0
   fi

   # awips2 rpms.
   if [ "${1}" = "awips2-ncep-database" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.ncep-database"
      return 0
   fi
   if [ "${1}" = "awips2-adapt-native" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.adapt-native"
      return 0
   fi
   if [ "${1}" = "awips2-aviation-shared" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.aviation"
      return 0
   fi
   if [ "${1}" = "awips2-cli" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.cli"
      return 0
   fi
   if [ "${1}" = "awips2-database" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.database"
      return 0
   fi
   if [ "${1}" = "awips2-database-standalone-configuration" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.database-standalone-configuration"
      return 0
   fi
   if [ "${1}" = "awips2-gfesuite-client" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.gfesuite-client"
      return 0
   fi
   if [ "${1}" = "awips2-gfesuite-server" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.gfesuite-server"
      return 0
   fi
   if [ "${1}" = "awips2-hydroapps-shared" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.hydroapps"
      return 0
   fi
   if [ "${1}" = "awips2-localapps-environment" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.localapps-environment"
      return 0
   fi
   if [ "${1}" = "-localization" ]; then
      return 0
   fi
   if [ "${1}" = "awips2-maps-database" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.maps-database"
      return 0
   fi
   if [ "${1}" = "awips2-notification" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.notification"
      return 0
   fi
   if [ "${1}" = "awips2-pypies" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.pypies"
      return 0
   fi
   if [ "${1}" = "awips2-rcm" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.rcm"
      return 0
   fi
   if [ "${1}" = "awips2-data.hdf5-topo" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.topo"
      return 0
   fi
   if [ "${1}" = "awips2-data.gfe" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.gfe"
      return 0
   fi
   if [ "${1}" = "awips2" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.version"
      return 0
   fi
   if [ "${1}" = "awips2-common-base" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.common-base"
      return 0
   fi

   # foss rpms.
   if [ "${1}" = "-qpid" ]; then
      return 0
   fi
   if [ "${1}" = "awips2-ant" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.ant"
      return 0
   fi
   if [ "${1}" = "awips2-httpd-pypies" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.httpd-pypies"
      return 0
   fi
   if [ "${1}" = "awips2-java" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.java"
      return 0
   fi
   if [ "${1}" = "awips2-groovy" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.groovy"
      return 0
   fi
   if [ "${1}" = "awips2-ldm" ]; then
      export RPM_SPECIFICATION="${awips2_upc_dir}/Installer.ldm"
      return 0
   fi
   if [ "${1}" = "awips2-edex-upc" ]; then
      export RPM_SPECIFICATION="${awips2_upc_dir}/Installer.edex-upc"
      return 0
   fi
   if [ "${1}" = "awips2-postgres" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.postgres"
      return 0
   fi
   if [ "${1}" = "awips2-pgadmin3" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.pgadmin"
      return 0
   fi
   if [ "${1}" = "awips2-tools" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.tools"
      return 0
   fi
   if [ "${1}" = "awips2-eclipse" ]; then
      export RPM_SPECIFICATION="${awips2_ade_dir}/Installer.eclipse"
      return 0
   fi
   if [ "${1}" = "awips2-openfire" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.xmpp"
      return 0
   fi
   if [ "${1}" = "awips2-collab-dataserver" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.collab-dataserver"
      return 0
   fi
   if [ "${1}" = "awips2-yajsw" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.yajsw"
      return 0
   fi

   # awips2 rpms -> viz rpms.
   if [ "${1}" = "awips2-alertviz" ]; then
      export RPM_SPECIFICATION="${awips2_cave_dir}/Installer.alertviz"
      return 0
   fi
   if [ "${1}" = "-cave" ]; then
      return 0
   fi

   # awips2 rpms -> edex rpms.
   if [ "${1}" = "-edex" ]; then
      return 0
   fi
   if [ "${1}" = "awips2-edex-environment" ]; then
      export RPM_SPECIFICATION="${awips2_core_dir}/Installer.edex-environment/edex"
      return 0
   fi

   return 1
}
