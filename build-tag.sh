#!/bin/bash
# build and push script for zkrw (Zookeeper client) releases


# misc paths
SVN_REPO="https://svn.fmpub.net/DataServices"

APP_ROOT="zkrw" # directory inside code tree
APP_NAME="zkrw" # name of app

# base path for building the application. Should be a temporary directory.
# it can be empty or non-existant in which case the necessary SVN folders will be checked out.
# If it is not empty, then svn update will be done for each required folder.
# NOTE: If it is not empty, and there are conflicts in the working directory, the build may not work. So the best practice is to use a directory only for checkout/update, not commits.
LOCAL_BUILD_DIR="/media/truecrypt2/dev-build_$APP_NAME" 

APP_ROOT_PATH="$LOCAL_BUILD_DIR/$APP_ROOT"
TARGET_JAR_NAME="zkrw.jar"
LOCAL_BUILD_APP_SUBDIR="$APP_ROOT_PATH"
RPM_SPEC_NAME="datasrv-tupelo.spec"
RPM_MAC_NAME="datasrv-tupelo.macro"
RPM_POST_NAME="datasrv-tupelo-post.sh"

#
# Functions
#

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
  svn ls ${SVN_REPO}/$2/$1 2>&1 > /dev/null
  local SVN_EXISTS=$?
  echo $SVN_EXISTS
}

usage() {

	echo "Usage:"
	echo "$0 -v version -t tag [-f]"
	echo "e.g. $0 -v TUPELO_01 -t 03"
	echo -e "Options \n"
	echo -e "\t -f \t To force a new build and tag creation even if the tag for this version and tag already exists."
	echo -e "\nUsage: $0 -h \n\t\t Print this help."
}

# Checkout or Update a working directory from SVN.
updateSvnFolder() {
	path="$1"
	
	if [ ! -d "$path" ]; then
		echo "Local code path \"$path\" does not exists. Creating it now."
		mkdir --parents "$path"
	fi

	folder="$2"
	folderPath="$path/$folder"

	svnPath="$3"

	if [ ! -d "$folderPath" ]; then
		echo "Subversion working directory \"$folderPath\" does not exists. Running svn checkout for this directory:"
		echo "svn checkout $svnPath/$folder $folderPath"
		svn checkout --force $svnPath/$folder $folderPath
	else
		echo "Updating subversion working directory: $folderPath"	
		cd $folderPath
		svn up

	fi

}

#
# Will copy one or more SVN paths from Working Copy OR URL to the deployment tag
#
copyToTagForRPM() {
	local svnPath="$1"
	local msg="adding ${svnPath} for ${TAG_NAME} release"
	echo "$msg"
	svn copy ${svnPath} ${SVN_REPO}/tags/${TAG_NAME} -m "$msg"

}

#
# Will copy one or more SVN paths from Working Copy OR URL to the pre-build tag
#
copyToTagForPre() {
	local svnPath="$1"
	local msg="adding ${svnPath} for ${TAG_PREBUILD_NAME}"
	echo "$msg"
	svn copy ${svnPath} ${SVN_REPO}/tags/${TAG_PREBUILD_NAME} -m "$msg"
}

# CONSTANTS
# errors
E_TAG_EXISTS=127 # you asked to create a tag but it already exists
E_TAG_DOES_NOT_EXIST=126 # you didn't ask to create a tag but it doesn't exist
E_BAD_OPTION=124 # bad command-line option
E_CANNOT_FIND_CODE=123 # code repo (app path) not found


####################################
# variables specific to this release
####################################
VERSION=  # e.g. JACARANDA_01
TAG= # e.g. 03
ENVIRONMENT=
FORCE="no"

###############################
# handle command-line arguments
###############################

# get command-line options (tag, version, target, etc)
while getopts ":t:v:hf" OPTION 
do
  case $OPTION in
    
    h)
      usage
      exit 0
      ;;
    f)
      FORCE="yes"
      ;;
    t)
      TAG=$OPTARG
      ;;
    v)
      VERSION=$OPTARG
      ;;
    	
    ?)
      echo "Invalid option"
      usage
      exit $E_BAD_OPTION
  esac
done

####################
# validate arguments
####################
if [[ -z $TAG ]] || [[ -z $VERSION ]]; then
  usage
  exit $E_BAD_OPTION
fi



###############################

TAG_NAME="${VERSION}_${TAG}"
TAG_PREBUILD_NAME="${TAG_NAME}_code"

BUILD_JAR_NAME="zkrw-${TAG_NAME}-SNAPSHOT-jar-with-dependencies.jar"
BUILD_JAR_PATH="$APP_ROOT_PATH/target/$BUILD_JAR_NAME"
#COMMIT_TARBALL="false"
MVN_ACTION="package"

