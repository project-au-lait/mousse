# Mousse

Mousse is a lightweight Java utility library that provides the following features:

- **BeanUtils**: Bean mapping between different types using ModelMapper
- **JsonUtils**: JSON serialization and deserialization using Jackson
- **RestClient**: HTTP REST client using Java's built-in `HttpClient`

## Required Software

- Java 21+
- Maven

## Usage

To use Mousse, add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>dev.aulait.mousse</groupId>
  <artifactId>mousse-util</artifactId>
  <version>0.8</version>
</dependency>
```

### BeanUtils

`BeanUtils` provides static methods to map objects between different types using [ModelMapper](https://modelmapper.org/).

#### Basic mapping

```java
import dev.aulait.mousse.util.BeanUtils;

UserEntity entity = userRepository.findById(id);

// Map to a different type
UserDto dto = BeanUtils.map(entity, UserDto.class);
```

#### Mapping generic types

Use `BeanType` when mapping to generic types such as `List<T>`:

```java
import dev.aulait.mousse.util.BeanUtils;
import dev.aulait.mousse.util.BeanType;

Set<UserEntity> entities = userRepository.findAll();

List<UserDto> dtos = BeanUtils.map(entities, new BeanType<List<UserDto>>() {});
```

#### Custom type maps

Register a named type map to customize mapping behavior:

```java
import dev.aulait.mousse.util.BeanUtils;

BeanUtils.registerTypeMap(UserEntity.class, UserDto.class, "UserEntityToUserDto")
    .addMappings(mapper -> mapper.skip(UserDto::setPassword))
    .implicitMappings();

UserDto dto = BeanUtils.map(entity, UserDto.class, "UserEntityToUserDto");
// dto.getPassword() == null
```

---

### JsonUtils

`JsonUtils` provides static methods for JSON serialization and deserialization using [Jackson](https://github.com/FasterXML/jackson).

#### Convert object to JSON string

```java
import dev.aulait.mousse.util.JsonUtils;

UserDto dto = new UserDto("alice", "alice@example.com");

String json = JsonUtils.obj2str(dto);
// {"name":"alice","email":"alice@example.com"}
```

#### Convert JSON string to object

```java
import dev.aulait.mousse.util.JsonUtils;
import dev.aulait.mousse.util.JsonType;

String json = "[{\"name\":\"alice\"},{\"name\":\"bob\"}]";

List<UserDto> users = JsonUtils.str2obj(json, new JsonType<List<UserDto>>() {});
```

#### Read from / write to file

```java
import dev.aulait.mousse.util.JsonUtils;
import java.nio.file.Path;

// Write
JsonUtils.obj2file(dto, Path.of("user.json"));

// Read
UserDto dto = JsonUtils.file2obj(Path.of("user.json"), UserDto.class);
```

---

### RestClient

`RestClient` is an HTTP client that supports GET, POST, PUT, and DELETE operations with automatic JSON serialization/deserialization.

#### Setup

```java
import dev.aulait.mousse.util.restclient.RestClient;

RestClient client = new RestClient("https://api.example.com");

// Optional: set an access token for Bearer authentication
client.setAccessToken(accessToken);
```

#### GET

```java
// Single object
UserDto user = client.get("/users/{id}", UserDto.class, userId);

// List
List<UserDto> users = client.getAsList("/users", new JsonType<List<UserDto>>() {});

// Binary (e.g. file download)
byte[] bytes = client.getAsByte("/files/{id}", fileId);
```

#### POST

```java
UserDto created = client.post("/users", newUser, UserDto.class);
```

#### Multipart POST

```java
import java.nio.file.Path;
import java.util.Map;

Map<String, Object> parts = new LinkedHashMap<>();
parts.put("file", Path.of("document.pdf"));
parts.put("description", "My document");

UploadResult result = client.postMultipart("/upload", parts, UploadResult.class);
```

#### PUT

```java
UserDto updated = client.put("/users/{id}", updatedUser, UserDto.class, userId);
```

#### DELETE

```java
client.delete("/users/{id}", null, Void.class, userId);
```

#### Error handling

`RestClient` throws `RestClientException` when the response status is not 2xx:

```java
import dev.aulait.mousse.util.restclient.RestClientException;

