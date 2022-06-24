/*
 * Copyright 2022 ICONLOOP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.bmv.btp;

import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.crypto.KeystoreException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class Env {
    public static final Log LOG = Log.getGlobal();
    private static Chain chain;

    static {
        String envFile = System.getProperty("env.props", "conf/env.props");
        Properties props = new Properties();
        try {
            LOG.info("Using env.props: " + envFile);
            FileInputStream fis = new FileInputStream(envFile);
            props.load(fis);
            fis.close();
        } catch (IOException e) {
            System.err.printf("'%s' does not exist\n", envFile);
            throw new IllegalArgumentException(e.getMessage());
        }
        String confPath = Path.of(envFile).getParent().toString() + "/";
        readProperties(props, confPath);
    }

    private static void readProperties(Properties props, String confPath) {
        String chainName = "chain";
        String nid = props.getProperty(chainName + ".nid");
        if (nid == null) {
            throw new IllegalArgumentException("nid not found");
        }
        String godWalletPath = confPath + props.getProperty(chainName + ".godWallet");
        String godPassword = props.getProperty(chainName + ".godPassword");
        KeyWallet godWallet;
        try {
            godWallet = readWalletFromFile(godWalletPath, godPassword);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        String nodeName = "node";
        String url = props.getProperty(nodeName + ".url");
        if (url == null) {
            throw new IllegalArgumentException("node url not found");
        }
        chain = new Chain(Integer.parseInt(nid.substring(2), 16), godWallet, url);
    }

    private static KeyWallet readWalletFromFile(String path, String password) throws IOException {
        try {
            File file = new File(path);
            return KeyWallet.load(password, file);
        } catch (KeystoreException e) {
            e.printStackTrace();
            throw new IOException("Key load failed!");
        }
    }

    public static Chain getDefaultChain() {
        if (chain == null) {
            throw new AssertionError("Chain not found");
        }
        return chain;
    }

    public static class Chain {
        public final int networkId;
        public final Wallet godWallet;
        private final String nodeUrl;

        public Chain(int networkId, Wallet godWallet, String url) {
            this.networkId = networkId;
            this.godWallet = godWallet;
            this.nodeUrl = url;
        }

        public String getEndpointURL() {
            return this.nodeUrl;
        }
    }
}
