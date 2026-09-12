# Optional Elasticsearch configuration

Elasticsearch is disabled by default through `elasticsearch.enabled: ${ELASTICSEARCH_ENABLED:false}` in `application.yml`. Set `ELASTICSEARCH_ENABLED=true` to enable the optional integration. Automatic Spring Data Elasticsearch repository discovery is disabled; the project's conditional repository configuration controls repository activation.

The maintained `GET /api/search/messages?q=...` endpoint searches MySQL and applies the authenticated user's conversation access before returning results. Elasticsearch indexing is not required for that message search. The message-search response uses the backend API envelope with `data.list` and `data.total`; see [the backend guide](../../../../docs/backend.md).

The optional `MessageDocument` model targets the `message` index and defines `id`, `fromId`, `chatListId`, `content` and `sendTime`. Its content field is plain text without an explicit IK analyzer. Installing an IK plugin alone does not configure this model to use it, and this repository does not guarantee IK-based search behavior.

Configure the server URL through `ELASTICSEARCH_URL`, which defaults to `http://localhost:9200`. The current HTTP branch of `ElasticsearchConfig` does not attach credentials. For the HTTPS branch, supply `ELASTICSEARCH_USERNAME` and `ELASTICSEARCH_PASSWORD`; both are required by that branch.

HTTPS also requires a trusted CA certificate. Set the Spring property `elasticsearch.ca-path` to its external path, for example through `ELASTICSEARCH_CA_PATH`. The loader accepts a filesystem path or a `file:` path; its fallback is `classpath:es/http_ca.crt`. The application builds its TLS trust from the configured certificate rather than disabling certificate validation.

Keep credentials, private keys and PKCS#12 files out of source control and build resources. The example environment file is documentation and is not automatically loaded by the backend. Deployment must supply its own values and complete any required credential rotation; see [the operations guide](../../../../docs/operations.md).
