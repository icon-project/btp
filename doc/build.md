# Build guide

## Platform preparation

* GoLang 1.13+

  **Mac OSX**
    ```
    brew install go
    ```

### ICON SCORE(Smart Contract On Reliable Environment) 
 
* Python 3.7+ Virtual Environment

  **Mac OSX**
    ```
    brew install python
    pip install virtualenv setuptools wheel
    ```

## Environment

### Source checkout

First of all, you need to check out the source.
```bash
git clone $REPOSITORY_URL btp
```

## Build

### Build executables

```bash
make
```

Output binaries are placed under `bin/` directory.


### Build ICON SCORE

```bash
make dist-py
```

Output files are placed under `build/pyscore/` directory.
