%define _groovy_version 2.1.3
#
# AWIPS II Groovy Spec File
#
Name: awips2-groovy
Summary: AWIPS II Groovy Distribution
Version: %{_groovy_version}
Release: %{_component_version}.%{_component_release}
Group: AWIPSII
BuildRoot: %{_build_root}
BuildArch: noarch
URL: N/A
License: N/A
Distribution: N/A
Vendor: Raytheon
Packager: Bryan Kowal

AutoReq: no
provides: awips2-groovy
requires: awips2-java

%description
AWIPS II Groovy Distribution - Contains Groovy %{_groovy_version}.

%prep
# Ensure that a "buildroot" has been specified.
if [ "%{_build_root}" = "" ]; then
   echo "ERROR: A BuildRoot has not been specified."
   echo "FATAL: Unable to Continue ... Terminating."
   exit 1
fi

if [ -d %{_build_root} ]; then
   rm -rf %{_build_root}
   if [ $? -ne 0 ]; then
      exit 1
   fi
fi

%build

%install
mkdir -p %{_build_root}/awips2
if [ $? -ne 0 ]; then
   exit 1
fi

RPMS_DIRECTORY="%{_baseline_workspace}/rpms"
INSTALLER_GROOVY="${RPMS_DIRECTORY}/awips2.core/Installer.groovy"

_groovy_dist="${INSTALLER_GROOVY}/src/groovy-binary-%{_groovy_version}.zip"

unzip ${_groovy_dist} -d %{_build_root}/awips2
if [ $? -ne 0 ]; then
   exit 1
fi
mv  %{_build_root}/awips2/groovy-%{_groovy_version} \
   %{_build_root}/awips2/groovy
if [ $? -ne 0 ]; then
   exit 1
fi

%clean
rm -rf ${RPM_BUILD_ROOT}

%files
%defattr(644,awips,fxalpha,755)

%dir /awips2/groovy
%dir /awips2/groovy/conf
/awips2/groovy/conf/*
%dir /awips2/groovy/embeddable
/awips2/groovy/embeddable/*
%dir /awips2/groovy/indy
/awips2/groovy/indy/*
%dir /awips2/groovy/lib
/awips2/groovy/lib/*
%dir /awips2/groovy/bin

%doc /awips2/groovy/ANTLR-LICENSE.txt
%doc /awips2/groovy/ASM-LICENSE.txt
%doc /awips2/groovy/CLI-LICENSE.txt
%doc /awips2/groovy/JSR223-LICENSE.txt
%doc /awips2/groovy/LICENSE.txt
%doc /awips2/groovy/NOTICE.txt

%defattr(755,awips,fxalpha,755)
/awips2/groovy/bin/*
