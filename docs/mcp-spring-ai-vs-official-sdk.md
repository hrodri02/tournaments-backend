# Serving MCP: Spring AI's starter, or the official SDK directly

Two branches, one server. `feature/mcp-separate-process` serves the tournaments MCP server with
`org.springframework.ai:spring-ai-starter-mcp-server-webmvc:2.0.0`;
`feature/mcp-separate-process-offical-sdk` serves the identical thing with
`io.modelcontextprotocol.sdk:mcp:2.0.0` and no Spring AI at all. Everything else is held constant —
same three tools, same nginx route, same compose file, same Dockerfile, same backend client — so the
diff between the branches is only ever the answer to one question.

```
git diff feature/mcp-separate-process..feature/mcp-separate-process-offical-sdk
```

Spring AI wraps the official SDK; the question is whether the wrapper earns its place.

## What the answer is

**Use the SDK directly for a server like this one.** It costs about a hundred lines of plumbing,
once, plus roughly fifteen per tool. It removes 22 jars, one framework's release cadence, and a
layer of generation between what the code says and what a client sees. For a server of three read
tools the trade is close to even; for one of ten it is still fine, because the per-tool cost is a
JSON schema, and a JSON schema is worth writing on purpose.

The one thing the wrapper was genuinely doing for free is the thing most likely to be missed on the
way out — see *The trap*, below.

## The criteria

| Criterion | Spring AI 2.0.0 starter | Official SDK 2.0.0 | Verdict |
|---|---|---|---|
| Streamable/stateless HTTP server | Yes, `protocol=STATELESS` | Yes, `HttpServletStatelessServerTransport` | Tie — same class does the work either way |
| Boot 4.1 / Java 23 compatible | Yes | Yes | Tie |
| Per-request identity reaches a tool | `contextExtractor` on the transport | Same hook, `HttpServletRequest` instead of `ServerRequest` | Tie |
| Sits in the servlet filter chain | Yes, a `RouterFunction` | Yes, a plain `HttpServlet` | Tie, and the servlet is the simpler thing to reason about |
| Dependency weight | 102 artifacts | 80 artifacts | **SDK**, by 22 jars |
| Effort to the first tool | One annotation | ~100 lines of wiring | **Spring AI** |
| Effort per additional tool | One annotation | ~15 lines, mostly schema | **Spring AI**, but narrowly |
| Errors reaching the model usefully | Free | Must be written, and it is silent when forgotten | **Spring AI** |
| Testability, no network | Fine | Fine, and the plumbing is now unit-testable | Slight edge to the SDK |
| Maintenance over 2–3 years | Tied to Spring AI's line and its Boot floor | Tied to the protocol's own SDK | **SDK** |

## Dependency weight

`mvn dependency:list`, same module, same everything else: **102 artifacts before, 80 after. Nothing
added.** The SDK jars were already on the classpath — Spring AI was pulling them in. Dropping the
starter drops only the wrapper and what the wrapper needed:

```
org.springframework.ai:spring-ai-starter-mcp-server-webmvc   the starter
org.springframework.ai:spring-ai-autoconfigure-mcp-server-webmvc
org.springframework.ai:spring-ai-autoconfigure-mcp-server-common
org.springframework.ai:spring-ai-mcp
org.springframework.ai:spring-ai-mcp-annotations
org.springframework.ai:mcp-spring-webmvc
org.springframework.ai:spring-ai-model                       and, beneath it:
org.springframework.ai:spring-ai-commons
org.springframework.ai:spring-ai-template-st                 StringTemplate, for prompt templates
org.antlr:ST4, org.antlr:antlr-runtime, org.antlr:antlr4-runtime
com.knuddels:jtokkit                                         an OpenAI tokenizer
io.micrometer:micrometer-core, io.micrometer:context-propagation, org.hdrhistogram:HdrHistogram
com.github.victools:jsonschema-generator + -module-jackson + -module-swagger-2
io.swagger.core.v3:swagger-annotations-jakarta, com.fasterxml:classmate
org.springframework:spring-messaging
```

Note what that list is. A prompt-template engine, a tokenizer and a schema generator are not MCP;
they are the shared `spring-ai-model` artifact, which the MCP starter depends on because tool
callbacks live there alongside the chat abstractions. This is the concrete version of "reintroducing
half of Spring AI just for a starter" — not the whole of it, no `ChatClient` and no model starters,
but more than a protocol server has any use for.

## What the code costs

```
 mcp-server/pom.xml                              |  43 ++--
 .../tournaments_mcp/tools/McpLeagueTools.java   |  46 +---
 .../tournaments_mcp/tools/McpTeamTools.java     |  11 -
 .../tournaments_mcp/tools/ToolCatalog.java      | 166 +++++++++++++    (new)
 .../tournaments_mcp/tools/ToolResults.java      |  63 ++++++         (new)
 .../transport/McpTransportConfig.java           | 120 +++++++---
 .../src/main/resources/application.properties   |  22 +-
 .../ToolContractIntegrationTest.java            | 121 +++++++++++    (new)
 .../tournaments_mcp/tools/ToolCatalogTest.java  | 241 +++++++++++++++++++ (new)
```

Counting code lines only, comments and blanks excluded, the plumbing that Spring AI used to supply
is **203 lines**: `ToolCatalog` 111, `McpTransportConfig` 66 (against 32 before), `ToolResults` 26.
Split by how it scales:

