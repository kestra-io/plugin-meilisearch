# Kestra Meilisearch Plugin

## What

- Provides plugin components under `io.kestra.plugin.meilisearch`.
- Includes classes such as `DocumentAdd`, `DocumentGet`, `FacetSearch`, `Search`.

## Why

- What user problem does this solve? Teams need to load data into and query Meilisearch indexes from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Meilisearch steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Meilisearch.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `meilisearch`

Infrastructure dependencies (Docker Compose services):

- `meilisearch`

### Key Plugin Classes

- `io.kestra.plugin.meilisearch.DocumentAdd`
- `io.kestra.plugin.meilisearch.DocumentGet`
- `io.kestra.plugin.meilisearch.FacetSearch`
- `io.kestra.plugin.meilisearch.Search`

### Project Structure

```
plugin-meilisearch/
├── src/main/java/io/kestra/plugin/meilisearch/
├── src/test/java/io/kestra/plugin/meilisearch/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
