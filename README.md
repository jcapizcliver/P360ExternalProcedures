# P360ExternalProcedures

This repository contains P360ExternalProcedures, the shared integration and processing layer for the P360 middleware. It contains reusable services, P360 API clients, file processors, messaging helpers, and operational utilities used by P360ContingencyServices.

## Overview

The project centralizes the logic that communicates with P360 and adjacent systems. It is used by servlet endpoints from P360ContingencyServices and also contains standalone utilities for controlled operational, migration, maintenance, and data preparation tasks.

The runtime-facing code is concentrated in the main P360 service packages. A significant portion of the repository consists of manual tools and temporary utilities; those should be treated carefully and validated before reuse.

## Project Structure

```text
src/
  mx/com/liverpool/p360/services/core/       Main P360 service and integration logic
  mx/com/liverpool/p360/services/core/amqp/  Messaging-related processors
  mx/com/liverpool/p360/services/core/sftp/  SFTP and file exchange helpers
  mx/com/liverpool/p360/services/core/xml/   XML parsing and processing helpers
  mx/com/liverpool/p360/services/core/dq/    Data quality rules
  mx/com/liverpool/dataprofiling/            Data profiling and preparation utilities
bin/                                         Generated Eclipse build output
```

## Main Responsibilities

- Build and process P360 proposal payloads.
- Retrieve and transform template, catalog, lookup, and product metadata.
- Execute REST calls to P360 through shared request wrappers.
- Process files from STEP, XML, CSV, SFTP, and related operational flows.
- Publish or prepare messages for downstream integration channels.
- Provide maintenance and data correction utilities for controlled use.

## Core Design

P360ExternalProcedures uses a small set of shared infrastructure classes to keep integration code consistent:

- `RESTWorkshop` builds and executes HTTP requests against P360 APIs.
- `RESTWrapper` provides higher-level collection, write, and delete helpers.
- `PropertiesManager` centralizes runtime configuration access.
- Processor interfaces and helper classes handle reusable response, write, delete, XML, and CSV workflows.

Business flows should reuse these shared components instead of creating unrelated HTTP, configuration, or parsing mechanisms.

## Build and Runtime

This project does not use Maven or Gradle. It is built as an Eclipse Java project.

1. Import P360ExternalProcedures. In Eclipse, the local project name may appear as `Memelos`.
2. Verify that required local libraries are available in the Eclipse classpath.
3. Build this project before deploying P360ContingencyServices.

The project targets JavaSE-9 in its Eclipse classpath and relies on local dependencies configured outside the repository.

## Configuration

Runtime configuration is externalized and read through `PropertiesManager`. Configuration may include P360 connectivity, authentication, cache directories, SFTP endpoints, messaging settings, feature flags, and file-processing locations.

Do not commit credentials, tokens, private keys, service account files, generated data extracts, production payloads, or environment-specific paths.

## Development Notes

Preserve existing JSON contracts and field names; several servlet responses and downstream consumers depend on them. When changing large flows, prefer small helper methods and narrowly scoped modifications.

Useful discovery commands:

```powershell
rg -n "class CreateProposal|class GetTemplateInformation|class RESTWorkshop|class RESTWrapper" src
rg -n "public static void main\(" src
```

Before running write or maintenance utilities, confirm the target environment, input files, expected side effects, and rollback or cleanup approach.

## Environment Warnings

Many utilities assume a prepared operating environment with local libraries, configuration files, cache directories, network access, and integration credentials. A class with a `main` method is not necessarily safe for general execution; review inputs, outputs, and P360 side effects before running it.

## Maintainers

- Juan Capiz | jcapizc@liverpool.com.mx
- Alfonso de la Rosa | jose.aguirre@isol.dev
