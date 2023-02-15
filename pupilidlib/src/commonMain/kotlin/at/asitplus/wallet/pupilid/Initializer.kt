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
            val polymorphicSerializersModule = SerializersModule {
                polymorphic(CredentialSubject::class) {
                    subclass(PupilIdCredential::class)
                }
            }
            LibraryInitializer.registerExtensionLibrary(
                LibraryInitializer.ExtensionLibraryInfo(
                    goalCodes = listOf(ConstantIndex.PupilId.goalCodeIssue, ConstantIndex.PupilId.goalCodeRequestProof),
                    schema = SchemaIndex.CRED_PUPIL_ID,
                    credentialScheme = ConstantIndex.PupilId,
                    serializersModule = polymorphicSerializersModule,
                )
            )
            registered = true
        }
    }

}