# JavaScript simple clients

## Subscribing client

The file `index.html` is a browser client that connects to a Diffusion server or Diffusion Cloud service and subscribes to a topic. When the topic is updated, the web page displays the updated value of the topic.

#### Using this client

Before you can use this client with any Diffusion service, you will need to edit the location of the Diffusion client library:

1. Change the path in the script tag from `path_to_library` to point at your Diffusion client library.

To use this client with a Diffusion Cloud service, make the following edits:

1. Change the URL in the `diffusion.connect` method to that of your Diffusion Cloud service.

        diffusion.connect({
             host : 'service_url',
             principal : 'client',
             credentials : 'password'
         })


To use this client with a Diffusion server, make the following edits:

1. Change the URL in the `diffusion.connect` method to that of your Diffusion server.

2. You might also have to add the port parameter to specify the port on which the Diffusion server receives client connections. If no port is specified,
   the JavaScript client assumes port 80 for standard connections and port 443 for secure connections.

        diffusion.connect({
             host : 'diffusion_url',
             port : port,
             principal : 'client',
             credentials : 'password'
         })




Open `index.html` in a browser, it makes a connection to your Diffusion Cloud service or Diffusion server and subscribes to the topic `foo/counter`.

#### Testing this client

The client subscribes to the topic `foo/counter`. The web page is updated every time the data on the `foo/counter` topic is updated.
To see the client working you must publish updates to `foo\counter`. You can update the data on `foo/counter` by using one of the following tools:

* The publishing client example in this GitHub project
* A test client
    + For Diffusion Cloud, a test client is provided on the Dashboard
    + For Diffusion, test clients are provided in the `tools` directory of your installation

#### Further information

[Diffusion user manual](https://docs.pushtechnology.com/docs/6.1/)

[Diffusion Cloud user manual](https://docs.pushtechnology.com/cloud/latest/)


## Publishing client

The file `publishing.js` is a Node client that connects to Diffusion Cloud or your Diffusion server, creates a topic `foo/counter`, and publishes an incrementing count to the topic.

#### Prerequisites

1. Install NodeJS and NPM on the system that hosts this client.

2. Install the Diffusion JS library on your system:

        npm install diffusion

#### Using this client
To use this client with a Diffusion Cloud service or Diffusion server, make the following edits:

1. Change the URL in the `diffusion.connect` method to that of your Diffusion Cloud service or Diffusion server.

        diffusion.connect({
             host : 'url',
             principal : 'control',
             credentials : 'password'
         })

2. (Diffusion only) If required, add a port parameter to the connect method to specify the port on which the Diffusion server receives client connections. If no port is specified,
   the JavaScript client assumes port 80 for standard connections and port 443 for secure connections.

3. Run `publishing.js` from the command line using node:

        node publishing.js

#### Testing this client

The client creates the topic `foo/counter` and publishes a value to it every second.

To see the client working you must subscribe to the topic to receive the updates. You can subscribe to `foo/counter` by using one of the following tools:

* The subscribing client example in this GitHub project
* A test client
    * For Diffusion Cloud, a test client is provided on the Dashboard
    * For Diffusion, test clients are provided in the `tools` directory of your installation

#### Further information

[Diffusion user manual](https://docs.pushtechnology.com/docs/6.1/)

[Diffusion Cloud user manual](https://docs.pushtechnology.com/cloud/latest/)

