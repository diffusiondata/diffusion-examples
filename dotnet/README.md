# Diffusion .NET Client Examples

This directory contains examples showing the use of the Diffusion .NET Client Library.

## Client libraries

In order for all examples to work you have to add the .NET `Diffusion.Client` via NuGet:

*   using NuGet Package Manager in Visual Studio, or,

*   on the command line with: `dotnet add package Diffusion.Client`.

Also,

*   Download the client from our website (https://www.diffusiondata.com/diffusion-cloud/#dotnet).

*   The client library is available in the `clients` directory of the Diffusion server installation.

## Running Examples

PLEASE READ THIS FIRST
Before running the examples back up the `persistence` directory of the Diffusion server installation. 
This is necessary as some examples, such as those for Topic Views, will leave the persistence store changed when finished running. 
In this situation stop the Diffusion server and restore the backup before continuing.

1.   Uncomment the example in Program.cs.

2.   Start the diffusion server using diffusion.bat or diffusion.sh in the `bin` directory of the Diffusion server installation.

3.   Run the example:

*    in Visual Studio: F5 or Ctrl+F5.

*    on the command line: `dotnet run`.

## Documentation

For further information see our documentation at https://docs.diffusiondata.com/docs/latest/manual/html/.