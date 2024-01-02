# PupilIdLib

This is a Kotlin Multiplatform library implementing features of the Austrian electronic pupil ID ([edu.digicard](https://www.bmbwf.gv.at/Themen/schule/zrp/dibi/itinf/itdienstleistungen/educard.html))
as [W3C Verifiable Credentials](https://w3c.github.io/vc-data-model/).
It targets iOS and Android on the client side, as well as the JVM on the back-end.
It also contains JVM-only data classes to be consumed by an OpenAPI generator (i.e. Swagger)

The business logic is written in 100% pure Kotlin and relies heavily on the [KMM VC library](https://github.com/a-sit-plus/kmm-vc-library).

Implemented features:

- `PupilIdCredential` as a W3C VC to store all properties
- Initialization of needed classes wtih vclib (call `Initializer.initWithVcLib()` in your code)
- Creation of a device Binding (i.e. certified holder key for the wallet)
- QR code encoding and parsing of credentials, including splitting of large credentials into chunks of data


Some bits and pieces are rather enterprise-y and read very much like traditional Java code.
The plain and simple reason for this is the fact that the code is used in production, which is why breaking API and ABI changes are costly, hence the separation between KMM VC library and PupilIdLib.

Check the testcases for examples on how to use it.