try {
    UserDto user = client.get("/users/{id}", UserDto.class, userId);
} catch (RestClientException e) {
    int status = e.getStatusCode();
    String body = e.getBody();
}
```

#### Logging and header masking

##### Configuration prerequisites

The logging filters read settings through the **MicroProfile Config API**, using
`ConfigProviderResolver` and `Config.getOptionalValue`. Mousse declares
`org.eclipse.microprofile.config:microprofile-config-api:3.1.1` as a provided,
optional dependency; it does not bundle a configuration implementation.
Even when no logging settings are specified, using these filters requires both
the API and a compatible implementation at runtime.

[MicroProfile Config](https://download.eclipse.org/microprofile/microprofile-config-3.1/microprofile-config-spec-3.1.html)
defines the configuration API, provider discovery, type conversion (including
comma-separated values to arrays), and default configuration sources:

- Java system properties
- Environment variables
- `META-INF/microprofile-config.properties` on the classpath

Mousse's property names, supported log items,
`none` handling, and masking behavior are Mousse-specific, not part of that specification.

Use a MicroProfile Config implementation compatible with the API version above,
such as **SmallRye Config**, or a runtime that already provides one. An API-only
dependency is insufficient. A different configuration library is usable only if
it also exposes settings through a MicroProfile Config provider or `ConfigSource`;
merely supporting properties files is not enough.

For a standalone Java application, one option is to add SmallRye Config Core:

```xml
<dependency>
  <groupId>io.smallrye.config</groupId>
  <artifactId>smallrye-config-core</artifactId>
  <version>3.12.4</version>
</dependency>
```

This supplies an implementation and its transitive API dependency. If your runtime
already provides MicroProfile Config (for example, Quarkus), use its managed
dependencies rather than adding a second implementation.
The filters also require SLF4J API and an application-selected logging provider.

##### Configuration file location

For a standard Maven application, place settings in
`src/main/resources/META-INF/microprofile-config.properties`. Maven copies this to
the classpath as `META-INF/microprofile-config.properties`, where a MicroProfile
Config implementation discovers it automatically.

`application.properties` is **not** a standard MicroProfile Config file name.
Frameworks may support additional sources: for example, Quarkus reads
`src/main/resources/application.properties` and exposes its values through
MicroProfile Config. Outside such a framework, use the standard path above unless
you explicitly configure another source.

##### Registering filters

Register logging filters to log request and response details:

```java
import dev.aulait.mousse.util.restclient.RequestLoggingFilter;
import dev.aulait.mousse.util.restclient.ResponseLoggingFilter;

RestClient client = RestClient.builder()
    .baseUrl("https://api.example.com")
    .filter(new RequestLoggingFilter())
    .filter(new ResponseLoggingFilter())
    .build();
```

Select request and response log items with comma-separated lists in
`META-INF/microprofile-config.properties` (or your framework's supported configuration source):

```properties
mousse.rest-client.logging.request.items=method,uri
mousse.rest-client.logging.response.items=status,body
```

| Item     | Request     | Response    | Log level |
| -------- | ----------- | ----------- | --------- |
| `status` |             | Status code | INFO      |
| `header` | Headers     | Headers     | INFO      |
| `body`   | Body        | Body        | DEBUG     |
| `method` | HTTP method |             | INFO      |
| `uri`    | Request URI |             | INFO      |

If omitted or empty, all items for that direction are enabled.
Item names are case-insensitive. Enable DEBUG logging to include bodies.

Set `none` by itself to disable all log items for that direction:

```properties
mousse.rest-client.logging.request.items=none
mousse.rest-client.logging.response.items=none
```

Configure the headers to mask in the same configuration file:

```properties
mousse.rest-client.logging.masked-headers=Authorization,Cookie,Set-Cookie,X-Api-Key
```

Both filters replace the listed headers' values with `<hidden>` in logs only;
the actual request and response headers remain unchanged.
Header names are case-insensitive. If omitted or empty, no headers are masked.
Bodies and URIs are not masked.