- **One-off, ~120 lines.** Three beans in `McpTransportConfig` — build the servlet transport, register
  it at `/mcp`, bind the tools to a `McpStatelessSyncServer` — plus `ToolResults`. Paid once whether
  the server has three tools or thirty.
- **Per tool, ~15 lines.** A `Tool` with a name, a description, hints, and a JSON schema, plus a
  handler that unpacks the arguments and calls the method. Ten tools would be ~150 more lines.

Against that, the tool classes got *shorter*: `McpLeagueTools` lost 46 lines of annotation, because
the contract moved out of the method signature and into `ToolCatalog`. The methods are now ordinary
Java, and their unit tests — which were already ordinary — did not change by a character.

## The trap

This is the finding that matters most, because nothing fails when you get it wrong.

Spring AI's `SyncStatelessMcpToolMethodCallback` catches whatever a tool method throws and returns a
normal result carrying `isError: true`. The SDK does not. `DefaultMcpStatelessServerHandler` maps an
escaping exception to a JSON-RPC `error` object — a protocol-level failure, which a client is
entitled to read as the server malfunctioning rather than as a tool that ran and has something to
say. The whole design of this server's error messages ("get a new token from `POST
/api/v1/auth/login`") depends on the model actually reading them.

So every handler routes through `ToolResults.of`, and `ToolCatalogTest` asserts it once per tool
rather than once in total, because the mistake to guard against is a single handler forgetting.

A related, smaller version of the same shape: with the starter, the schema was reflected out of the
method, so `get_league(Long leagueId)` depended on `-parameters` or the model would have seen an
argument called `arg0` — which is why that pom pinned the flag explicitly, though the Boot parent
sets it anyway. The explicit pin is gone. The flag is still on and now makes no difference: the
argument names are written in the schemas, where they can be read.

## Wire-level equivalence

`tools/list` was captured from both servers and compared field by field. The three tools are
identical in name, title, description, input schema (including `$schema`, `format: int64` on
`leagueId`, and the `LeagueStatus` enum) and annotations, with **one intentional difference**:
Spring AI emits `"annotations": {"title": ""}`, an empty string, and this branch omits the field.
An empty title is an artifact of generation, and a client is better off with no title than a blank
one shadowing the name.

Error results were compared too, and here the SDK version reads better by accident of doing less:

| | Spring AI | Official SDK |
|---|---|---|
| No `Authorization` header | `Error invoking method: listMyTeams\nThis request carried no Authorization header. …` | `This request carried no Authorization header. …` |
| Bad argument type | `Tool (get_league) input validation failed: … string found, integer expected` | identical |
| Backend unreachable | `Error invoking method: listLeagues\nnull` | `I/O error on GET request for "http://localhost/api/v1/leagues": null` |

The `Error invoking method: <javaMethodName>` prefix is the wrapper naming its own internals to the
model. Dropping it was not a goal; it simply is not there when the handler builds its own result.

Schema validation is unchanged and equally strict on both: the SDK's `ToolInputValidator` runs
before the handler and is on by default, so a string `leagueId` or an unknown status comes back as
an error result without any tool code running.

## What did not change

`Dockerfile`, `docker-compose.yml`, `nginx/default.conf`, `devops/local/mcp/test-http.sh`, the whole
`backend/` package, `CallerToken`, and all four original test classes — 15 tests, still passing
unmodified, including `TokenForwardingIntegrationTest`, which posts raw JSON-RPC at `/mcp` and
asserts on `isError`. That test not needing a single edit is the strongest evidence the two servers
are the same server.

The transport semantics did not change either, which is worth stating plainly: both are stateless
HTTP, each JSON-RPC call a self-contained POST, no sticky sessions, identity per request. On the
Spring AI branch that was `spring.ai.mcp.server.protocol=STATELESS` in a properties file; here it is
which class `McpTransportConfig` constructs. Three properties replace five — less configurable, but
there is no longer a property that can silently disagree with the code.

## Caveats

- This is a **server** with **read-only tools** and no resources, prompts, completions, sampling or
  elicitation. Spring AI's autoconfiguration covers all of those, and a server that needed them
  would be paying the 100-line one-off cost several times over. Re-open the question then.
- Both branches run **Boot 4.1 with Spring AI 2.0.0**. A project pinned to Boot 3.5 can only use
  Spring AI 1.1.x, which is a maintenance line — an argument for the SDK that does not even arise
  here, and one that gets stronger the longer that project stays on 3.5.
- Neither branch puts the MCP endpoint behind `ts-security` or any resource server; this process
  authenticates nobody and forwards the caller's token. A project that wants Spring Security in
  front of `/mcp` should re-check the filter-chain question. The SDK's transport being a plain
  servlet rather than a `RouterFunction` makes it, if anything, the easier of the two to secure.

## Reproducing the numbers

```bash
./mvnw -f mcp-server/pom.xml test                    # 33 green: 15 original, unmodified
./mvnw -f mcp-server/pom.xml dependency:list         # 80 artifacts, against 102 on the other branch
./devops/local/mcp/test-http.sh                      # end to end, script unmodified

git diff feature/mcp-separate-process..HEAD -- mcp-server
```
