# Simple Server Wrapper (SSW)

A native Java wrapper for Minecraft servers. Works on Windows, Linux (tested on Ubuntu), and macOSX

---

## About

I host Minecraft servers off of a second PC (or cloud VM) for my friends and I to play on. More often than not, they're
very heavily modded and require manual restarting or other tasks that are annoying to do. To help with that, I created
this. I'm aware there are other wrappers out there with way more features, but I just needed a very basic feature-set
that was portable between versions, types, and even multiple servers. A few weeks later, out pops a releasable version
of Simple Server Wrapper

---

## Features

### Current

Features currently included in SSW

* Support for any version or type of Minecraft server (Vanilla, Spigot, Forge, etc.)

* GUI or CLI

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

The settings file is stored at `<minecraft server folder>/ssw/wrapperSettings.json`.

If you're using the GUI, just don't manually edit this file. Use the settings menu.

Otherwise, use this guide:

* `memory`: How much memory (in MB) to allocate to the server. Google this if you don't know what to put here.
* `extraArgs`: An array of extra arguments to pass to the server. Note that the memory args are automatically passed by
  the wrapper. I recommend using those referenced in
  [this reddit post](https://www.reddit.com/r/feedthebeast/comments/5jhuk9/modded_mc_and_memory_usage_a_history_with_a/)
  .
* `autoRestart`: Eventually I'm going to deprecate this (the CLI doesn't even support it) but this defines whether the
  wrapper should automatically restart after a given interval.
* `restartInterval`: How long (in hours) the server should run before the wrapper automatically restarts it.
* `autoShutdown`: Whether the server should automatically shut down the server. This is also what determines whether the
  wrapper will automatically start the server again when someone tries to connect on its port.
* `shutdownInterval`: How long (in minutes) the server should remain without players before shutting down.

Note: don't move it out of the "ssw" folder or rename it. The wrapper looks to that specific folder for that specific
file. If it doesn't find the correct file in there, it WILL create a new one and use that instead

### Server won't close when application does

If a server is running when you try to close this application, it will warn you, and it can close the server before
closing itself. **There are rare cases I have found where this does not work as intended.** Most of these cases I've
accounted for, and the server process is destroyed by the wrapper. If that still doesn't work, use Task Manager
(Windows), Activity Monitor (macOSX), or the terminal (Linux) to kill the server process yourself. Most likely, the
issue was not my application, but the log would give more info.

### Internal Errors/Forge errors

In my experience, these are heavily correlated. It is *almost always* caused by a bad Java version. Forge servers are
made on **Java 8** and for whatever reason get pissy when they're not run with such. If you don't know what version you
have, run the command `java -version` in a terminal, and ensure the version number begins with `1.8`. So far, this has
solved the issue every single time. If you're using Forge on Minecraft 1.17 or later and have this issue, I have no
clue. I haven't used that myself, and you probably shouldn't be either. Use Fabric.
