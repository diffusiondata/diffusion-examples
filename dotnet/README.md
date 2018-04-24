# .NET Examples for the Diffusion APIs

This directory contains examples showing the use of the .NET API for Diffusion and Diffusion Cloud.

These examples are all bundled in a Visual Studio 2015 solution file. For the examples to work,
you need to download and reference the PushTechnology.ClientInterface assembly via NuGet. The following projects within the solution file need a reference to the assembly for a successful compilation:

*   Examples/Runner
*   GettingStarted/Publishing
*   GettingStarted/Subscribing

The GettingStarted and Examples directories contain runnable files and are included in the dotnet-examples.sln. Select the Publishing and Subscribing projects to run the GettingStarted examples.
For the rest of the examples, uncomment the appropriate block of code inside the Program.cs to run a particular set of examples.


## Client libraries

You can download the required files from the following locations:

*   Using NUget: [https://www.nuget.org/packages/PushTechnology.UnifiedClientInterface/](https://www.nuget.org/packages/PushTechnology.UnifiedClientInterface/)

*   Download from [our website](http://download.pushtechnology.com/cloud/latest/sdks.html#dotnet)

*   The client libraries are also available in the `clients` directory of the Diffusion server installation.

## Non-Runnable Examples

All files within the SampleCode folder are non-runnable examples and are not bound to any solution or project file. These examples are from an older version of the Diffusion .NET Client Library and have no runnable equivalent yet.
