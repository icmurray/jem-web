#!/bin/sh

./extract-dependencies.sh

ctags -h ".scala" -R -f scalatags ./app
ctags -h ".scala.java" -a -R -f scalatags ./target/srcs
ctags -h ".scala" -a -R -f scalatags ../../lib/ReactiveMongo
ctags -h ".scala" -a -R -f scalatags ../../lib/Play-ReactiveMongo
