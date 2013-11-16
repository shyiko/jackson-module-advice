# jackson-module-advice

Advisable Jackson (2.X+) data-binding.

What it means is that you can modify JSON on-the-fly while still leveraging power & simplicity of Jackson DataBind module.
This can be particularly useful when creating REST representations that follow the [HATEOAS](http://en.wikipedia.org/wiki/HATEOAS),
for example.

## Usage

1. Include Maven dependency:
```xml
<dependency>
    <groupId>com.github.shyiko</groupId>
    <artifactId>jackson-module-advice</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
> The latest development version always available through [Sonatype Snapshots](https://oss.sonatype.org/content/repositories/snapshots) repository.

2. Register [JsonAdviceModule](https://github.com/shyiko/jackson-module-advice/blob/master/src/main/java/com/github/shyiko/jackson/module/advice/JsonAdviceModule.java).
```java
ObjectMapper objectMapper = ...
objectMapper.registerModule(new JsonAdviceModule());
```

3. Annotate with [@JsonSerializerAdvice]((https://github.com/shyiko/jackson-module-advice/blob/master/src/main/java/com/github/shyiko/jackson/module/advice/JsonSerializerAdvice.java)) type(s)/field(s)/method(s) you wish to advise serialization of. Be ready to provide
implementation(s) of [AbstractBeanSerializerAdvice](https://github.com/shyiko/jackson-module-advice/blob/master/src/main/java/com/github/shyiko/jackson/module/advice/AbstractBeanSerializerAdvice.java).

## Example

Suppose we have a class named "Entity", for which JSON representation looks like `{"parentId": <number>, "id": <number>,
"name": "<string>"}`.

```java
public class Entity {

    private Long parentId;
    private Long id;
    private String name;

    // getters & setters omitted for brevity
}
```

Imagine we want to embedded links which would allow users to navigate through the entities without the need to know how
to construct the URLs. In our case what we want is something like:

```json
{
    "links": [
        {
            "rel" : "parent",
            "href" : "http://somewhere-in-the-wonderland/entities/<parentId>"
        },
        {
            "rel" : "self",
            "href" : "http://somewhere-in-the-wonderland/entities/<id>"
        }
    ]
    "parentId": <number>,
    "id": <number>,
    "name": "<string>"
}
```

Sure, there are multiple ways to accomplish it (by adding "links" field to the Entity (which
may or may not be possible depending on whether you have an access to the Entity's source code + doesn't feel right
considering that links are "not really" part of the Entity itself but rather of some request-specific representation),
defining custom serializer (which stops being fun once number of fields grows or changes become too frequent), etc).
JsonAdviceModule brings another way which may seem more appropriate.

So, in this case we'll go with the mixin + @JsonSerializerAdvice.

```java
@JsonSerializerAdvice(EntityMixin.EntitySerializerAdvice.class)
public interface EntityMixin {

    public static class EntitySerializerAdvice extends AbstractBeanSerializerAdvice {

        @Override
        public void before(T bean, JsonGenerator json, SerializerProvider provider) throws IOException {
            List<Link> links = new LinkedList<Link>();
            if (bean.getParentId() != null) {
                links.add(linkTo(EntityResource.class).slash(bean.getParentId()).withRel("parent"));
            }
            links.add(linkTo(EntityResource.class).slash(bean.getId()).withRel("self"));
            json.writeObjectField("links", links);
        }
    }
}
```

At this point all is left is to wire all the stuff up.

```java
ObjectMapper objectMapper = ...
objectMapper.registerModule(new JsonAdviceModule());
objectMapper.addMixInAnnotations(Entity.class, EntityMixin.class);

Entity entity = ...
String entityAsAJSON = objectMapper.writeValueAsString(entity);
```

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
