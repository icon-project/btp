# Btpsimple

## btpsimple

### Description
Command Line Interface of Relay for Blockchain Transmission Protocol

### Usage
` btpsimple [flags] `

### Options
|Name,shorthand | Environment Variable | Default | Description|
|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR |  | Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG |  | Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | trace | Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS |  | BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT |  | Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | [] | Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD |  | Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET |  | Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE |  | KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS |  | LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | info | LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME |  | LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | [] | LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR |  | LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | debug | Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME |  | Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | 0 | Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | 0 | Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | 100 | Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | 0 | Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS |  | BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT |  | Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | [] | Options, comma-separated 'key=value' |

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
|Name,shorthand | Environment Variable | Default | Description|
|---|---|---|---|
| --save_key_store |  |  | KeyStore File path to save |

### Inherited Options
|Name,shorthand | Environment Variable | Default | Description|
|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR |  | Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG |  | Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | trace | Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS |  | BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT |  | Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | [] | Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD |  | Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET |  | Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE |  | KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS |  | LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | info | LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME |  | LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | [] | LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR |  | LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | debug | Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME |  | Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | 0 | Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | 0 | Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | 100 | Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | 0 | Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS |  | BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT |  | Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | [] | Options, comma-separated 'key=value' |

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
|Name,shorthand | Environment Variable | Default | Description|
|---|---|---|---|
| --cpuprofile |  |  | CPU Profiling data file |
| --memprofile |  |  | Memory Profiling data file |
| --mod_level |  | [] | Set console log level for specific module ('mod'='level',...) |

### Inherited Options
|Name,shorthand | Environment Variable | Default | Description|
|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR |  | Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG |  | Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | trace | Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS |  | BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT |  | Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | [] | Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD |  | Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET |  | Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE |  | KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS |  | LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | info | LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME |  | LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | [] | LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR |  | LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | debug | Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME |  | Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | 0 | Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | 0 | Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | 100 | Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | 0 | Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS |  | BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT |  | Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | [] | Options, comma-separated 'key=value' |

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
|Name,shorthand | Environment Variable | Default | Description|
|---|---|---|---|
| --base_dir | BTPSIMPLE_BASE_DIR |  | Base directory for data |
| --config, -c | BTPSIMPLE_CONFIG |  | Parsing configuration file |
| --console_level | BTPSIMPLE_CONSOLE_LEVEL | trace | Console log level (trace,debug,info,warn,error,fatal,panic) |
| --dst.address | BTPSIMPLE_DST_ADDRESS |  | BTP Address of destination blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --dst.endpoint | BTPSIMPLE_DST_ENDPOINT |  | Endpoint of destination blockchain |
| --dst.options | BTPSIMPLE_DST_OPTIONS | [] | Options, comma-separated 'key=value' |
| --key_password | BTPSIMPLE_KEY_PASSWORD |  | Password of KeyStore |
| --key_secret | BTPSIMPLE_KEY_SECRET |  | Secret(password) file for KeyStore |
| --key_store | BTPSIMPLE_KEY_STORE |  | KeyStore |
| --log_forwarder.address | BTPSIMPLE_LOG_FORWARDER_ADDRESS |  | LogForwarder address |
| --log_forwarder.level | BTPSIMPLE_LOG_FORWARDER_LEVEL | info | LogForwarder level |
| --log_forwarder.name | BTPSIMPLE_LOG_FORWARDER_NAME |  | LogForwarder name |
| --log_forwarder.options | BTPSIMPLE_LOG_FORWARDER_OPTIONS | [] | LogForwarder options, comma-separated 'key=value' |
| --log_forwarder.vendor | BTPSIMPLE_LOG_FORWARDER_VENDOR |  | LogForwarder vendor (fluentd,logstash) |
| --log_level | BTPSIMPLE_LOG_LEVEL | debug | Global log level (trace,debug,info,warn,error,fatal,panic) |
| --log_writer.compress | BTPSIMPLE_LOG_WRITER_COMPRESS | false | Use gzip on rotated log file |
| --log_writer.filename | BTPSIMPLE_LOG_WRITER_FILENAME |  | Log file name (rotated files resides in same directory) |
| --log_writer.localtime | BTPSIMPLE_LOG_WRITER_LOCALTIME | false | Use localtime on rotated log file instead of UTC |
| --log_writer.maxage | BTPSIMPLE_LOG_WRITER_MAXAGE | 0 | Maximum age of log file in day |
| --log_writer.maxbackups | BTPSIMPLE_LOG_WRITER_MAXBACKUPS | 0 | Maximum number of backups |
| --log_writer.maxsize | BTPSIMPLE_LOG_WRITER_MAXSIZE | 100 | Maximum log file size in MiB |
| --offset | BTPSIMPLE_OFFSET | 0 | Offset of MTA |
| --src.address | BTPSIMPLE_SRC_ADDRESS |  | BTP Address of source blockchain (PROTOCOL://NID.BLOCKCHAIN/BMC) |
| --src.endpoint | BTPSIMPLE_SRC_ENDPOINT |  | Endpoint of source blockchain |
| --src.options | BTPSIMPLE_SRC_OPTIONS | [] | Options, comma-separated 'key=value' |

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

