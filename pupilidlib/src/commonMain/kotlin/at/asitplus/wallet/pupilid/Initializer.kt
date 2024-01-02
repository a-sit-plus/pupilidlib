package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.LibraryInitializer
import at.asitplus.wallet.lib.data.CredentialSubject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object Initializer {
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
                credentialScheme = ConstantIndex.PupilId,
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
