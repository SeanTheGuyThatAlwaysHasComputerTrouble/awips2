#
# AWIPS II Adapt Native Spec File
#
Name: awips2-adapt-native
Summary: AWIPS II Adapt Native Distribution
Version: %{_component_version}
Release: %{_component_release}
Group: AWIPSII
BuildRoot: /tmp
Prefix: /awips2
URL: N/A
License: N/A
Distribution: N/A
Vendor: Raytheon
Packager: Bryan Kowal

AutoReq: no
provides: awips2-adapt-native
requires: awips2-edex-base

# The two python native libraries are now included directly in the python
# distribution that is built.
%description
AWIPS II Native Distribution - Contains the AWIP II Native Files for Adapt.

# Turn off the brp-python-bytecompile script
%global __os_install_post %(echo '%{__os_install_post}' | sed -e 's!/usr/lib[^[:space:]]*/brp-python-bytecompile[[:space:]].*$!!g')

%prep
# Verify That The User Has Specified A BuildRoot.
if [ "${RPM_BUILD_ROOT}" = "/tmp" ]
then
   echo "An Actual BuildRoot Must Be Specified. Use The --buildroot Parameter."
   echo "Unable To Continue ... Terminating"
   exit 1
fi

mkdir -p ${RPM_BUILD_ROOT}/awips2

%build

%install
NATIVE_TAR_FILE="dist.native/i386-pc-linux-gnu.tar"

/bin/gtar -xpf %{_baseline_workspace}/${NATIVE_TAR_FILE} \
   -C ${RPM_BUILD_ROOT}/awips2

# Remove Unnecessary Directories.
rm -rf ${RPM_BUILD_ROOT}/awips2/setup
rm -rf ${RPM_BUILD_ROOT}/awips2/edex
rm -rf ${RPM_BUILD_ROOT}/awips2/lib
rm -rf ${RPM_BUILD_ROOT}/awips2/awipsShare

%pre

%post

%preun

%postun

%clean
rm -rf ${RPM_BUILD_ROOT}

%files
%defattr(644,awips,fxalpha,755)
%dir /awips2/adapt
%dir /awips2/adapt/climate
%dir /awips2/adapt/climate/data
/awips2/adapt/climate/data/*
%dir /awips2/adapt/climate/bin

%defattr(755,awips,fxalpha,755)
%dir /awips2/adapt/climate/bin/Linux
/awips2/adapt/climate/bin/Linux/*