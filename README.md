# PupilIdLib

This is a Kotlin Multiplatform Library implementing features to use the PupilId
as [W3C Verifiable Credentials](https://w3c.github.io/vc-data-model/).

Previous code containing processes and network calls have been moved to PupilIdUmbrella.

Implemented features:

- `PupilIdCredential` as a VC to store all properties
- Initialization of needed classes wtih vclib (call `PupilIdInitializer.initWithVcLib()` in your code)
- Services to create a device binding and issue credentials, see `DeviceBindingService`, `PupilIdIssuingService` and `DeviceBindingPupilIdIssuingService`
- Service to create QR codes for validation of credentials, see `QrCodeContentService` and `PupilIdQrCodeService`
