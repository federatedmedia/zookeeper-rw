#!/bin/bash
#
# a simple script that could be used to do more in the future without changing the spec file
#
APP_PATH="$1" # from %{__DEPLOY_DIR__}/%{__APP_DIR__}
SCRIPT_DIR="${APP_PATH}/scripts"
SUB_HOSTNAME=`hostname |cut -f2- -d.`
CONF_DIR="/opt/zookeeper/conf"
CONF_FILE_NAME="zk.conf"

# determine which configuration to use based on the hostname. Note: The target hostnames
# are in one line of the .conf file
cd $CONF_DIR
find ${SCRIPT_DIR} -name "*.conf" 2>/dev/null | while read f; do
	echo "Checking $f for ${SUB_HOSTNAME}..."
	matchn=`grep -i -c "${SUB_HOSTNAME}" $f`
	if [ $matchn -eq 1 ]; then
		echo "Found $SUB_HOSTNAME in $f. Linking $f to ${CONF_FILE_NAME} in ${CONF_DIR}"
		ln -f -s $f ${CONF_FILE_NAME}
		FOUND="1"
	fi
done

rm -f ${APP_PATH}/datasrv-tupelo.macro ${APP_PATH}/datasrv-tupelo.spec;
