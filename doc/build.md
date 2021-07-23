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

### SOLIDITY

* Node.js v14.0.0, Yarn v1.22.0, Truffle v5.3.0

  **Mac OSX**
    ```
    npm install --global yarn truffle@5.3.0
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

### Build SOLIDITY

```bash
make dist-sol
```

Output files are placed under `build/solidity/` directory.
