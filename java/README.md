# Diffusion API Java examples

This directory contains examples showing the use of the Java API
for Diffusion and Reappt.

The examples can be built using Apache Maven.

The Maven POM file is also configured to launch the  PublishingClient
example. To use, edit PublishingClient.java to set an appropriate URL for
your Diffusion or Reappt server. Then start the example using Maven:

    mvn clean compile exec:java