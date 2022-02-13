# Build guide

## Platform preparation

* GoLang 1.13+

  **Mac OSX**
    ```
    brew install go
    ```

### ICON PYTHONSCORE(Smart Contract On Reliable Environment) 
 
* Python 3.7+ Virtual Environment

  **Mac OSX**
    ```
    brew install python
    pip install virtualenv setuptools wheel
    ```

### ICON JAVASCORE(Smart Contract On Reliable Environment)

* Java 11+ and Grade 6.7+

  **Mac OSX**
    ```
    if [ ! -d "$HOME/.sdkman" ]; then
        curl -s "https://get.sdkman.io" | bash
    fi
    source $HOME/.sdkman/bin/sdkman-init.sh
    sdk install java 11.0.11.hs-adpt
    sdk default java 11.0.11.hs-adpt
    sdk install gradle 6.7.1
    sdk default gradle 6.7.1
    ```

### Ethereum SOLIDITY

* Node.js v14.0.0, Yarn v1.22.0, Truffle v5.3.13

  **Mac OSX**
    ```
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash
    source $HOME/.nvm/

    echo 'export NVM_DIR="$([ -z "${XDG_CONFIG_HOME-}" ] && printf %s "${HOME}/.nvm" || printf %s "${XDG_CONFIG_HOME}/nvm")"
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" # This loads nvm' >> ~/.zshrc

    source ~/.zshrc
    
    nvm install v14.17.0
    nvm alias default v14.17.0
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
****
### Build ICON PYTHONSCORE

```bash
make dist-py
```

Output files are placed under `build/contracts/pyscore/` directory.

### Build ICON JAVASCORE

```bash
make dist-java
```

### Build Ethereum SOLIDITY

```bash
make dist-sol
```

Output files are placed under `build/contracts/solidity/` directory.
