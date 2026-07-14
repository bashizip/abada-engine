# API Pagination

Abada bounds public collection reads so database growth does not turn one API
request into an unbounded load of runtime state. The first paginated endpoints
are:

- `GET /api/v1/tasks`
- `GET /api/v1/processes/instances`

Both endpoints keep their existing JSON array response body for compatibility.
They accept these query parameters:

| Parameter | Default | Valid range | Meaning |
|---|---:|---:|---|
| `page` | `0` | `0` or greater | Zero-based page number |
| `size` | `50` | `1` through `100` | Maximum rows returned |

Task lists continue to accept the optional `status` filter. Invalid page or
size values return the engine's typed `400 Bad Request` response.

Every successful page includes:

| Header | Meaning |
|---|---|
| `X-Page` | Current zero-based page |
| `X-Page-Size` | Requested page size |
| `X-Total-Count` | Total matching rows |
| `X-Total-Pages` | Total number of pages |

The headers are exposed through CORS for browser clients. Ordering is stable:
task pages use ascending creation time then task ID, while process-instance
pages use descending start time then instance ID.

Example:

```http
GET /api/v1/tasks?status=AVAILABLE&page=1&size=25
```

The response body contains at most 25 tasks. Clients should advance `page`
until it reaches `X-Total-Pages - 1`; they must not assume that an omitted page
parameter returns every matching row.
