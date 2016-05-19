%define _build_arch %(uname -i)
%define _pgadmin3_version 1.18.1
%define _pgadmin3_build_loc %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)

#
# AWIPS II PostgreSQL Spec File
#

Name: awips2-pgadmin3
Summary: AWIPS II pgadmin3 Distribution
Version: %{_pgadmin3_version}
Release: 2
Group: AWIPSII
BuildRoot: %{_build_root}
BuildArch: %{_build_arch}
URL: N/A
License: N/A
Distribution: N/A
Vendor: Raytheon
Packager: Bryan Kowal

AutoReq: no
BuildRequires: awips2-postgresql
BuildRequires: postgresql 
BuildRequires: postgresql-devel
BuildRequires: postgresql-libs
BuildRequires: wxGTK 
BuildRequires: wxGTK-devel

provides: awips2-pgadmin3
requires: awips2-psql
requires: wxGTK

%description
AWIPS II pgadmin3 Distribution - A custom compilation of the pgadmin3 client compatible with
CentOS / Redhat 5.5.

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
if [ -d %{_pgadmin3_build_loc} ]; then
   rm -rf %{_pgadmin3_build_loc}
fi
mkdir -p %{_pgadmin3_build_loc}

SRC_DIR="%{_baseline_workspace}/rpms/awips2.core/Installer.pgadmin/SOURCES"
PGADMIN_TAR_FILE="pgadmin3-%{_pgadmin3_version}.tar.gz"

mkdir -p %{_build_root}/awips2/pgadmin3

# Copy our source tar file to the build directory.
cp ${SRC_DIR}/${PGADMIN_TAR_FILE} %{_pgadmin3_build_loc}

# Untar the postgresql source
cd %{_pgadmin3_build_loc}

tar -xvf ${PGADMIN_TAR_FILE}

%build

cd %{_pgadmin3_build_loc}/pgadmin3-%{_pgadmin3_version}

export CPPFLAGS="-I/awips2/postgresql/include -I/awips2/postgresql/include/server"
export LDFLAGS="-L/awips2/postgresql/lib"

./configure --prefix=/awips2/pgadmin3
if [ $? -ne 0 ]; then
   exit 1
fi

make
if [ $? -ne 0 ]; then
   exit 1
fi

%install
# Copies the standard Raytheon licenses into a license directory for the
# current component.
function copyLegal()
{
   # $1 == Component Build Root
   
   COMPONENT_BUILD_DIR=${1}
   
   mkdir -p ${RPM_BUILD_ROOT}/${COMPONENT_BUILD_DIR}/licenses
   
   # Create a Tar file with our FOSS licenses.
   tar -cjf %{_baseline_workspace}/rpms/legal/FOSS_licenses.tar \
      %{_baseline_workspace}/rpms/legal/FOSS_licenses/
   
   cp "%{_baseline_workspace}/rpms/legal/Master_Rights_File.pdf" \
      ${RPM_BUILD_ROOT}/${COMPONENT_BUILD_DIR}/licenses
   cp %{_baseline_workspace}/rpms/legal/FOSS_licenses.tar \
      ${RPM_BUILD_ROOT}/${COMPONENT_BUILD_DIR}/licenses
      
   rm -f %{_baseline_workspace}/rpms/legal/FOSS_licenses.tar    
}

cd %{_pgadmin3_build_loc}/pgadmin3-%{_pgadmin3_version}

make install prefix=%{_build_root}/awips2/pgadmin3
if [ $? -ne 0 ]; then
   exit 1
fi

%clean
rm -rf ${RPM_BUILD_ROOT}
rm -rf %{_pgadmin3_build_loc}

%files
%defattr(644,awips,fxalpha,755)
%dir /awips2
%dir /awips2/pgadmin3
%dir /awips2/pgadmin3/share
/awips2/pgadmin3/share/*

%defattr(755,awips,fxalpha,755)
%dir /awips2/pgadmin3/bin
/awips2/pgadmin3/bin/*
