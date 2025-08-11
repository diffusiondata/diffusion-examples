# Diffusion Python Client Examples

This project contains a set of examples for accessing the Diffusion server using the
 Python SDK, grouped into directories representing different areas of Diffusion
  functionalities. Each illustrates a single use case. These snippets can be
 used directly in application code, making sure that various variables are changed to
  reflect the actual system.

Python Client for Diffusion can be installed using `pip`:
```shell
$ pip install diffusion
```

For more information on using Diffusion, please refer to the [online documentation](https://docs.diffusiondata.com).

## Examples as Python Scripts
Each directory in the project includes one or more self-contained Python scripts. To
 execute the example scripts, you can call them either directly or as Python modules:

```shell script
$ python src/diffusion_examples/mapping_and_wrangling/session_trees/get_branch_mapping_table.py
$ python -m diffusion_examples.mapping_and_wrangling.session_trees.get_branch_mapping_table
```
 
### Typical 'run' method arguments

- `principal` - username of the principal connecting to the server
- `password` - password of the principal connecting to the server
- `server_url` - URL of the server
