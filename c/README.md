# C Examples for the Diffusion APIs

This directory contains examples showing the use of the C API for Diffusion and Diffusion Cloud.

To use these examples, download the C client library for your operating system from the links below.
The C API is available for the following operating systems:

* 64-bit Linux as a statically or dynamically linked library
* 32-bit and 64-bit Windows as a statically linked library
* 64-bit OS X as a statically linked library


## Client libraries

You can download the C client libraries from the following locations:

*   Download from [our website](http://download.pushtechnology.com/cloud/latest/sdks.html#c)

*   The client libraries are also available in the `clients` directory of the Diffusion server installation.


## Dependencies

The C client requires the following dependencies:

*   [Perl Compatible Regular Expressions (PCRE)](http://pcre.org) library, version 8.3
*   [OpenSSL](https://www.openssl.org) library, version 1.0.2a

For Linux or OS X, you can download them through your operating system's package manager.
For Windows™, Push Technology provides custom builds of these libraries built with the same compiler and on the same architecture as the C client libraries.


## Running the examples

1. Install the required dependencies on your development system.
2. Get the C client library zip for your platform and extract it.
3. Uncomment the variables at the top of the `Makefile` in the examples directory. Set these variables to the location of the extracted Diffusion C client library:

           DIFFUSION_C_CLIENT_INCDIR	=    <path_to_library>/include
           DIFFUSION_C_CLIENT_LIBDIR	=    <path_to_library>/lib

4. Run the `make` command in the examples directory. 