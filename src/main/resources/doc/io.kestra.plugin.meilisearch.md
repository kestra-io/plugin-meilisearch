# How to use the Meilisearch plugin

Add, retrieve, delete, and search documents in Meilisearch from Kestra flows.

## Authentication

Set `url` to your Meilisearch instance URL and `key` to your API key (both required). Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and set connection properties on each task.

## Tasks

`DocumentAdd` indexes documents into an `index` — set `from` to a `kestra://` URI or an inline list of document maps.

`DocumentGet` retrieves a single document by `documentId` from an `index`.

`DocumentDelete` deletes documents from an `index` by a single `documentId`, a list of `documentIds`, a `filter` expression, or all documents with `deleteAll: true`. Exactly one deletion target must be specified. Note that filter expressions require the filtered attributes to be declared in the index's `filterableAttributes`.

`Search` runs a full-text search — set `query` and optionally scope to an `index`.

`FacetSearch` runs a faceted search on an `index` — set `facetName` (required) and optionally `facetQuery` and `filters` (a list of filter expressions).
