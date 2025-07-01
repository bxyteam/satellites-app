#!/bin/bash -ex
cd $(dirname $0)

# check that compilerId environment variable is set
if [ -z "${satelliteConfigId}" ]; then
    echo "satelliteConfigId is empty, you need to add -e satelliteConfigId=${satelliteConfigId} to docker run "
    exit 1;
fi

# create needed paths
storagePath="/var/compiler/satellite"
mkdir -p $storagePath

BASE_DIR="/var/satellite/data"
RESOURCE_FILE="devleo"

# Clean up previous web folder
rm -rf "${BASE_DIR}/web"

# Recreate the structure cleanly
mkdir -p "${BASE_DIR}"

# Move web directory properly
mv /home/satellite/application/web "${BASE_DIR}/web"

chmod -R ugo+rwx "${BASE_DIR}/web"

# start application
cd $(dirname $0)
(nohup ./start.sh ${satelliteConfigId} &) && tail -F /etc/hostname
