# NOTICE

## JavaQuest100

- **Concept, idea and project direction:** Emadol
- **Implementation:** developed with the assistance of Claude, an AI model by Anthropic, under the direction of Emadol.

The source code, course content (`data-src/`, `src/main/resources/data/`), graphics and documentation in this
repository are released under the [MIT License](LICENSE).

"Java" and "OpenJDK" are trademarks or registered trademarks of Oracle and/or its affiliates.
JavaQuest100 is an independent educational project and is not affiliated with, endorsed by or sponsored by Oracle
or Anthropic.

## Third-party components (binary release only)

The source code in this repository has **no third-party library dependencies**.
The Windows executable published in the Releases section, however, bundles a Java runtime image that contains:

| Component | Source | License |
|---|---|---|
| OpenJDK 25 runtime (Eclipse Temurin build) | https://adoptium.net | GNU General Public License v2 with the Classpath Exception |
| OpenJFX 25 (JavaFX) | https://openjfx.io | GNU General Public License v2 with the Classpath Exception |

The Classpath Exception allows these components to be linked with and distributed alongside independent code
(such as JavaQuest100) without that code becoming subject to the GPL. The complete license texts of each bundled
module are included inside the runtime, in the `runtime/legal/` folder extracted by the executable
(`%LOCALAPPDATA%\JavaQuest100_app\...\JavaQuest100\runtime\legal\`). The corresponding source code is available
from the projects linked above.
