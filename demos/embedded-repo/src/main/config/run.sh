#!/bin/sh

java -Djava.ext.dirs=`pwd`/lib -cp modeshape-embedded-repo-demo-${project.version}.jar org.modeshape.demo.embedded.repo.EmbeddedRepositoryDemo
