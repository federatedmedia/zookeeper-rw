#!/bin/bash
# add -x to the previous line to turn on debug info

# Each function can also be called from the command line.
# as in . scriptName.sh
#and
# where any function can be called externally
# ./scriptName.sh funcname arg1 arg2...
#
# brogosky

. functionsZk.sh

##########################
# END OF FUNCTIONS
##########################

# To create a script of functions that can be included in another file 
# as in . scriptName.sh
#and
# where any function can be called externally
# ./scriptName.sh funcname arg1 arg2...

# if the first argument is not empty, and it is the name 
# of a function, then run it as a function call
if [[ "$1" != "" && "$1" =~ ^[a-zA-Z]+.* && $(type -t $1) =~ .*function.* ]]; then
	cmdStr="$1"
	
	args=("$@")
	#echo "@=$@"
	for ((i=1; i < $#; i++)); do
	   cmdStr="$cmdStr \"${args[$i]}\""
	done
	#echo "cmdStr=$cmdStr"
	eval $cmdStr
	exit $?
fi
