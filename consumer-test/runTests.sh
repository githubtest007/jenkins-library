#!/bin/sh

set -ex

rm -rf workspace
git clone -b infrastructure-integration-test https://github.com/sap/cloud-s4-sdk-book workspace
cp -f jenkins.yml workspace
cd workspace
docker run -v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:/workspace -v /tmp -e CASC_JENKINS_CONFIG=/workspace/jenkins.yml -e CF_PW -e ERP_PW -e BRANCH_NAME=master ppiper/jenkinsfile-runner
