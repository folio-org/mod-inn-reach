# What's here

This directory contains some scripts for testing the initial contribution job. 

To run them the kafka command line tools are required. To run them copy them into the same path where your kafka
command line bin directory is. 
```
cd <to where you unzipped the kafka command line tools>
cp ~/folio/mod-inn-reach/src/test/scripts/initial-contribution/* .
```
Right now this tests the job runner code only. It simulates calls to folio and to the d2ir central sever.

Most of the scripts have a command line param. Take a look. To run a test of the initial contribution do the following
after copying the files.
1. Start up kafka locally at its default port `9092`.
2. Run ./create-test-topic.sh <topic name>
3. Start another web server to simulate calls to folio or to d2ir at port `8080`. You can do this with any server
you like. But you can also use this: https://github.com/steveellis/springserver.
4. Start mod-inn-reach in your debugger or launch its jar with `java -jar <target>/<jarfile>.jar`

This depends on code in the `ContributionJobRunner.java` file being changed to call the simulation servers rather
than the actual ones. Also you have to change topic name in `IterationEventReaderFactory.java`.

After running a test the offsets in the topics will be changed. So to reset those to run another test without having to
add more messages just call `./reset-test.sh <topic name>`. 

Running kafka locally as a container is easiest. Find a docker compose file that works for you and your system. It's
easier if it makes itself available on the default port but you can probably place an env variable so that
mod-inn-reach knows where to look for kafka. See this mod's readme for that.
