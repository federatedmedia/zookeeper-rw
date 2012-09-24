Summary: ZK command line tool
Name: %{__NAME__}
Version: %{__VERSION__}
Release: %{__RELEASE__}
Vendor: Federated Media
Group: Data Services
License: FM
Packager: %{__PKGR_NAME__} %{__PKGR_EMAIL__}
Source: %{__NAME__}.tar
Prefix: %{__DEPLOY_DIR__}/%{__APP_DIR__}
Provides: %{__APP_DIR__}
BuildArch: noarch
BuildRoot: %{_tmppath}
Epoch: %{__TIMESTAMP__}

%description
A stand-alone Java application which provides a command-line interface for Zookeeper.

%prep
%setup -n %{__NAME__}

%build

%install
mkdir -p $RPM_BUILD_ROOT%{prefix}
cp -Ra * $RPM_BUILD_ROOT%{prefix}

if [ ! -d $RPM_BUILD_ROOT/usr/local/bin/ ]; then
	mkdir -p $RPM_BUILD_ROOT/usr/local/bin/
fi
if [ ! -d $RPM_BUILD_ROOT/%{__CONF_DIR__} ]; then
	mkdir -p $RPM_BUILD_ROOT/%{__CONF_DIR__}
fi

SCRIPT_DIR="$RPM_BUILD_ROOT%{prefix}/scripts"
cp -f ${SCRIPT_DIR}/zk $RPM_BUILD_ROOT/usr/local/bin/
cp -f ${SCRIPT_DIR}/%{__CONF_DEFAULT_FILE__} $RPM_BUILD_ROOT/%{__CONF_DEST_FILE__}

%clean
rm -rf %{buildroot}

%files
%defattr(-,datasrv,datasrv,-)
%{prefix}
%attr(755,root,root) /usr/local/bin/zk
%attr(664,root,root) /opt/zookeeper/%{__JAR_NAME__}
%config %attr(664,datasrv,datasrv) %{__CONF_DEST_FILE__}

%pre

%post
./%{__DEPLOY_DIR__}/%{__APP_DIR__}/datasrv-tupelo-post.sh "%{__DEPLOY_DIR__}/%{__APP_DIR__}"
rm -f %{__DEPLOY_DIR__}/%{__APP_DIR__}/datasrv-tupelo-post.sh

