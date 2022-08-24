# Diffusion Python Client Examples

This project contains a set of examples for accessing the Diffusion server using the
 Python SDK, grouped into directories representing different areas of Diffusion
  functionalities. There are two types of examples for each area:

* Short Python code snippets, each illustrating a single use case. These snippets can be
 used directly in application code, making sure that various variables are changed to
  reflect the actual system.
* Jupyter notebooks, which can be executed (assuming there is a running Diffusion server
 they can connect to) to illustrate the functionalities in practice. The notebooks are
  designed to work out of the box in most cases, provided there is a Diffusion server
   they can connect to.

Python Client for Diffusion can be installed using `pip`:
```shell
$ pip install diffusion
```

For more information on using Diffusion, please refer to the [online documentation](https://docs.pushtechnology.com).

## Examples as Python Scripts
Each directory in the project includes one or more self-contained Python scripts. To
 execute the example scripts, you can call them either directly or as Python modules:

```shell script
$ python examples/messaging/send_request_to_path.py
$ python -m examples.messaging.send_request_to_path
```

## Examples as Jupyter Notebooks

Each directory in the project also a Jupyter notebook for each example, with the same
 functionality as the corresponding Python script.