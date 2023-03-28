package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.LibraryInitializer
import at.asitplus.wallet.lib.data.CredentialSubject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class Initializer {

    companion object {

        /**
         * A reference to this class is enough to trigger the init block
         */
        init {
            initWithVcLib()
        }

        private var registered = false

        /**
         * This has to be called first, before anything first, to load the
         * relevant classes of this library into the base implementations of vclib
         */
        fun initWithVcLib() {
            if (registered) return
            LibraryInitializer.registerExtensionLibrary(
                LibraryInitializer.ExtensionLibraryInfo(
                    credentialScheme = object : at.asitplus.wallet.lib.data.ConstantIndex.CredentialScheme {
                        override val goalCodeIssue: String = "issue-vc-pupilid"
                        override val goalCodeRequestProof: String = "request-proof-pupilid"
                        override val credentialDefinitionName: String = "pupilid"
                        override val schemaUri: String = SchemaIndex.CRED_PUPIL_ID
                        override val vcType: String = "PupilId2022"
                    },
                    serializersModule = SerializersModule {
                        polymorphic(CredentialSubject::class) {
                            subclass(PupilIdCredential::class)
                        }
                    },
                )
            )
            registered = true
        }
    }

}