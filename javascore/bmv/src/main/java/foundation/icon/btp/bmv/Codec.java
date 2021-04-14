/*
 * Copyright 2021 ICON Foundation
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
package foundation.icon.btp.bmv;

import foundation.icon.ee.io.*;

public abstract class Codec {
    abstract public DataReader newReader(byte[] bytes);
    abstract public DataWriter newWriter();

    public static final Codec messagePack = new Codec() {
        @Override
        public DataReader newReader(byte[] bytes) {
            return new MessagePackDataReader(bytes);
        }
        @Override
        public DataWriter newWriter() {
            return new MessagePackDataWriter();
        }
    };

    public static final Codec rlp = new Codec() {
        @Override
        public DataReader newReader(byte[] bytes) {
            return new RLPDataReader(bytes);
        }

        @Override
        public DataWriter newWriter() {
            return new RLPDataWriter();
        }
    };
}
