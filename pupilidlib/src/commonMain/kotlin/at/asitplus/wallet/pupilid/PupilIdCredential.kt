package at.asitplus.wallet.pupilid

import at.asitplus.wallet.lib.data.CredentialSubject
import at.asitplus.wallet.pupilid.ConstantIndex.PupilId.AttributeNames
import at.asitplus.wallet.pupilid.ConstantIndex.PupilId.VC_TYPE
import at.asitplus.wallet.utils.ByteArrayBase64Serializer
import io.matthewnelson.component.base64.encodeBase64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VC spec leaves the representation of a single credential open to implementations.
 * We decided to encode all attributes of a pupil ID into one VC.
 */
@Serializable
@SerialName(VC_TYPE)
class PupilIdCredential : CredentialSubject {

    override val id: String

    /**
     * "Vorname"
     */
    @SerialName(AttributeNames.FIRST_NAME)
    val firstName: String

    /**
     * "Familienname"
     */
    @SerialName(AttributeNames.LAST_NAME)
    val lastName: String

    /**
     * "Geburtsdatum"
     */
    @SerialName(AttributeNames.DATE_OF_BIRTH)
    val dateOfBirth: String

    /**
     * "Schulname"
     */
    @SerialName(AttributeNames.SCHOOL_NAME)
    val schoolName: String

    /**
     * "Schuladresse Stadt"
     */
    @SerialName(AttributeNames.SCHOOL_CITY)
    val schoolCity: String

    /**
     * "Schuladresse Postleitzahl"
     */
    @SerialName(AttributeNames.SCHOOL_ZIP)
    val schoolZip: String

    /**
     * "Schuladresse Straße"
     */
    @SerialName(AttributeNames.SCHOOL_STREET)
    val schoolStreet: String

    /**
     * "Schulkennzahl"
     */
    @SerialName(AttributeNames.SCHOOL_ID)
    val schoolId: String

    /**
     * "Wohnort"
     */
    @SerialName(AttributeNames.PUPIL_CITY)
    val pupilCity: String?

    /**
     * "Wohnort-Postleitzahl"
     */
    @SerialName(AttributeNames.PUPIL_ZIP)
    val pupilZip: String?

    /**
     * "Kartennummer"
     */
    @SerialName(AttributeNames.CARD_ID)
    val cardId: String

    /**
     * "Gültig bis"
     */
    @SerialName(AttributeNames.VALID_UNTIL)
    val validUntil: String

    /**
     * Hash of "Foto"
     */
    @SerialName(AttributeNames.PICTURE_HASH)
    @Serializable(with = ByteArrayBase64Serializer::class)
    val pictureHash: ByteArray

    /**
     * Hash of scaled-down "Foto"
     */
    @SerialName(AttributeNames.SCALED_PICTURE_HASH)
    @Serializable(with = ByteArrayBase64Serializer::class)
    val scaledPictureHash: ByteArray

    constructor(
        id: String,
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        schoolName: String,
        schoolCity: String,
        schoolZip: String,
        schoolStreet: String,
        schoolId: String,
        pupilCity: String?,
        pupilZip: String?,
        cardId: String,
        validUntil: String,
        pictureHash: ByteArray,
        scaledPictureHash: ByteArray,
    ) : super() {
        this.id=id
        this.firstName = firstName
        this.lastName = lastName
        this.dateOfBirth = dateOfBirth
        this.schoolName = schoolName
        this.schoolCity = schoolCity
        this.schoolZip = schoolZip
        this.schoolStreet = schoolStreet
        this.schoolId = schoolId
        this.pupilCity = pupilCity
        this.pupilZip = pupilZip
        this.cardId = cardId
        this.validUntil = validUntil
        this.pictureHash = pictureHash
        this.scaledPictureHash = scaledPictureHash
    }

    override fun toString(): String {
        return "PupilIdCredential(firstName='$firstName'," +
                " lastName='$lastName'," +
                " dateOfBirth='$dateOfBirth'," +
                " schoolName='$schoolName'," +
                " schoolCity='$schoolCity'," +
                " schoolZip='$schoolZip'," +
                " schoolStreet='$schoolStreet'," +
                " schoolId='$schoolId'," +
                " pupilCity='$pupilCity'," +
                " pupilZip='$pupilZip'," +
                " cardId='$cardId'," +
                " validUntil='$validUntil'," +
                " pictureHash='${pictureHash.encodeBase64()}'" +
                " scaledPictureHash='${scaledPictureHash.encodeBase64()}'" +
                ")"
    }

}
