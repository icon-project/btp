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

package foundation.icon.score.util;

import score.Context;
import scorex.util.HashMap;

import java.util.Map;

public class Logger {
    static String loggerId(Class<?> clazz) {
        return clazz.getName();
    }
    static Map<String, Logger> loggers = new HashMap<>();

    /**
     * Return static Logger(clazz.getName())
     *
     * @param clazz class
     * @return Logger
     */
    public static Logger getLogger(Class<?> clazz) {
        String id = loggerId(clazz);
        Logger logger = loggers.get(id);
        if(logger == null) {
            logger = new Logger(id);
            loggers.put(id, logger);
        }
        return logger;
    }

    public static final String DELIMITER = " ";
    final String id;

    public Logger(String id) {
        this.id = id;
    }

    /**
     * Print list of message
     *
     * @param msg list of message
     */
    public void println(String ... msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(this.id).append("]");
        for(String s : msg) {
            sb.append(DELIMITER).append(s);
        }
        Context.println(sb.toString());
    }

    /**
     * Print list of object
     *
     * @param prefix prefix
     * @param objs list of object
     */
    public void println(String prefix, Object ... objs) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(this.id);
        if (prefix != null) {
            sb.append(":").append(prefix);
        }
        sb.append("]");
        for(Object obj : objs) {
            sb.append(DELIMITER).append(StringUtil.toString(obj));
        }
        Context.println(sb.toString());
    }

    /**
     * Print key, value with ClassName
     *
     * @param prefix prefix
     * @param key  key
     * @param value  value
     */
    public void printKeyValue(String prefix, Object key, Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(this.id).append(":").append(prefix).append("]");
        sb.append(DELIMITER).append("key: ").append(toStringWithClassName(key));
        sb.append(DELIMITER).append("value: ").append(toStringWithClassName(value));
        Context.println(sb.toString());
    }

    public static String toStringWithClassName(Object obj) {
        if (obj == null) {
            return "null";
        } else {
            if (obj instanceof String) {
                return (String)obj;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Class<").append(obj.getClass().getName()).append(">");
                sb.append(StringUtil.toString(obj));
                return sb.toString();
            }
        }
    }
}
