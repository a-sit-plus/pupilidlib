package at.asitplus.wallet.pupilid


import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "BindingParamsRequest", description = "Request to get parameters for a new binding")
class BindingParamsRequestJ(
    @Schema(description = "Name of the mobile device", example = "Pixel 3", nullable = false)
    deviceName: String,
) : BindingParamsRequest(deviceName)

@Schema(name = "BindingParamsResponse", description = "Response containing parameters for a new binding")
class BindingParamsResponseJ(
    @Schema(
        description = "Random challenge to be included in the next request",
        example = "6j2a9M7P1J9bOUuGe5Tpto7Ylz+2DtbH54jdHh2YO/Y=",
        nullable = false,
    )
    challenge: ByteArray,
    @Schema(
        description = "Subject to be set in the CSR of the client",
        example = "CN=0iar5aae,O=wallet.a-sit.at",
        nullable = false,
    )
    subject: String,
    @Schema(
        description = "Key type to create on the client",
        example = "EC",
        nullable = false,
    )
    keyType: String,
) : BindingParamsResponse(challenge, subject, keyType)

@Schema(name = "BindingCsrRequest", description = "Request to sign a public key")
class BindingCsrRequestJ(
    @Schema(
        description = "Challenge from previous response",
        example = "6j2a9M7P1J9bOUuGe5Tpto7Ylz+2DtbH54jdHh2YO/Y=",
        nullable = false,
    )
    challenge: ByteArray,
    @Schema(
        description = "Certification Signing Request in PKCS#10 format",
        example = "MIHNMHQCAQAwEjEQMA4GA1UEAwwHU3ViamVjdDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABEgRPVMG" +
                "MgkAilfugC/3mncR8mot9gsC4/bJmlW0ugpxRMiIgi3srUmIlCMgTN9hMPGEAXdPd0Hvize9o9vuezag" +
                "ADAKBggqhkjOPQQDAgNJADBGAiEA2l1XvS1c1j/f6SN0AwTdJZNvTwnZP3tRQyNpzQMZMnMCIQDepERQ" +
                "mECr3mqFGS4AQzSnWpwZZBjGtmU1NWiK/E92Ew==",
        nullable = false,
    )
    csr: ByteArray,
    @Schema(description = "Name of the mobile device", example = "Pixel 3", nullable = false)
    deviceName: String,
    @Schema(
        description = "The Key Attestation (Android) or Device Attestation (iOS) structure of the client device",
        example = "[MIICpjCCAkqgAwIBAgIBATAMBggqhkjOPQQDAgUAMD8xEjAQBgNVBAwMCVN0cm9uZ0JveDEpMCcGA1UE" +
                "BRMgMDY4NDJmODRiY2JhZGJkMTk2NDA1YmZkNmE2MzQ5ZWIwHhcNNzAwMTAxMDAwMDAwWhcNNDgwMTAx" +
                "MDAwMDAwWjAfMR0wGwYDVQQDExRBbmRyb2lkIEtleXN0b3JlIEtleTBZMBMGByqGSM49AgEGCCqGSM49" +
                "AwEHA0IABD1auUFhE6prEafZ90OHrq6CPZS6+hTJ3HLmeqOw2OCytf0NaCLLz6DMLe1GV3EWxCDGi1UH" +
                "e10UO5zwx/2OyFCjggFTMIIBTzAOBgNVHQ8BAf8EBAMCB4AwggE7BgorBgEEAdZ5AgERBIIBKzCCAScC" +
                "AWQKAQICAWQKAQIEJDQ1Y2ZiYWRhLWE5NTItNGVhNS05M2JjLWYyZWQzNjVlOGRiOAQAMEy/hUVIBEYw" +
                "RDEeMBwEFmF0LmFzaXRwbHVzLmJpb21ldHJpY3MCAgFAMSIEIEFfrT4RcXh0HaTOlPpeZXwPjA8Z06Nw" +
                "7B6ZSBe/nLXrMIGioQUxAwIBAqIDAgEDowQCAgEApQUxAwIBBKoDAgEBv4N4AwIBAr+FPgMCAQC/hUBM" +
                "MEoEIA9udcgBg7XewHSwBU1CcemTievksTawgZ3h8VC6D/nXAQH/CgEABCBmOJbI61T3+Ji7mfx/sIEd" +
                "md7/o4Vwizd3ttcqU2kaH7+FQQUCAwHUwL+FQgUCAwMVf7+FTgYCBAE0ZaG/hU8GAgQBNGWcMAwGCCqG" +
                "SM49BAMCBQADSAAwRQIgae9OOc3NwhakcZCAeA9IXRWyBauT47ADg9Dy9EtasnMCIQDH/fwrI3O45Oqo" +
                "6OQdBpqNGI77GprvrXoKs6kqldIjmA==]",
    )
    attestationCerts: List<ByteArray>,
) : BindingCsrRequest(challenge, csr, deviceName, attestationCerts)

@Schema(name = "BindingCsrResponse", description = "Response to the CSR, containing the binding certificate")
class BindingCsrResponseJ(
    @Schema(
        description = "The signed binding certificate, to be stored on the mobile device",
        example = "MIIBFzCBvaADAgECAgjWVAvsBy5UXDAKBggqhkjOPQQDAjASMRAwDgYDVQQDDAdTdWJqZWN0MB4XDTIy" +
                "MDIyMjE1MzM0NVoXDTIyMDIyMjE1MzQ0NVowETEPMA0GA1UEAwwGSXNzdWVyMFkwEwYHKoZIzj0CAQYI" +
                "KoZIzj0DAQcDQgAEPnNczNYC/8QwBXZrKqBDdSwvzHQQKOi8UWpsy+33uW2zJorQXgAljj0qxCmVlgPs" +
                "5FAoF7zzQbM/4pF1DfK+6jAKBggqhkjOPQQDAgNJADBGAiEAs9sOHPs3vuHP5zbaTUTxC2j4a/afLfW1" +
                "GlMJdHGwsToCIQCiAbOdx7Bth+T7MjQhv9hsYo0zDzuMBvxYKF+pbNtJdg==",
        nullable = false,
    )
    certificate: ByteArray,
    @Schema(
        description = "The signed public key as an JWS, if the attestation from the client was correct, otherwise `null`",
        example = "eyJhbGciOiJFUzI1NiJ9.eyJwayI6IkJFWHlSS3JVdWh6RHluV1N3YTJEcytUanNzaEVQRDBOZEFGUDB" +
                "HVVlha2krQUZoTUxxT0hYUnN3MUgreFFNM2JmYXRoTlhJY3hicWg3N1dPaVJUMHFZTT0ifQ.OBdGISyF" +
                "Nba1YpPEMj8Su-wWgSKDEBuFNAUHAggugQ1bbT01cjuLxphmiGnHYuXXi86wSg_JkCOcgV-acUrysQ",
        nullable = true,
    )
    attestedPublicKey: String?,
) : BindingCsrResponse(certificate, attestedPublicKey)

@Schema(name = "BindingConfirmRequest", description = "Request to confirm the binding")
class BindingConfirmRequestJ(
    success: Boolean
) : BindingConfirmRequest(success)

@Schema(name = "BindingConfirmResponse", description = "Response to confirmation")
class BindingConfirmResponseJ(
    success: Boolean
) : BindingConfirmResponse(success)

