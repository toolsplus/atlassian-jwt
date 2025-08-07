# Atlassian JWT

[![Continuous integration](https://github.com/toolsplus/atlassian-jwt/actions/workflows/ci.yml/badge.svg)](https://github.com/toolsplus/atlassian-jwt/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/toolsplus/atlassian-jwt/branch/master/graph/badge.svg)](https://codecov.io/gh/toolsplus/atlassian-jwt)
[![Maven Central](https://img.shields.io/maven-central/v/io.toolsplus/atlassian-jwt-core_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/io.toolsplus/atlassian-jwt-core_2.12)

Utilities to read, validate and generate valid Atlassian JWTs. Atlassian tokens
are identical to regular JWTs with the exception of a few custom claims, such as `qsh` claim.

## Quick start

atlassian-jwt is published to Maven Central Scala 2.13:

    libraryDependencies += "io.toolsplus" %% "atlassian-jwt" % "x.x.x"


### Read JWT

### Validate JWT

### Write JWT

## Contributing
 
Pull requests are always welcome. Please follow the [contribution guidelines](CONTRIBUTING.md).

## License

atlassian-jwt is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
