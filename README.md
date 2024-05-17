
# anysync

A service to transfer the exact structure of a directory to another place.  
It consists of a rust server that acts as a main source of data, and clients (android kotlin app and rust cli), that communicate with http, and transfer files via websockets.  
I created it because I could not find a easy solution to sync my files on a android phone with files on my computer.  
Server is tested and built on linux, it may not work on windows.


## Features

#### server

- [x]  get list of files  
- [x]  get / set files  
- [x]  config file and cli params  
- [x]  multiple sources  
- [x]  one-way allowed transfers  
- [ ]  per-user permissions to sources

#### client

- [x]  get list of files  
- [x]  get / set files  
- [x]  config file and cli params  
- [ ]  multiple sources  

#### app

- [x]  get list of files  
- [x]  get / set files  
- [x]  multiple sources  
- [x]  add sources in-app
- [x]  persistent sources
- [ ]  scheduled sync
- [ ]  transfer only necesary files  
- [ ]  better support for scoped storage


## Run Locally

clone the project

```bash
  git clone git@github.com:NikodemMarek/anysync.git
```

go to the project directory

```bash
  cd anysync
```

#### server

go to the server directory

```bash
  cd server
```

install dependencies

```bash
  cargo install
```

start the server

```bash
  cargo run
```

#### client

currently not working

#### app

you can run the app from android studio

or

go to the app directory

```bash
  cd app
```

connect a physical device and enable file transfer and usb debugging

start adb server

```bash
  adb server
```

run gradle task

```bash
  ./gradlew installDebug
```


## Configuration

#### server

```bash
port = 5050

[sources.name]
name = "name"
path = "path/to/source"
actions = "none" # none, get, set, getset
```

#### client

currently not working

