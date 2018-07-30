Inferno Project protocol
========================

```
Revision: 1.0
Date: 30 July 2018
```

This document describes Inferno Project data transfer protocol

## Transport level protocol and ports

Inferno Project protocol is built on top of TCP protocol.

### Ports

* ***3274*** - realm server port
* ***8085*** - world server port

## Inferno Project protocol structure

### Packet structure

```
+----+----+----+----+----+----+----+----+
|       Protocol Version (1 byte)       |
+----+----+----+----+----+----+----+----+
|         Payload Size (4 bytes)        |
+----+----+----+----+----+----+----+----+
|                Payload                |
+----+----+----+----+----+----+----+----+
```

#### Payload structure

```
+----+----+----+----+----+----+----+----+
|        Encryption key (8 bytes)       |
+----+----+----+----+----+----+----+----+
|             Encrypted data            |
+----+----+----+----+----+----+----+----+
```

#### Encryption

Inferno Project protocol payload is encrypted using simple XOR algorithm with dynamic key.

Encryption key in packet is XOR encoded string with key ***0x55***

#### Data structure

##### Request

```
+----+----+----+----+----+----+----+----+
|        Operation Code (1 byte)        |
+----+----+----+----+----+----+----+----+
|              Data Wrapper             |
+----+----+----+----+----+----+----+----+
```

##### Response

```
+----+----+----+----+----+----+----+----+
|        Operation Code (1 byte)        |
+----+----+----+----+----+----+----+----+
|          Status Code (1 byte)         |
+----+----+----+----+----+----+----+----+
|              Data Wrapper             |
+----+----+----+----+----+----+----+----+
```

#### Data Wrapper

Data Wrapper is described in ***data_wrapper.md***