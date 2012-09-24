#!/bin/bash
# Functions that wrap Zookeeper zkrw client calls.
# brogosky

# Poll a Zookeeper queue and echo the results. Returns non-zero if the queue is not found or empty.
# Arg 1: Path to queue.
# Arg 2: Wait time in seconds for polling the queue. May be blank. Default is zero.
#
# exit value less than 0 indicates an error
pollQueue()
{
	local zk_q_path="$1"
	local zk_q_wait="$2"

	if [ "$zk_q_wait" == "" ]; then
		zk_q_wait=0
	fi

	local exists="$(zk exists ${zk_q_path})"
	if [[ "$?" != "0" || "${exists}" != "true" ]]; then
		return -1
	fi
	
        local cmd="zk qPoll ${zk_q_path} ${zk_q_wait}"
	local q_res
	# run command, hiding any error messages
        eval $cmd 2>/dev/null
        q_res=$?

	if [ "$q_res" != "0" ]; then
		local nChildren="$(zk getNumChildren ${zk_q_path})"
		if [[ "$?" != 0 || "${nChildren}" == "0" ]]; then
			return 1
		fi
	fi

	return $q_res
	
}

peakQueue()
{

	local zk_q_path="$1"

	local exists=""
	exists="$(zk exists ${zk_q_path})"
	if [[ "$?" != "0" || "${exists}" != "true" ]]; then
		return -1
	fi
	
	local nChildren=""
	nChildren="$(zk getNumChildren ${zk_q_path})"
	if [[ "$?" != "0" || "${nChildren}" == "0" ]]; then
		return 1
	fi

        local cmd="zk getChildrenOnly ${zk_q_path} | awk -F '\t' '{printf(\"%s\n\",\$2)}'"
	# run command, hiding any error messages
        eval $cmd 2>/dev/null
        return $?

}

enQueue()
{

	local zk_q_path="$1"
	local val="$2"
	local priority="$3"
	local priorArg=" $priority" 
	if [ "$priority" == "" ]; then
		priorArg=""
	fi
	local exists=""
	exists="$(zk exists ${zk_q_path})"
	if [[ "$?" != "0" || "${exists}" != "true" ]]; then
		return -1
	fi
	
        local cmd="zk qAdd ${zk_q_path} \"$val\"${priorArg}"
	
        eval $cmd
        return $?

}

# adds to queue if the value does not already exist in the queue
enQueueIfNew()
{

	local zk_q_path="$1"
	local val="$2"
	local priority="$3"
	local priorArg=" $priority" 
	if [ "$priority" == "" ]; then
		priorArg=""
	fi
	local exists=""
	exists="$(zk exists ${zk_q_path})"
	if [[ "$?" != "0" || "${exists}" != "true" ]]; then
		return -1
	fi
	local numInQueue="$(peakQueue $zk_q_path | grep -c $val)"	
	if [ $numInQueue -eq 0 ]; then
		local cmd="zk qAdd ${zk_q_path} \"$val\"${priorArg}"
	
		eval $cmd
		return $?
	else
		# already in the queue
		return 0
	fi
}

# Return the number of retry jobs in the queue
getNumJobsInQueue()
{

	local zk_q_path="$1"

	local num=""
	local result=""
	if [ "$(zk exists $zk_q_path)" == "true" ]; then
		num="$(zk getNumChildren $zk_q_path)"
		result=$?

		if [[ "$result" == "0" && "$num" =~ ^[0-9]+$ ]] ; then
			echo $num
		else
			echo "0"
			exit 1
		fi
		
	else
		echo "0"		
	fi

}

# Waits for a change in a zookeeper zNode and returns the new value
waitForChange() 
{
	local zk_path="$1"
	local wait_in_sec="$2"
	local old_value="$3" # optional

	local pause_in_sec="$4" #optional

	if [[ "$old_value" == "" ]]; then
		old_value="$(zk get ${zk_path})"
	fi

	if [[ "$pause_in_sec" == "" ]]; then
		pause_in_sec="60"
	fi
	
	local exists="true"
	local cur_dt="$(date +%s)"
	local end_dt="$(( $cur_dt + $wait_in_sec ))"
	local new_value="$old_value"
	while [[ $cur_dt -lt $end_dt && "${exists}" == "true" ]]; do

		new_value="$(zk get ${zk_path})"

		if [[ "$?" != "0" ]]; then

			# some error, so check if it really exists			
			exists="$(zk exists ${zk_path})"

		elif [[ "$new_value" != "$old_value" ]]; then
			echo "$new_value"
			return 0
		fi

		sleep $pause_in_sec
		
		cur_dt="$(date +%s)"
	done
		
	return 1
}

