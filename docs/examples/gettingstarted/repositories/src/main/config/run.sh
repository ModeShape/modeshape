#!/bin/sh

java -Djava.ext.dirs=`pwd`/lib -cp .:modeshape-example-repositories-0.1-SNAPSHOT.jar org.modeshape.example.repository.RepositoryClient $1
