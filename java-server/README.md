# Java server examples for the Diffusion Publisher API

This directory contains examples showing the use of the Java Publisher API for Diffusion.

The examples can be built using Apache Maven. To compile you need a Diffusion installation with
    the DIFFUSION_HOME environment variable set appropriately.

    `mvn clean compile`
    
To install in the server, configure [conflation](http://docs.pushtechnology.com/docs/latest/manual/html/administratorguide/configuration/Server.html#reference_server__section_conflation)
Add the following to your `etc/Server.xml`:
```
    <conflation>
        <default-conflation-policy>example-merger</default-conflation-policy>
        <conflation-policy name="example-merger">
             <mode>REPLACE</mode>
             <merger>com.pushtechnology.diffusion.examples.MessageMergerExample</merger>
        </conflation-policy>
    </conflation>
```
and set the default queue to enable conflation 
```
    <!-- True if the queue conflates -->
    <conflates>true</conflates>
```
Copy the generated jar file from your maven target directory to the server's `ext` directory.

## Libraries

You can download the Diffusion server from the following location:

*   [http://download.pushtechnology.com/](http://download.pushtechnology.com/)