VERSION_FILE_NAME="version"

echo "Running $0 ..."
echo " version $VERSION"
echo " tag $TAG"
echo " force build & tag ${FORCE}"


echo "checking if svn tag exists: ${SVN_REPO}/tags/$TAG_NAME"
TAG_EXISTS=$(svn_exists $TAG_NAME tags)

if [[ "$FORCE" == "yes" ]] || [[ "$TAG_EXISTS" != "0" ]]; then
	# if the tag already exists, warn
	if [ "$TAG_EXISTS" == "0" ]; then
	  echo "WARNING: tag $TAG_NAME already exists, but you passed the -f option. The tag will be deleted and a new build created."
	  svn delete ${SVN_REPO}/tags/$TAG_NAME -m "removing old tag $TAG_NAME while re-building."
	
	fi && \

	echo "creating tag: ${TAG_NAME}" && \
	svn mkdir --parents "${SVN_REPO}/tags/${TAG_NAME}" -m "new tag ${TAG_NAME} for ${APP_NAME} release" && \

	####################################################################
	# cut a new tag prior to build for all source files and projects
	####################################################################
	if [ "$(svn_exists ${TAG_PREBUILD_NAME} tags)" == "0" ]; then
		echo "WARNING: tag ${TAG_PREBUILD_NAME}. It will be deleted first." && \
		svn delete ${SVN_REPO}/tags/${TAG_PREBUILD_NAME} -m "removing old tag ${TAG_PREBUILD_NAME} while re-building."
	fi && \
	echo "creating tag: ${TAG_PREBUILD_NAME}" && \
	svn mkdir --parents "${SVN_REPO}/tags/${TAG_PREBUILD_NAME}" -m "new tag ${TAG_PREBUILD_NAME} for ${APP_NAME} code" && \
	echo "copying source to tag: ${TAG_PREBUILD_NAME}" && \
	copyToTagForPre "${SVN_REPO}/trunk/$APP_ROOT ${SVN_REPO}/trunk/zkrw" && \

	############################################# 
	# Build the files from SVN
	#############################################

	echo "Starting build and packaging process..." && \

	# delete the build directory otherwise some files may stick around even after deletion.
	if [ -d $LOCAL_BUILD_APP_SUBDIR ]; then
		echo "Cleaning build dir: $LOCAL_BUILD_APP_SUBDIR"
		rm -rf $LOCAL_BUILD_APP_SUBDIR
	fi && \

	# update the svn folders we need
	updateSvnFolder $LOCAL_BUILD_DIR "$APP_ROOT" $SVN_REPO/trunk && \
	
	# build the jar using Maven
	echo "building jar using Maven...." && \
	
	cd $APP_ROOT_PATH && \
	
	sed "s:<version>.*-SNAPSHOT</version>:<version>${TAG_NAME}-SNAPSHOT</version>:" pom.xml > pom.xml.new && \
    	mv pom.xml.new pom.xml && \
	#echo " > WARNING: not running unit tests."
	# mvn -DskipTests=true package
	mvn $MVN_ACTION && \
	
	# Note: must svn add files before svn copy to the tag.

	echo " > Creating version file." && \
	echo -e "version $VERSION tag $TAG built on $(date)" > $APP_ROOT_PATH/$VERSION_FILE_NAME && \
	svn add $VERSION_FILE_NAME && \
	
	echo "copy $BUILD_JAR_PATH to $APP_ROOT_PATH/$TARGET_JAR_NAME" && \
	cp -f $BUILD_JAR_PATH $APP_ROOT_PATH/$TARGET_JAR_NAME && \
	svn add $TARGET_JAR_NAME && \
	svn commit -m "adding version and pom files for $APP_NAME $TAG" && \
	echo "build complete" && \

        

	#############################################################################################
	# cut a new tag for RPM deployment, which will have only what is needed for deployment
	#############################################################################################
	echo "copying stuff to RPM deployment tag: ${TAG_NAME}" && \
	copyToTagForRPM "${SVN_REPO}/trunk/${APP_ROOT}/scripts" && \
	
	# copy jar and other fields from local directory
	copyToTagForRPM "$APP_ROOT_PATH/$VERSION_FILE_NAME $APP_ROOT_PATH/$TARGET_JAR_NAME $APP_ROOT_PATH/$RPM_SPEC_NAME $APP_ROOT_PATH/$RPM_MAC_NAME $APP_ROOT_PATH/$RPM_POST_NAME" && \
	
	echo "done" || ( echo "!!!!failed!!!!!" && exit 1 )

else 
	echo "WARNING: tag $TAG_NAME already exists, so it will not be recreated. No build will be performed."
	exit 1
	
fi

