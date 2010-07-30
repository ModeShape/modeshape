#!/bin/sh

# The restclient.sh script was developed to enable quick publishing fom
# the command line.

# How to Execute:  Call restclient.sh (passing no arguments or --help) to see the help
#


# resolve links - $0 may be a softlink
LOC="$0"

while [ -h "$LOC" ] ; do
  ls=`ls -ld "$LOC"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    LOC="$link"
  else
    LOC=`dirname "$LOC"`/"$link"
  fi
done

LOCDIR=`dirname "$LOC"`
PRGDIR=`cd "$LOCDIR"; pwd`

java -cp "${PRGDIR}:${PRGDIR}/*" org.modeshape.web.jcr.rest.client.json.JsonRestClient $*


