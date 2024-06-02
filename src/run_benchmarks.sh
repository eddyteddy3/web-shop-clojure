#!/usr/bin/env bash

# set -e makes the script exit if any command fails
# set -u makes the script exit if any unset variable is used
# set -o pipefail makes the script exit if any command in a pipeline fails
set -euo pipefail

for i in {1..10}; do
    echo "---"
    echo "> iteration $i"
    lein run >> "benchmarks/result-conc-thread-50-$i.txt"
done
# do
    # echo "---"
    # echo "> iteration $i"
#     # ./clj custom_web_shop_app/core.clj > "benchmarks/result-random-$i.txt"
# done
