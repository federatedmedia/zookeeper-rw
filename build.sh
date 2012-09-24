#!/bin/bash
echo "DEPRECATED!"
# DEPRECATED.  Used for build and push of the RPM. Replaced with build-tag.sh

# Builds the rpm for zkrw using svn or git-svn.
# Notes:
# * The zkrw project uses project/tags and project/branches structure instead
# of tags off of the root.
#
# By Nick and later Brian


#
# Functions
#
usage()
{
cat << EOF
usage: $0 options

This script tags and builds and rpm for zkrw from within a git svn environment
OPTIONS:
   -h      Show this message
   -e      Environment can be PRD, QA, or DEV
   -r      Release number
   -v      Release name and version
   -s      Skip tag creation (optional)
   -g      Use git-svn instead of svn (optional)
   -n      Skip unit tests (optional)
Example:
    $0 -e QA -v TESTING_01 -r 01

EOF
}

copyToSvnTag() {
	fromFolder=$1
	toFolder=$2
	echo "Copying ${fromFolder} to the tag."
  	svn copy "${SVN_BASE}/${fromFolder}" "${SVN_TAG_BASE}/${TAG}/${toFolder}" -m "tagging ${toFolder} to tag ${TAG} for ${APP_NAME} release"
	
}

# FROM Roger (see push_etl_code.sh)
# returns 0 if a tag exists, 1 otherwise
# usage
# EXISTS=$(svn_exists DOLPHIN_01_99 tags)
# EXISTS=$(svn_exists DOLPHIN_01 releases)
# if [ "$EXISTS" == "0" ]; then
#   echo "99 exists
# fi
svn_exists()
{
  svn ls ${SVN_BASE}/$2/$1 2>&1 > /dev/null
  local SVN_EXISTS=$?
  echo $SVN_EXISTS
}

svn_checkoutTag() {

	cd $TMP_DIR && \
	echo "svn checkout $SVN_TAG_BASE/${TAG} ${TAG}" && \
	svn checkout --force $SVN_TAG_BASE/${TAG} ${TAG}

}

#TMP_DIR=/tmp/zkrw_push
TMP_DIR=/media/truecrypt2/build-temp-zkrw

#
# Constants. Should not need to change.
#
SVN_BASE="https://svn.fmpub.net/DataServices"
SVN_TAG_BASE="$SVN_BASE/tags"
RELEASE_REPO="http://nexus.ds:8080/content/repositories/releases"
FINAL_JAR_NAME=zkrw.jar

# 
# Parameters
#
SKIP_TAG=false
USE_VC=svn
BUILD_JAR_NAME=
ENVIRONMENT=
TAG=
RELEASE=
VERSION=
TEST_MVN_OPT=""

while getopts "hsgte:r:v:" OPTION
do
  case $OPTION in
    h)
      usage
      exit 1
      ;;
    e)
      ENVIRONMENT=$OPTARG
      ;;
    v)
      VERSION=$OPTARG
      ;;
    r)
      RELEASE=$OPTARG
      ;;
    s)
      SKIP_TAG=true
      ;;
    g)
      USE_VC=git
      ;;
    t)
      TEST_MVN_OPT="-Dmaven.test.skip=true"   # skip testing
      ;;
    ?)
      usage
      exit 1
    ;;
  esac
done
TAG=${VERSION}_${RELEASE}
BUILD_JAR_NAME=zkrw-${TAG}-jar-with-dependencies.jar

echo -e "----------------"
echo -e "Build Parameters\n SKIP_TAG=$SKIP_TAG \n USE_VC=$USE_VC \n BUILD_JAR_NAME=$BUILD_JAR_NAME \n ENVIRONMENT=$ENVIRONMENT \n TAG=$TAG \n RELEASE=$RELEASE \n TEST_MVN_OPT=$TEST_MVN_OPT "
echo -e "----------------"

if [[ -z "$RELEASE" || -z "$VERSION" || -z "$ENVIRONMENT" ]]
then
     usage
     exit 1
fi

if [[ "${ENVIRONMENT}" == "PRD" ]]; then
  CONF_FILENAME=zk.prd.conf
