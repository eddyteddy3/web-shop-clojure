#!/usr/bin/env bash

# set -e makes the script exit if any command fails
# set -u makes the script exit if any unset variable is used
# set -o pipefail makes the script exit if any command in a pipeline fails
set -euo pipefail

# for i in {1..10}; do
#     echo "---"
#     echo "> iteration $i"
#     lein run >> "benchmarks/result-conc-thread-50-$i.txt"
# done
# do
    # echo "---"
    # echo "> iteration $i"
#     # ./clj custom_web_shop_app/core.clj > "benchmarks/result-random-$i.txt"
# done

echo '---------'
echo 'EXPERIMENT 1'
echo '---------'

# mkdir benchmarks/e1

shops=20

for i in {1..10}
do
    echo "---"
    echo "> iteration $i"
    lein run >>  "benchmarks/e1/result-$shops-shops.txt" #"benchmarks/result-conc-thread-50-$i.txt"
    #   java -cp ".;libs/*" clojure.main web_shop.clj >> "benchmarks/e3/result-$shops-shops.txt"
done