# Delete zk zNodes that have modified times older than the given date-time.
# The zk zNodes must match the pattern using egrep -i
# Also deletes children of the base path if the parents are empty.
# Usage:
# "<base path>" "<date-time>" "<pattern>"
#
# base path = the root path in zookeeper
# date-time = The cutoff. Either a valid date string for the date function, or unixtime in seconds.
# pattern = Optional. The path pattern to match. If blank all child paths (recursively) from the basepath will be checked.
#
# Examples
# deleteOlder /DS/pv-tenzing/pv-hadoop-aws/staged-logs "2012-01-01 00:00:00" "staged_logs"
# deleteOlder /DS/pv-tenzing/pv-hadoop-aws/staged-logs "$(date --date "-5 days" +%s)" "staged_logs"
deleteOlder()
{
	local path="$1"
	local dateStr="$2"
	local pattern="$3"


	# try the date param as is, then try as unixtime

	local dateRes=1
	local dateSec=""

	if [[ "${dateStr}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}* ]]; then

		dateSec=$(date --date "${dateStr}" +%s) 2> /dev/null
		dateRes=$?
	fi

	if [[ $dateRes -ne 0 ]] || ! [[ "$dateSec" =~ ^[0-9]+$ ]]; then
		echo "Parsing as seconds..."
		date --date "1970-01-01 +${dateStr} seconds" > /dev/null
		dateRes=$?
		dateSec="${dateStr}"		
	fi
	
	if [[ $dateRes -ne 0 ]]; then
		echo "Invalid date parameter: $dateStr"
		return 1
	fi

	if ! [[ "$dateSec" =~ ^[0-9]+$ ]]; then
		echo "Invalid date parameter conversion: $dateStr converted to $dateSec"
		return 1
	fi
	
	echo "Cutoff: $(date --date "1970-01-01 +${dateSec} seconds")"

	# convert to milliseconds
	local dateCutoffMs=$(echo "$dateSec * 1000" | bc)
	
	if [ "$(zk exists $path)" != "true" ]; then
		echo "Zk path does not exist: $path"
		return 2
	fi

	if [ "${pattern}" == "" ]; then
		pattern="."
	fi

	# compare mtime. Note mtime is in milliseconds.
	echo "Running $0, using path=$path pattern=$pattern dateCutoff=$dateCutoffMs"
	zk getall $path | egrep -i "$pattern" | sort 2>/dev/null | while read pv; do

		# get just the path (tab delimited)
		local p=$(echo "$pv" | cut -d'	' -f1)

		# process each child path
		# ignore the base path itself
		if [[ "$p" != "$path" && "$(zk exists $p)" == "true" ]]; then
			local mtime=$(zk getMTime $p)
			if [ $mtime -lt $dateCutoffMs ]; then
				echo ">> Preparing to delete all $p. Current values:" && \
				zk getAll $p && \
				zk deleteAll $p && \
				echo ">> Deleted $p" || echo ">> Failed to delete $p"
			#else
			#	echo ">> Skipped $p"
			fi

			# check whether we can delete the parents
			# up to the base path
			local parPrev=""
			local parNext="$(getParent $p)"
			local parOK=1

			while [[ "$parNext" != "$path" && "$parNext" != "" && "$parNext" != "/" && "$parPrev" != "$parNext" && "$parOK" == "1" ]]; do
				parPrev="$parNext"
				local parMTime=$(zk getMTime $parNext)
				if [ $parMTime -lt $dateCutoffMs ]; then
					deleteIfNoChildren "$parNext" && \
					echo ">> Deleted parent $parNext" && \
					parNext="$(getParent $parNext)" || parOK=0
				fi
				#if [ "$parOK" != "1" ]; then
				#	echo "Failed to delete parent $parNext. Continuing"
				#fi
			done
		fi
	done

}

# Get the parent ZK Node path from the given path
# 
getParent() 
{
	local path="$1"
	local out="$(dirname $path)"
	local res=$?
	echo "$out"
	return $res
}

# Calls delete on the ZK node path if the path does
# not have any children.
# Returns 0 if the node was deleted.
#
# Note: delete will fail if children are added in between
# the time of the getNumChildren call

deleteIfNoChildren()
{
	local path="$1"

	if [ "$(zk exists $path)" != "true" ]; then
		echo "Path does not exist: $path"
		return 1
	fi

	local nch="$(zk getNumChildren $path)"
	local res=2
	if [ "$nch" == "0" ]; then
		zk delete $path
		res=$?
	fi

	return $res	
}


