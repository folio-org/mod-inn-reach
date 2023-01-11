# What's here

This directory contains some scripts for testing the initial contribution job. 

To run them the kafka command line tools are required. To run them copy them into the same path where your kafka
command line bin directory is. 
```
cp ~/folio/mod-inn-reach/src/test/scripts/* .
```
Right now this tests the job runner code only. It simulates calls to folio and to the d2ir central sever.

Most of the scripts have a command line param. Take a look. To run a test of the initial contribution do the following
after copying the files.
1. Start up kafka locally at its default port `9092`.
2. Run ./create-test-topic.sh <topic name>
3. Start another web server to simulate calls to folio or to d2ir at port `8080`. You can do this with any server
you like.
4. Start mod-inn-reach in your debugger or launch its jar with `java -jar <target>/<jarfile>.jar`

This depends on code in the `ContributionJobRunner.java` file being changed to call the simulation servers rather
than the actual ones. Also you have to change topic name in `IterationEventReaderFactory.java`.

After running a test the offsets in the topics will be changed. So to reset those to run another test without having to
add more messages just call `./reset-test.sh <topic name>`. 
