# Packaging Guide

Back to entry documents: [../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

## Overview

The project includes Maven packaging configuration for desktop delivery. The main class is `com.xuesinuo.muppet.UiStarter`, and Maven profiles are present for Windows and macOS packaging using `jpackage-maven-plugin`.

## Common Requirements

- JDK 21
- Maven
- A supported `jpackage` environment from the installed JDK
- Platform-specific icon resources already present in `src/main/resources`

## Build The Application JAR

```bash
mvn clean package
```

This produces the Spring Boot application JAR under `target/`.

## Windows Packaging

The `windows` Maven profile configures:

- application name: `MuppetPrint`
- output directory: `target/dist`
- icon: `src/main/resources/app.ico`
- Start Menu and shortcut options enabled

### Windows Example Command

```bash
mvn clean package -Pwindows
```

### Windows Notes

- Packaging must run on Windows or in an environment that supports Windows packaging.
- The resulting package is intended to expose a desktop app with GUI behavior.

## macOS Packaging

The `macos` Maven profile configures:

- application name: `MuppetPrint`
- output directory: `target/dist`
- icon: `src/main/resources/app.icns`

### macOS Example Command

```bash
mvn clean package -Pmacos
```

### macOS Notes

- Packaging must run on macOS with a JDK that provides `jpackage`.
- The UI includes macOS auto-start support via a LaunchAgent file.

## Linux Packaging

There is no dedicated Linux `jpackage` Maven profile in `pom.xml`.

Practical options include:

- run the JAR directly on Linux with JDK 21
- add a Linux packaging profile in a future iteration if a desktop distribution format is required

## Packaging Validation Checklist

- The desktop UI starts correctly.
- The tray behavior matches the target platform expectations.
- The configured port can be changed and the service starts.
- `/api/getAllPrinters` returns local printers.
- HTML and PDF print flows work with the target printers.
- Auto-start behavior is verified on the target operating system.

## Future Packaging Documentation

If the project adds Linux native packaging, managed deployment installers, or cluster transport components, this guide should be expanded accordingly.
