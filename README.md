# Simple Server Wrapper (SSW)

A native Java wrapper for Minecraft servers. Works on Windows, Linux, and macOSX

---

## About

I host Minecraft servers off of a second PC (or cloud VM) for my friends and I to play on. More often than not, they're
very heavily modded and require restarting and other tasks that are annoying to do. I get that there are other wrappers
out there that are better made and have more features, but I wanted a project with a practical use case that I could
also use as an introduction to GUI programming (and Java in general).

---

## Features

### Current

Features currently included in SSW

* Support for any version or type of Minecraft server (Vanilla, Spigot, Forge, etc.)

* Automatically patches [Log4Shell](https://en.wikipedia.org/wiki/Log4Shell) to ensure the server is safe to play on

* GUI & CLI

* Per-server settings

* Automatic restarting

* Automatically close a server when nobody is online and restart it when someone tries to connect

* Set server memory

* Manage server properties *(GUI only)*

* Extra server arguments (memory allocation is hard-coded in already)

* Batch file (launch.bat) generation (.sh file on macOSX/Linux)

* CurseForge Modpack installation into an existing server

### Planned

* Optimize automatic starting on connection by making a full proxy server between the client and server

* Specify JVM to run server with

* Better logging

---

## CLI Commands

A list of commands currently provided by the CLI

* `start`: Starts the Minecraft server.
* `logout`: Disconnect the client from the server.
* `exit`: Alias for `logout`.
* `log`: Prints out the most recent log from the Minecraft server.
* `debuglog`: Prints out the SSW debug log.
* `close`: Disconnects the client from the server and closes the server (if there are no other clients connected).

Any commands not immediately recognized by the wrapper are passed on to the Minecraft server (if it's running).

---

## Troubleshooting

### Settings file

The settings file is stored at `<server folder>/ssw/wrapperSettings.json`.

If you're using the GUI, just don't manually edit this file. Use the settings menu.

Otherwise, use this guide:

* `memory`: How much memory (in GB) to allocate to the server. Google this if you don't know what to put here.
* `extraArgs`: An array of extra arguments to pass to the server. Note that the memory args are automatically passed by
  the wrapper. I recommend using those referenced in
  [this reddit post](https://www.reddit.com/r/feedthebeast/comments/5jhuk9/modded_mc_and_memory_usage_a_history_with_a/)
  .
* `autoRestart`: I wanted to deprecate this but forgot my friends have no life and grind the game for hours, so this
  defines whether the wrapper should automatically restart after a given interval.
* `restartInterval`: How long (in hours) the server should run before the wrapper automatically restarts it.
* `autoShutdown`: Whether the server should automatically shut down the server. This is also what determines whether the
  wrapper will automatically start the server again when someone tries to connect on its port.
* `shutdownInterval`: How long (in minutes) the server should remain without players before shutting down.
* `version`: The Minecraft server version (e.g., `1.18.1`). If your server is on `1.14` or later, the wrapper can
  auto-detect the version from the server JAR file. Otherwise, both the GUI and CLI will prompt you to set it before you
  can do anything

Note: don't move it out of the "ssw" folder or rename it. The wrapper looks to that specific folder for that specific
file. If it doesn't find the correct file in there, it will create a new one and use that instead

### Server won't close when application does

If a server is running when you try to close the GUI application, it will warn you, and it can close the server before
closing itself. The CLI will just shut off the server. **There are rare cases I have found where this does not work as
intended.** Most of these cases I've accounted for, and the server process is destroyed by the wrapper. If that still
doesn't work, use Task Manager (Windows), Activity Monitor (macOSX), or the terminal (Linux) to kill the server process
yourself. Most likely, the issue was not my application, but the log would give more info.

### A note about Forge

In my experience, Forge servers simply will not work sometimes. I do not know the cause and none of my attempts to fix
it have succeeded. I've noticed before that running it with `Java 1.8` can sometimes fix this, but as of right now, this
application uses `Java 17` to ensure any server can be launched and does not support running the server with a different
JVM than the application was launched with.

## Credits

* Now on [JavaFX](https://openjfx.io/)!

* [Apache Commons IO](https://commons.apache.org/proper/commons-io/)

* [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/)

* [Argparse4j](https://argparse4j.github.io/)

* [Eclipse Jersey](https://eclipse-ee4j.github.io/jersey/)

* [gson](https://github.com/google/gson)

* [Zip4j](https://github.com/srikanth-lingala/zip4j)
