
- Prerequire

you need to have JDK6 installed on your OS

check 

 sp$ java -version
java version "1.6.0_13"
Java(TM) SE Runtime Environment (build 1.6.0_13-b03-211)
Java HotSpot(TM) 64-Bit Server VM (build 11.3-b02-83, mixed mode)


- How to generate mass of data
 
 -- This two files bank.properties and benerator.xml describe how many entities and the model
 
         * /dna-jcr-gen/src/test/resources/benerator/bank.properties
         * /dna-jcr-gen/src/test/resources/benerator/benerator.xml
 -- Use "mvn benerator:generate" command to generate data into you Source (For now only test on 
    nMemoryRepositorySource source)
 
 