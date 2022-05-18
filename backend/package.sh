#!/bin/sh

mkdir -pv classes target

clojure -M -e "(compile 'rehearser.main)"
clojure -M:uberdeps --main-class rehearser.main
