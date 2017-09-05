#!/bin/bash
PYTHON_PATH="${WORKSPACE}/.venv/bin/python"
REPORTS_FOLDER="${WORKSPACE}/test_results"
INFLUX_DOWNTIME_FOLDER="$(dirname "${BASH_SOURCE[0]}")"
ADDITIONAL_ARGS="$*"

pushd "$WORKSPACE"
  virtualenv .venv
  . .venv/bin/activate
  pip install -r rpc-gating/requirements.txt -c rpc-gating/constraints.txt
popd
mkdir -p "${REPORTS_FOLDER}"
x=0
while [ $x -lt 48 ]
do
  echo "Report $x, total elapsed time $(( $x * 5)) minutes"
  $INFLUX_DOWNTIME_FOLDER/influx.py --ymlreport $REPORTS_FOLDER/influx-downtime.yml --subunitreport $REPORTS_FOLDER/influx-downtime.bin $ADDITIONAL_ARGS
  x=$(( $x + 1 ))
  sleep 5m
done
