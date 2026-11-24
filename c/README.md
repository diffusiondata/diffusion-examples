
<img src="https://www.diffusiondata.com/wp-content/themes/diffusion/images/logo-tag.svg" style="width:250px;"/>

<p/>

<p align="center">


## Diffusion SDK Examples - C


### Pre-requirements

- [CMake](https://cmake.org/) (version 3.16+)
- ZLib library
- PCRE library


### Folder Structure

1. Create a folder named `target` at the same level as this README.md
2. Create a folder named `client` in `target` containing the `include` and `lib` folders of the Diffusion C SDK you wish to use.


### Build process

Run the following command at this folder level:

```bash
cmake --fresh .
cmake --build .
```

This will created an executable in `target/bin` for each example.
