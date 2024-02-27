# PupilIdLib

This is a Kotlin Multiplatform Library implementing features to use the PupilId in Apps and on the Backend.

## Note on Composite KMM Builds
This is borked on the curren stable version of the KMP gradle plugin and might not even work correctly once 1.8.20 is release.
Hence, `./gradlew init` builds the included `vclib` and publishes it to a local maven repo which is included in this project.
Makes the build work without relying on composite builds, but does not support refactoring across modules.


## Publishing

The CI pipeline runs a `publish` job on every commit for `master` and `development` to publish the library artifacts, see [.gitlab-ci.yml](.gitlab-ci.yml).

To publish the Swift package, the archive of the XCFramework is uploaded to the Package Registry of this project, and the link is set to the `Package.swift` of the [Swift Package repository](https://gitlab.iaik.tugraz.at/wallet/swift-package-pupilid-kmm-lib). Therefore, a `SWIFT_REPO_ACCESS_TOKEN` is configured in [this project](https://gitlab.iaik.tugraz.at/wallet/pupilid-kmm-lib/-/settings/ci_cd) with tha value from a [project access token](https://gitlab.iaik.tugraz.at/wallet/swift-package-pupilid-kmm-lib/-/settings/access_tokens) of the Swift Package repo.

These artifacts are stored in the [GitLab Package Registry](https://gitlab.iaik.tugraz.at/wallet/pupilid-kmm-lib/-/packages) for this project.

### Gradle Client

Clients using Gradle need to configure a repository in their projects:

```gradle
repositories {
	mavenLocal()
	if (System.getenv("CI_JOB_TOKEN") != null || project.hasProperty("gitLabPrivateToken")) {
		maven {
			name = "gitlabProject"
			url = "https://gitlab.iaik.tugraz.at/api/v4/groups/159/-/packages/maven"
			if (project.hasProperty("gitLabPrivateToken")) {
				credentials(HttpHeaderCredentials) {
					name = 'Private-Token'
					value = gitLabPrivateToken
				}
			} else if (System.getenv("CI_JOB_TOKEN") != null) {
				credentials(HttpHeaderCredentials) {
					name = 'Job-Token'
					value = System.getenv("CI_JOB_TOKEN")
				}
			}
			authentication {
				header(HttpHeaderAuthentication)
			}
		}
	}
	mavenCentral()
	google()
}
```

The value for `gitLabPrivateToken` shall be stored in a local `~/.gradle/gradle.properties` as `gitLabPrivateToken=YOUR_TOKEN_VALUE`. Create a new [Personal Access Token](https://gitlab.iaik.tugraz.at/-/profile/personal_access_tokens) with the scope of `api` for that.

### Swift Client

Clients using Swift Package Manager are referred to the repository containing a [Swift Package](https://gitlab.iaik.tugraz.at/wallet/swift-package-pupilid-kmm-lib/) for this library.

On your local machine (where you are importing this package into an App), you'll need to configure credentials to be able to access the URL of the archive defined in the Swift Package.

Create a new [personal access token](https://gitlab.iaik.tugraz.at/-/profile/personal_access_tokens) with the scope `read_api` and add it to the local `~/.netrc` file:

```
machine gitlab.iaik.tugraz.at
login doesntmatter
password VALUEOFYOURTOKEN
```




