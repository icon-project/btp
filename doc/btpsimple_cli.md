# Btpsimple

## btpsimple

### Description
Command Line Interface of Relay for Blockchain Transmission Protocol

### Usage
` btpsimple [flags] `

### Options
|Name,shorthand | Environment Variable | Required | Default | Description|
|---|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR | false |  |  Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG | false |  |  Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | false | trace |  Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS | true |  |  BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT | true |  |  Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | false | [] |  Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD | false |  |  Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET | false |  |  Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE | false |  |  KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS | false |  |  LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | false | info |  LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME | false |  |  LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | false | [] |  LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR | false |  |  LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | false | debug |  Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | false |  Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME | false |  |  Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | false |  Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | false | 0 |  Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | false | 0 |  Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | false | 100 |  Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | false | 0 |  Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS | true |  |  BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT | true |  |  Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | false | [] |  Options, comma-separated 'key=value' |

### Child commands
|Command | Description|
|---|---|
| [btpsimple save](#btpsimple-save) |  Save configuration |
| [btpsimple start](#btpsimple-start) |  Start server |
| [btpsimple version](#btpsimple-version) |  Print btpsimple version |

## btpsimple save

### Description
Save configuration

### Usage
` btpsimple save [file] [flags] `

### Options
|Name,shorthand | Environment Variable | Required | Default | Description|
|---|---|---|---|---|
| --save_key_store |  | false |  |  KeyStore File path to save |

### Inherited Options
|Name,shorthand | Environment Variable | Required | Default | Description|
|---|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR | false |  |  Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG | false |  |  Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | false | trace |  Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS | true |  |  BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT | true |  |  Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | false | [] |  Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD | false |  |  Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET | false |  |  Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE | false |  |  KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS | false |  |  LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | false | info |  LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME | false |  |  LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | false | [] |  LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR | false |  |  LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | false | debug |  Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | false |  Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME | false |  |  Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | false |  Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | false | 0 |  Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | false | 0 |  Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | false | 100 |  Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | false | 0 |  Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS | true |  |  BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT | true |  |  Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | false | [] |  Options, comma-separated 'key=value' |

### Parent command
|Command | Description|
|---|---|
| [btpsimple](#btpsimple) |  BTP Relay CLI |

### Related commands
|Command | Description|
|---|---|
| [btpsimple save](#btpsimple-save) |  Save configuration |
| [btpsimple start](#btpsimple-start) |  Start server |
| [btpsimple version](#btpsimple-version) |  Print btpsimple version |

## btpsimple start

### Description
Start server

### Usage
` btpsimple start [flags] `

### Options
|Name,shorthand | Environment Variable | Required | Default | Description|
|---|---|---|---|---|
| --cpuprofile |  | false |  |  CPU Profiling data file |
| --memprofile |  | false |  |  Memory Profiling data file |

### Inherited Options
|Name,shorthand | Environment Variable | Required | Default | Description|
|---|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR | false |  |  Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG | false |  |  Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | false | trace |  Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS | true |  |  BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT | true |  |  Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | false | [] |  Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD | false |  |  Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET | false |  |  Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE | false |  |  KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS | false |  |  LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | false | info |  LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME | false |  |  LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | false | [] |  LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR | false |  |  LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | false | debug |  Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | false |  Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME | false |  |  Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | false |  Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | false | 0 |  Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | false | 0 |  Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | false | 100 |  Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | false | 0 |  Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS | true |  |  BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT | true |  |  Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | false | [] |  Options, comma-separated 'key=value' |

### Parent command
|Command | Description|
|---|---|
| [btpsimple](#btpsimple) |  BTP Relay CLI |

### Related commands
|Command | Description|
|---|---|
| [btpsimple save](#btpsimple-save) |  Save configuration |
| [btpsimple start](#btpsimple-start) |  Start server |
| [btpsimple version](#btpsimple-version) |  Print btpsimple version |

## btpsimple version

### Description
Print btpsimple version

### Usage
` btpsimple version `

### Inherited Options
|Name,shorthand | Environment Variable | Required | Default | Description|
|---|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR | false |  |  Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG | false |  |  Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | false | trace |  Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS | true |  |  BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT | true |  |  Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | false | [] |  Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD | false |  |  Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET | false |  |  Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE | false |  |  KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS | false |  |  LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | false | info |  LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME | false |  |  LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | false | [] |  LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR | false |  |  LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | false | debug |  Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | false |  Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME | false |  |  Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | false |  Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | false | 0 |  Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | false | 0 |  Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | false | 100 |  Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | false | 0 |  Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS | true |  |  BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT | true |  |  Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | false | [] |  Options, comma-separated 'key=value' |

### Parent command
|Command | Description|
|---|---|
| [btpsimple](#btpsimple) |  BTP Relay CLI |

### Related commands
|Command | Description|
|---|---|
| [btpsimple save](#btpsimple-save) |  Save configuration |
| [btpsimple start](#btpsimple-start) |  Start server |
| [btpsimple version](#btpsimple-version) |  Print btpsimple version |

