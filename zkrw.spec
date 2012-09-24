%define __NAME__ zkrw
%define __SVN_ROOT__ https://svn.fmpub.net/DataServices

BuildRoot:	%{_tmppath}
Summary:  	ZK command line tool
Name:		dataservices-%{__RUN_ENVIRONMENT__}-%{__NAME__}
Version: 	%{__VERSION__}
Release: 	%{__RELEASE__}
Vendor: 	Federated Media
Group: 		Data Services
License: 	FM
Packager: 	Brian Rogosky <brogosky@federatedmedia.net>  #was: Nicholas Letourneau <nletourneau@federatedmedia.net>
Source: 	%{__NAME__}.tgz
Prefix:	 	/
BuildArch:	noarch

%description 
DEPRECATED .spec file!!!!! For use with RPM deployment, not use devportal.

%prep
# export the SVN tag here
rm -rf %{__TAG__}
svn export %{__SVN_ROOT__}/tags/%{__TAG__} %{__TAG__}

%build
# set up the environment in the config file(s)
cd %{__TAG__}
mvn package

%install
cd %{__TAG__}
mkdir -p $RPM_BUILD_ROOT/usr/local/bin/
mkdir -p $RPM_BUILD_ROOT/opt/zookeeper/conf

cp scripts/zk $RPM_BUILD_ROOT/usr/local/bin/
cp target/%{__BUILD_JAR_NAME__} $RPM_BUILD_ROOT/opt/zookeeper/zkrw.jar
cp scripts/%{__CONF_FILENAME__} $RPM_BUILD_ROOT/opt/zookeeper/conf/zk.conf

%clean
rm -rf %{buildroot}

%files
%attr(755,root,root) /usr/local/bin/zk
%attr(664,root,root) /opt/zookeeper/zkrw.jar
%config %attr(664,datasrv,datasrv) /opt/zookeeper/conf/zk.conf

%post

