# PupilIdLib

This is a Kotlin Multiplatform Library implementing features to use the PupilId in Apps and on the Backend.

Implemented features:

- Device Binding process
- PupilId Issuing process
- Combination of Device Binding and PupilId Issuing

## Changelog

v1.4.8:

- Modify `DeviceAdapter.loadAttestationCerts()` to pass `challenge` and `clientData`, which shall be included in iOS App
  Attest statements