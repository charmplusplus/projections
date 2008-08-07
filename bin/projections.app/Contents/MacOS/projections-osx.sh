#!/bin/sh

bname=$(basename $0)
dname=$(dirname "$0")

appbundle=$dname/../../
parent=$appbundle/../

proj_script=$parent/projections

$proj_script