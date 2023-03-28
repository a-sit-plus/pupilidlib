package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.data.ConstantIndex

object ConstantIndex {

    object PupilId : ConstantIndex.CredentialScheme {
        override val goalCodeIssue: String = "issue-vc-pupilid"
        override val goalCodeRequestProof: String = "request-proof-pupilid"
        override val credentialDefinitionName: String = "pupilid"
        override val schemaUri: String = SchemaIndex.CRED_PUPIL_ID
        override val vcType: String = "PupilId2022"
        const val attachmentNamePicture = "picture"
        const val attachmentNameScaledPicture = "scaledPicture"
    }

}

