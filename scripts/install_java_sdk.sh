# /bin/sh
echo "Install SDKMAN"

curl -s "https://get.sdkman.io" | bash
source $HOME/.sdkman/bin/sdkman-init.sh
sdk install java 11.0.11.hs-adpt
sdk install gradle 6.7.1