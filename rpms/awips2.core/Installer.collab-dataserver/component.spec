%define _ant_opts -Dfile.encoding=iso-8859-1 -Dworkspace='..' -Dworkspace_edexOsgi='..' -Dworkspace_javaUtilities='..' -Dworkspace_cots='..' -Ddataserver.root.directory='${RPM_BUILD_ROOT}/awips2/collab-dataserver'

Name: awips2-collab-dataserver
Summary: AWIPSII Collaboration HTTP Dataserver
Version: %{_component_version}
Release: %{_component_release}%{?dist}
Group: AWIPSII
BuildRoot: %{_build_root}
BuildArch: %{_build_arch}
URL: N/A
License: N/A
Distribution: N/A
Vendor: Raytheon
Packager: Bradley Gonzales

AutoReq: no
provides: awips2-collab-dataserver
requires: awips2-java, awips2-ant

%description
%{_component_desc}

%prep
# Ensure that a "buildroot" has been specified.
if [ "%{_build_root}" = "" ]; then
   echo "ERROR: A BuildRoot has not been specified."
   echo "FATAL: Unable to Continue ... Terminating."
   exit 1
fi

if [ -d %{_build_root} ]; then
   rm -rf %{_build_root}
fi
/bin/mkdir -p %{_build_root}
if [ $? -ne 0 ]; then
   exit 1
fi

%build
cd %{_baseline_workspace}/collaboration.dataserver
export ANT_OPTS="%{_ant_opts}"
/awips2/ant/bin/ant -f build.xml build
if [ $? -ne 0 ]; then
   exit 1
fi

%install
dataserver_project="%{_baseline_workspace}/rpms/awips2.core/Installer.collab-dataserver"
config_directory="${dataserver_project}/configuration"
cd %{_baseline_workspace}/collaboration.dataserver
export ANT_OPTS="%{_ant_opts}"
/awips2/ant/bin/ant -f build.xml deploy 
if [ $? -ne 0 ]; then
   exit 1
fi

cp -r ${config_directory}/etc ${RPM_BUILD_ROOT}

%pre
%post
chmod ug+x /awips2/collab-dataserver/bin/*.sh
chmod 755 /etc/init.d/collab-dataserver
/sbin/chkconfig --add collab-dataserver

%preun
# Remove and unregister the collab-dataserver service.
if [ -f /etc/init.d/collab-dataserver ]; then
   /sbin/chkconfig collab-dataserver off
   /sbin/chkconfig --del collab-dataserver

   rm -f /etc/init.d/collab-dataserver
fi

%postun

%clean
cd %{_baseline_workspace}/collaboration.dataserver
export ANT_OPTS="%{_ant_opts}"
/awips2/ant/bin/ant -f build.xml clean
if [ $? -ne 0 ]; then
   exit 1
fi
rm -rf ${RPM_BUILD_ROOT}

%files
%defattr(644,awips,fxalpha,755)
%dir /awips2
%dir /awips2/collab-dataserver
%dir /awips2/collab-dataserver/bin
/awips2/collab-dataserver/bin/*
%dir /awips2/collab-dataserver/lib
/awips2/collab-dataserver/lib/*

%defattr(775,awips,fxalpha,775)
%dir /awips2/collab-dataserver/lib/uframe
/awips2/collab-dataserver/lib/uframe/*
%dir /awips2/collab-dataserver/lib/foss
/awips2/collab-dataserver/lib/foss/*
%dir /awips2/collab-dataserver/config
/awips2/collab-dataserver/config/*

%defattr(755,root,root,755)
/etc/init.d/collab-dataserver
