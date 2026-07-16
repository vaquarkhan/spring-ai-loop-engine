# spring-ai-loop-engine-bom

Bill of Materials (BOM) for Spring AI Loop Engine modules.

Import this BOM in your `dependencyManagement` so all Loop Engine artifacts stay on the same version without repeating versions on each dependency.

## Artifact

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.vaquarkhan</groupId>
      <artifactId>spring-ai-loop-engine-bom</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

## Managed modules

- `spring-ai-loop-engine-core`
- `spring-ai-loop-engine-agui`
- `spring-ai-loop-engine-a2a`
- `spring-ai-loop-engine-mcp`
- `spring-ai-loop-engine-integrity`
- `spring-ai-loop-engine-observability`
- `spring-ai-starter-loop-engine`

## Typical app usage

```xml
<dependencies>
  <dependency>
    <groupId>io.github.vaquarkhan</groupId>
    <artifactId>spring-ai-starter-loop-engine</artifactId>
  </dependency>
</dependencies>
```

Version comes from the BOM import.
