#!/bin/sh

# find set_hydro_env and initialize the environment
prev_dir=`pwd`
env_file="set_hydro_env"
while [ ! -e ${env_file} ]
do
	cd ..
	if [ "/" = "`pwd`" ]
	then
		break
	fi
done
. ./${env_file}
cd ${prev_dir}

gad() {
        get_apps_defaults $@
}

