package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.agent.DefaultCryptoService
import at.asitplus.wallet.lib.agent.FixedTimeClock
import at.asitplus.wallet.lib.agent.InMemoryIssuerCredentialStore
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.IssuerCredentialStore
import at.asitplus.testballoon.matrix.matrixSuite
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldEndWith
import kotlin.time.Instant
import kotlin.random.Random


val RevocationListLogicTest by matrixSuite {


    "Start with an empty credential store:" {
        val store = InMemoryIssuerCredentialStore()
        val issuer = IssuerAgent.newDefaultInstance(
            clock = TestTimeSource,
            cryptoService = DefaultCryptoService(),
            timePeriodProvider = SchoolyearBasedTimePeriodProvider(TestTimeSource.timePeriodStart),
            issuerCredentialStore = store,
            dataProvider = PupilCredentialDataProvider(),
        )
        issuer.compileCurrentRevocationLists().let {
            it shouldHaveSize 1
            it.first() shouldEndWith TestTimeSource.timePeriod.toString()
        }

        run { // Adding a credential for the current year should have no impact in revocation lists
            val credential = issuer.issueCredential(
                Random.nextLong().toString(),
                PupilIdConstants.Scheme.vcType
            ).successful
            issuer.compileCurrentRevocationLists().let {
                it shouldHaveSize 1
                it.first() shouldEndWith TestTimeSource.timePeriod.toString()
            }

            run { // and neither should revoking it
                issuer.revokeCredentials(credential.map { it.vcJws })
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 1
                    it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                }
            }


        }

        run { // Adding a credential from the previous school year
            val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 1)

            run { // should not have an impact
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 1
                    it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                }
            }
            run { // but revoking it should add a revocation list from the past year.
                oldIssuer.revokeCredentials(credential)

                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 2
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                }
            }
        }


        run { // Adding a credential from the next school year
            val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 1)

            run { // should not have an impact
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 2
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                }
            }
            run { // but revoking it should add a revocation list from the next year.
                oldIssuer.revokeCredentials(credential)

                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 3
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                }
            }
        }


        run { // Adding a credential from two years in the past and future
            val (oldIssuerPast, credentialPast) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 2)
            val (oldIssuerFuture, credentialFuture) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 2)

            run { // should not have an impact
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 3
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                }
            }
            run { // and neither should revoking it.
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


    "Doing everything in different orders, starting empty" {

        val store = InMemoryIssuerCredentialStore()
        val issuer = IssuerAgent.newDefaultInstance(
            clock = TestTimeSource,
            cryptoService = DefaultCryptoService(),
            timePeriodProvider = SchoolyearBasedTimePeriodProvider(TestTimeSource.timePeriodStart),
            issuerCredentialStore = store,
            dataProvider = PupilCredentialDataProvider(),
        )

        run { // initially only the current list should be present
            issuer.compileCurrentRevocationLists().let {
                it shouldHaveSize 1
                it.first() shouldEndWith TestTimeSource.timePeriod.toString()
            }
        }

        run { // Adding a credential from the previous school year
            val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 1)

            run { // should not have an impact
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 1
                    it.first() shouldEndWith TestTimeSource.timePeriod.toString()
                }
            }
            run { // but revoking it should add a revocation list from the past year.
                oldIssuer.revokeCredentials(credential)

                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 2
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                }
            }
        }


        run { // Adding a credential from the next school year
            val (oldIssuer, credential) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 1)

            run { // should not have an impact
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 2
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                }
            }
            run { // but revoking it should add a revocation list from the next year.
                oldIssuer.revokeCredentials(credential)

                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 3
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                }
            }
        }


        run { // Adding a credential from two years in the past and future
            val (oldIssuerPast, credentialPast) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod - 2)
            val (oldIssuerFuture, credentialFuture) = store.createCredentialFortimePeriod(TestTimeSource.timePeriod + 2)

            run { // should not have an impact
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 3
                    it.filter { it.endsWith(("/2020")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2021")) }.shouldNotBeEmpty()
                    it.filter { it.endsWith(("/2022")) }.shouldNotBeEmpty()
                }
            }
            run { // and neither should revoking it.
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


        run { // Adding a credential for the current year should have no impact in revocation lists
            val credential = issuer.issueCredential(
                Random.nextLong().toString(),
                PupilIdConstants.Scheme.vcType
            ).successful
            issuer.compileCurrentRevocationLists().let {
                it shouldHaveSize 3
                it.any { it.endsWith(TestTimeSource.timePeriod.toString()) }.shouldBeTrue()
            }

            run { // and neither should revoking it
                issuer.revokeCredentials(credential.map { it.vcJws })
                issuer.compileCurrentRevocationLists().let {
                    it shouldHaveSize 3
                    it.any { it.endsWith(TestTimeSource.timePeriod.toString()) }.shouldBeTrue()
                }
            }


        }

    }
}

suspend fun IssuerCredentialStore.createCredentialFortimePeriod(year: Int): Pair<IssuerAgent, List<String>> {
    val oldIssuer = IssuerAgent.newDefaultInstance(
        clock = FixedTimeClock(Instant.parse("${year}-10-11T00:00:00.000Z").toEpochMilliseconds()),
        cryptoService = DefaultCryptoService(),
        timePeriodProvider = SchoolyearBasedTimePeriodProvider(TestTimeSource.timePeriodStart),
        issuerCredentialStore = this,
        dataProvider = PupilCredentialDataProvider(),
    )

    val credential = oldIssuer.issueCredential(
        Random.nextLong().toString(),
        PupilIdConstants.Scheme.vcType
    ).successful.map { it.vcJws }

    return oldIssuer to credential
}
