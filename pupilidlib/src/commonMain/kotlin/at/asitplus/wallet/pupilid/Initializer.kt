package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.LibraryInitializer
import at.asitplus.wallet.lib.data.CredentialSubject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object Initializer {

    /**
     * This has to be called first, before anything first, to load the
     * relevant classes of this library into the base implementations of vclib
     */
    fun initWithVcLib() {
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
    }

}