elif [[ "${ENVIRONMENT}" == "QA" ]]; then
  CONF_FILENAME=zk.qa.conf
elif [[ "${ENVIRONMENT}" == "DEV" ]]; then
  CONF_FILENAME=zk.dev.conf
else
    echo "Invalid operating environment (${ENVIRONMENT})"
    exit 1
fi && \

ORIGINAL_PATH="$(pwd)"


if $SKIP_TAG; then
    echo "skipping tag creation and mvn deploy"
    if [ -d "${TMP_DIR}/${TAG}" ]; then
	echo "Using existing build from tag: ${TMP_DIR}/${TAG}" && \
	cd ${TMP_DIR}/${TAG}
    else
	echo "Tag directory does not exist locally: ${TMP_DIR}/${TAG}" && \
	svn_checkoutTag
	cd ${TAG}	
    fi	
else
    echo "starting tagging..."

    if [ -d "${TMP_DIR}" ]; then
	echo "Removing temp dir contents: ${TMP_DIR}" && \
	rm -rf "${TMP_DIR}"
    fi && \
	
    mkdir -p $TMP_DIR && \
    if [ "$USE_VC" == "svn" ]; then
	TAG_EXISTS=$(svn_exists $TAG tags)
	
	if [[ "$TAG_EXISTS" != "0" ]]; then
		
		echo "creating tag ${TAG}"
		svn mkdir --parents "${SVN_TAG_BASE}/${TAG}" -m "new tag for zkrw release"

	else 
		echo "WARNING: tag $TAG already exists, so it will not be recreated." 
		
	fi && \

    	copyToSvnTag "trunk/zkrw/scripts" "scripts" && \
    	copyToSvnTag "trunk/zkrw/src" "src" && \
    	copyToSvnTag "trunk/zkrw/pom.xml" "pom.xml" && \

	svn_checkoutTag
	cd ${TAG}
    else 
    	git svn tag $TAG && \
    	git checkout $TAG
    fi && \
    
    sed "s:<version>.*-SNAPSHOT</version>:<version>${TAG}</version>:" pom.xml > pom.xml.new && \
    mv pom.xml.new pom.xml && \

    if [ "$USE_VC" == "svn" ]; then
	svn commit -m "adding tag name to pom.xml"
    else 
	git add pom.xml && \
	git commit -m "adding tag name to pom.xml" && \
	git svn dcommit
    fi && \

    echo ">> Running: mvn deploy $TEST_MVN_OPT ..." && \
    mvn deploy $TEST_MVN_OPT && \

    if [ "$USE_VC" == "svn" ]; then
    	if [ "$(svn_exists $TAG zkrw/tags)" != "0" ]; then
		(echo "failed to create tag ${TAG}" && exit 1)
	fi
    
    else 
	git checkout master || \
	    (echo "failed to create tag ${TAG}" && exit 1)
    fi

    echo -e "Build complete."

fi && \

echo -e "Starting rpm creation."

mkdir -p $TMP_DIR/SOURCES && \
mkdir -p $TMP_DIR/SRPMS && \
mkdir -p $TMP_DIR/BUILD && \
mkdir -p $TMP_DIR/RPMS && \
mkdir -p $TMP_DIR/INSTALL && \

rpmbuild -vvv --buildroot $TMP_DIR/INSTALL --define="_topdir $TMP_DIR" --define="__NAME__ zkrw" --define="__RELEASE__ $RELEASE" --define="__TAG__ $TAG" --define="__VERSION__ $VERSION" --define="__RUN_ENVIRONMENT__ $ENVIRONMENT" --define="__CONF_FILENAME__ $CONF_FILENAME" --define="__BUILD_JAR_NAME__ $BUILD_JAR_NAME" -bb scripts/zkrw.spec && \
echo "Copying RPM to $ORIGINAL_PATH/."
cp $TMP_DIR/RPMS/noarch/datasrv-${ENVIRONMENT}-zkrw-${VERSION}-${RELEASE}.noarch.rpm $ORIGINAL_PATH/. && \
echo "Complete."
