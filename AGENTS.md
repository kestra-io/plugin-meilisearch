# Kestra Meilisearch Plugin

## What

- Provides plugin components under `io.kestra.plugin.meilisearch`.
- Includes classes such as `DocumentAdd`, `DocumentGet`, `FacetSearch`, `Search`.

## Why

- This plugin integrates Kestra with Meilisearch.
- It provides tasks that load data into and query Meilisearch indexes.

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
