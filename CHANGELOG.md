# Changelog

Release NEXT:
 - depend on locally built vclib 3
 - use kmp-crypto instead of home-grown Asn1 implementations

Release 1.8.0:
 - Move network processes to Umbrella lib
 - Implement VC for PupilIds, as extension to vclib
 - Upgrade to vclib 1.7.2

Release 1.8.0:
 - Transport expected size of payloads in `InputInsuffientError`
 - Dependencies: Kotlin 1.8.0, Resultlib 1.4.0

Release 1.7.0:
 - Refactor passing HTTP client engine to various network services, introducing class `HttpClientBuilder`
 - Clients may also specify a user agent header string to be included in all requests, see parameter `userAgent` in `HttpClientBuilder`
 - Clients may enable logging of all HTTP requests, see parameter `enableLogging` in `HttpClientBuilder`

Release 1.7.0:
 - Refactor passing HTTP client engine to various network services, introducing class `HttpClientBuilder`
 - Clients may also specify a user agent header string to be included in all requests, see parameter `userAgent` in `HttpClientBuilder`
 - Clients may enable logging of all HTTP requests, see parameter `enableLogging` in `HttpClientBuilder`

Release 1.6.1:
 - Remove `QrCodeContentService.serializedContent()`, Clients may use `CredentialContent.serialize()` instead
 - Remove `QrCodeContentService.deserializedContent()`, Clients may use `CredentialContent.companion.deserialize()` instead
 - Return `InputIncorrectError` on `QrCodeContentService.decodeQrCode()` when any input does not start with the expected prefix
 - Implement encoding and decoding list of QR Codes with a second prefix, see `QrCodeContentService`
 - Add function to analyze scanned QR Codes, see `QrCodeContentService.analyzeQrCode`

Release 1.6.0:
 - Upgrade to Gradle 7.6, Kotlin 1.8.0, Resultlib 1.4.0

Release 1.5.0:
 - Implement extended creation and validation of QR Codes for PupilIds and pictures, see `QrCodeContentService`
 - Rename `endOfSchoolYear` in `CredentialContent` to `expiration`

Release 1.4.8:
 - Use library `KmmResult` in version 1.1
 - Modify `DeviceAdapter.loadAttestationCerts()` to pass `challenge` and `clientData`, which shall be included in iOS App Attest statements

Release 1.4.5:
 - Fix iOS target name to be `PupilIdKMM`

Release 1.4.4:
 - Skipped due to Swift Package Manager problems

Release 1.4.3:
 - Configurable network timeouts
