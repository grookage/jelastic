# JElastic [![Build](https://github.com/grookage/jelastic/actions/workflows/build.yml/badge.svg)](https://github.com/grookage/jelastic/actions/workflows/build.yml)

> Your mind is like a tunnel that has no end, and a baloon, that even too much air cannot burst.
> - by Michael Bassey Johnson

> So you can be 'fantastic' or 'elastic' but can you ever be both lol??
> - by Amber J.

JElastic is for using elasticsearch within application paradigm without setting your hair on fire about the internal client details. And oh, it is a dropwizard bundle! 

### Build instructions
  - Clone the source:

        git clone github.com/grookage/jelastic

  - Build

        mvn install

### Using JElastic bundle

#### Bootstrap
```java
     JElasticBundle jBundle = new JElasticBundle() {
            
            public JElasticConfiguration getElasticConfiguration(T configuration) {
                ...
            }
        }
            
    @Override
    public void initialize(final Bootstrap...) {
        bootstrap.addBundle(jBundle);
    }
```

#### Search Request
```java
      SearchRequest searchRequest = SearchRequest.builder()
                                        .index("testIndex")
                                        .query(Query.builder()
                                               .filters(Sets.newHashSet())
                                               .sorters(Sets.newHashSet())
                                               .pageWindow(
                                                   PageWindow.builder()
                                                       .pageNumber(pageNumber)
                                                       .pageSize(pageSize)
                                                       .build()
                                               )
                                               .build()
                                        )
                                        .klass(Example.class)
                                        .build();
      
      List<T> sources = jBundle.getRepository().search(searchRequest)
```

### Maven Dependency

Use the following maven dependency
```xml
<dependency>
    <groupId>com.grookage.jelastic</groupId>
    <artifactId>jelastic</artifactId>
    <version>7.3.0</version>
</dependency>
```

### Version support
| jelastic               |  es client|
| -----------------------| ------------------- |
| 7.2.1-2                |  7.2.1 (rest high level client)              |
| 7.3.0                  |  7.3.0 (rest high level client)              |

### Configuration
```yaml
jelastic:
  clusterName: elasticsearch
  servers:
    - localhost:9200
  failOnYellow: false
  settingsFile: ~/configs/settings.xml
  maxResultSize: 10000    
```

LICENSE
-------

Copyright 2019 Koushik R <rkoushik.14@gmail.com>.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
