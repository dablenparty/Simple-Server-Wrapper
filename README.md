# Simple Server Wrapper (SSW)

A native Java wrapper for Minecraft servers. Works on Windows, Linux (tested on Ubuntu), and macOSX

---

## About

I host Minecraft servers off of a second PC for my friends and I to play on. More often than not, they're very heavily
modded and require manual restarting or other tasks that are annoying to do. To help with that, I created this. I'm
aware there are other wrappers out there with way more features, but I just needed a very basic feature-set that was
portable between versions, types, and even multiple servers. A few weeks later, out pops a releasable version of
Simple Server Wrapper

---

## Features

### Current

Features currently included in SSW

* Support for any version or type of Minecraft server (Vanilla, Spigot, Forge, etc.)

* Per-server settings

* Automatic restarting

* Automatically close/reopen a server when nobody is on/tries to connect

* Custom memory slider

* Extra server arguments (memory allocation is coded in already)

* Batch file (launch.bat) generation (text file on macOSX/Linux)

* CurseForge Modpack installation into an existing server

* Edit server properties

### Future

These are features I want to add but either haven't gotten to them yet or don't have the capabilities right now

* [x] ~~Edit server.properties~~

* [ ] Mod/plugin list for supported server types

* [x] ~~Batch file (launch.bat) generation~~

* [ ] Realtime player data (either through mod or server)

* [x] ~~Automatically close/reopen a server when nobody is on~~

* [ ] "Hub" of sorts for adding/removing/managing servers (*I want everything else done before I even start this*)

I'm sure I'll think of more

---

## Troubleshooting

### Settings file

Don't manually edit the settings file. It's controlled entirely through the server wrapper itself, and the server does
not have to be running for its settings to be changed. *If you decide to do it anyways,* and it detects an error, it 
will probably overwrite the file with the default settings. If it doesn't detect an error but still breaks, delete the
file and let the wrapper create a new one

Also, don't move it out of the "ssw" folder or rename it. The wrapper specifically looks to that folder for that
specific file. If it doesn't find the correct file in there, it WILL create a new one and use that instead

### Server won't close when application does

If a server is running when you try to close this application, it will warn you and it can close the server before
closing itself. **There are rare cases I have found where this does not work as intended.** Most of these cases I've
accounted for, and the server process is destroyed by the wrapper. **If that still doesn't work, the problem is likely
with your server or PC, not this application.** Use Task Manager to close the server process.

### Internal Errors/Forge errors

In my experience, these are heavily correlated. It is *almost always* caused by a bad Java version. Both Forge servers
and this wrapper are made on **Java 8** and should be run with such. If you don't know what version you have, run the
command `java -version` in a terminal, and ensure the version number begins with `1.8`. So far, this has solved the
issue every single time.
