package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.data.ConstantIndex


object ConstantIndex {

    object PupilId : ConstantIndex.CredentialScheme {
        const val VC_TYPE = "PupilId2022"
        override val schemaUri: String = SchemaIndex.CRED_PUPIL_ID
        override val vcType: String = VC_TYPE
        const val attachmentNamePicture = "picture"
        const val attachmentNameScaledPicture = "scaledPicture"
        override val isoNamespace: String = "at.a-sit.wallet.pupilid-2022"
        override val isoDocType: String = "at.a-sit.wallet.pupilid-2022.iso"
        override val claimNames: Collection<String> = listOf(
            AttributeNames.FIRST_NAME,
            AttributeNames.LAST_NAME,
            AttributeNames.DATE_OF_BIRTH,
            AttributeNames.SCHOOL_NAME,
            AttributeNames.SCHOOL_CITY,
            AttributeNames.SCHOOL_ZIP,
            AttributeNames.SCHOOL_STREET,
            AttributeNames.SCHOOL_ID,
            AttributeNames.PUPIL_CITY,
            AttributeNames.PUPIL_ZIP,
            AttributeNames.CARD_ID,
            AttributeNames.VALID_UNTIL,
            AttributeNames.PICTURE_HASH,
            AttributeNames.SCALED_PICTURE_HASH,
        )

        object AttributeNames {
            const val FIRST_NAME = "firstName"
            const val LAST_NAME = "lastName"
            const val DATE_OF_BIRTH = "dateOfBirth"
            const val SCHOOL_NAME = "schoolName"
            const val SCHOOL_CITY = "schoolCity"
            const val SCHOOL_ZIP = "schoolZip"
            const val SCHOOL_STREET = "schoolStreet"
            const val SCHOOL_ID = "schoolId"
            const val PUPIL_CITY = "pupilCity"
            const val PUPIL_ZIP = "pupilZip"
            const val CARD_ID = "cardId"
            const val VALID_UNTIL = "validUntil"
            const val PICTURE_HASH = "pictureHash"
            const val SCALED_PICTURE_HASH = "scaledPictureHash"
        }
    }

}

