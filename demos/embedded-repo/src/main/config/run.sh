#!/bin/sh

java -Djava.ext.dirs=`pwd`/lib -cp  ${project.artifactId}-${project.version}.jar org.modeshape.demo.embedded.repo.EmbeddedRepositoryDemo
