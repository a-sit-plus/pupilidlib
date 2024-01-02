package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldEndWith
import kotlinx.datetime.Instant


class RevocationListLogicTest : FreeSpec() {

    init {
        val cryptoService = DefaultCryptoService()
        "Start with an empty credential store:" - {
            val store = InMemoryIssuerCredentialStore()
            val issuer = IssuerAgent.newDefaultInstance(
                clock = TestTimeSource,
                cryptoService = cryptoService,
                timePeriodProvider = SchoolyearBasedTimePeriodProvider(TestTimeSource.timePeriodStart),
                issuerCredentialStore = store,
                dataProvider = PupilCredentialDataProvider(),
            )
            issuer.compileCurrentRevocationLists().let {
                it shouldHaveSize 1
                it.first() shouldEndWith TestTimeSource.timePeriod.toString()
            }

            "Adding a credential for the current year should have no impact in revocation lists" - {
                val credential = issuer.issueCredential(
                    cryptoService.publicKey,
                    listOf(ConstantIndex.PupilId.vcType),
                    at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.PLAIN_JWT
                ).successful
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 1
                    it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                }

                "and neither should revoking it" {
                    issuer.revokeCredentials(credential.map { (it as Issuer.IssuedCredential.VcJwt).vcJws })
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 1
                        it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                    }
                }


            }

            "Adding a credential from the previous school year" - {
                val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 1)

                "should not have an impact" {
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 1
                        it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                    }
                }
                "but revoking it should add a revocation list from the past year." {
                    oldIssuer.revokeCredentials(credential)

                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 2
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    }
                }
            }


            "Adding a credential from the next school year" - {
                val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 1)

                "should not have an impact" {
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 2
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    }
                }
                "but revoking it should add a revocation list from the next year." {
                    oldIssuer.revokeCredentials(credential)

                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 3
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                    }
                }
            }


            "Adding a credential from two years in the past and future" - {
                val (oldIssuerPast, credentialPast) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 2)
                val (oldIssuerFuture, credentialFuture) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 2)

                "should not have an impact" {
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 3
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                    }
                }
                "and neither should revoking it." {
                    oldIssuerPast.revokeCredentials(credentialPast)
                    oldIssuerFuture.revokeCredentials(credentialFuture)

                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 3
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                    }
                }
            }
        }


        "Doing everything in different orders, starting empty" - {

            val store = InMemoryIssuerCredentialStore()
            val issuer = IssuerAgent.newDefaultInstance(
                clock = TestTimeSource,
                cryptoService = cryptoService,
                timePeriodProvider = SchoolyearBasedTimePeriodProvider(TestTimeSource.timePeriodStart),
                issuerCredentialStore = store,
                dataProvider = PupilCredentialDataProvider(),
            )

            "initially only the current list should be present" {
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 1
                    it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                }
            }

            "Adding a credential from the previous school year" - {
                val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 1)

                "should not have an impact" {
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 1
                        it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                    }
                }
                "but revoking it should add a revocation list from the past year." {
                    oldIssuer.revokeCredentials(credential)

                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 2
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    }
                }
            }


            "Adding a credential from the next school year" - {
                val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 1)

                "should not have an impact" {
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 2
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    }
                }
                "but revoking it should add a revocation list from the next year." {
                    oldIssuer.revokeCredentials(credential)

                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 3
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                    }
                }
            }


            "Adding a credential from two years in the past and future" - {
                val (oldIssuerPast, credentialPast) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 2)
                val (oldIssuerFuture, credentialFuture) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 2)

                "should not have an impact" {
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 3
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                    }
                }
                "and neither should revoking it." {
                    oldIssuerPast.revokeCredentials(credentialPast)
                    oldIssuerFuture.revokeCredentials(credentialFuture)

                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 3
                        it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                        it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                    }
                }
            }


            "Adding a credential for the current year should have no impact in revocation lists" - {
                val credential = issuer.issueCredential(
                    cryptoService.publicKey,
                    listOf(ConstantIndex.PupilId.vcType),
                    at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.PLAIN_JWT
                ).successful
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 3
                    it.any { it.endsWith(TestTimeSource.timePeriod.toString()) }.shouldBeTrue()
                }

                "and neither should revoking it" {
                    issuer.revokeCredentials(credential.map { (it as Issuer.IssuedCredential.VcJwt).vcJws })
                    issuer.compileCurrentRevocationLists().let {
                        it shouldHaveSize 3
                        it.any { it.endsWith(TestTimeSource.timePeriod.toString()) }.shouldBeTrue()
                    }
                }


            }

        }
    }
}

suspend fun IssuerCredentialStore.createCredentialFortimePeriod(year: Int): Pair<IssuerAgent, List<String>> {
    val cryptoService = DefaultCryptoService()
    val oldIssuer = IssuerAgent.newDefaultInstance(
        clock = FixedTimeClock(Instant.parse("${year}-10-11T00:00:00.000Z").toEpochMilliseconds()),
        cryptoService = cryptoService,
        timePeriodProvider = SchoolyearBasedTimePeriodProvider(TestTimeSource.timePeriodStart),
        issuerCredentialStore = this,
        dataProvider = PupilCredentialDataProvider(),
    )

    val credential = oldIssuer.issueCredential(
        cryptoService.publicKey,
        listOf(ConstantIndex.PupilId.vcType),
        at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.PLAIN_JWT
    ).successful.map { (it as Issuer.IssuedCredential.VcJwt).vcJws }

    return oldIssuer to credential
}
