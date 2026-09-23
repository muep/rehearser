#!/bin/sh
clojure -Stree | diff -u doc/deps.txt -
