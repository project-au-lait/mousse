# Mousse

Mousse is a lightweight Java utility library that provides the following features:

- **BeanUtils**: Bean mapping between different types using ModelMapper
- **JsonUtils**: JSON serialization and deserialization using Jackson
- **RestClient**: HTTP REST client using Java's built-in `HttpClient`

## Required Software

- Java 21+
- Maven

## Usage

Mousse is available on the [GitHub Packages](https://github.com/orgs/project-au-lait/packages?repo_name=mousse).
To use Mousse, add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>dev.aulait.mousse</groupId>
  <artifactId>mousse-util</artifactId>
  <version>0.8-SNAPSHOT</version>
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
import dev.aulait.mousse.util.RestClient;

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
import dev.aulait.mousse.util.RestClientException;

try {
    UserDto user = client.get("/users/{id}", UserDto.class, userId);
} catch (RestClientException e) {
    int status = e.getStatusCode();
    String body = e.getBody();
}
```
