#!/bin/sh

clojure -Stree | diff -u docs/deps.txt -
exit 0
