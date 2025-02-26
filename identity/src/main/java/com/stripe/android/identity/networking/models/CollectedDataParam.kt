package com.stripe.android.identity.networking.models

import com.stripe.android.core.networking.toMap
import com.stripe.android.identity.ml.IDDetectorAnalyzer
import com.stripe.android.identity.navigation.DocSelectionFragment
import com.stripe.android.identity.networking.UploadedResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CollectedDataParam(
    @SerialName("biometric_consent")
    val biometricConsent: Boolean = true,
    @SerialName("id_document_type")
    val idDocumentType: Type? = null,
    @SerialName("id_document_front")
    val idDocumentFront: DocumentUploadParam? = null,
    @SerialName("id_document_back")
    val idDocumentBack: DocumentUploadParam? = null,
    @SerialName("face")
    val face: FaceUploadParam? = null
) {
    @Serializable
    internal enum class Type {
        @SerialName("driving_license")
        DRIVINGLICENSE,

        @SerialName("id_card")
        IDCARD,

        @SerialName("passport")
        PASSPORT;

        companion object {
            fun fromName(typeName: String) =
                when (typeName) {
                    DocSelectionFragment.PASSPORT_KEY -> PASSPORT
                    DocSelectionFragment.DRIVING_LICENSE_KEY -> DRIVINGLICENSE
                    DocSelectionFragment.ID_CARD_KEY -> IDCARD
                    else -> {
                        throw IllegalArgumentException("Unknown name $typeName")
                    }
                }
        }
    }

    internal companion object {
        private const val COLLECTED_DATA_PARAM = "collected_data"

        /**
         * Create map entry for encoding into x-www-url-encoded string.
         */
        fun CollectedDataParam.createCollectedDataParamEntry(json: Json) =
            COLLECTED_DATA_PARAM to json.encodeToJsonElement(
                serializer(),
                this
            ).toMap()

        fun createFromFrontUploadedResultsForAutoCapture(
            type: Type,
            frontHighResResult: UploadedResult,
            frontLowResResult: UploadedResult
        ): CollectedDataParam =
            CollectedDataParam(
                idDocumentFront = DocumentUploadParam(
                    backScore = requireNotNull(frontHighResResult.scores)[IDDetectorAnalyzer.INDEX_ID_BACK],
                    frontCardScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_ID_FRONT],
                    invalidScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_INVALID],
                    passportScore = frontHighResResult.scores[IDDetectorAnalyzer.INDEX_PASSPORT],
                    highResImage = requireNotNull(
                        frontHighResResult.uploadedStripeFile.id
                    ) {
                        "front high res image id is null"
                    },
                    lowResImage = requireNotNull(
                        frontLowResResult.uploadedStripeFile.id
                    ) {
                        "front low res image id is null"
                    },
                    uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
                ),
                idDocumentType = type
            )

        fun createFromBackUploadedResultsForAutoCapture(
            type: Type,
            backHighResResult: UploadedResult,
            backLowResResult: UploadedResult
        ): CollectedDataParam =
            CollectedDataParam(
                idDocumentBack = DocumentUploadParam(
                    backScore = requireNotNull(backHighResResult.scores)[IDDetectorAnalyzer.INDEX_ID_BACK],
                    frontCardScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_ID_FRONT],
                    invalidScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_INVALID],
                    passportScore = backHighResResult.scores[IDDetectorAnalyzer.INDEX_PASSPORT],
                    highResImage = requireNotNull(
                        backHighResResult.uploadedStripeFile.id
                    ) {
                        "back high res image id is null"
                    },
                    lowResImage = requireNotNull(
                        backLowResResult.uploadedStripeFile.id
                    ) {
                        "back low res image id is null"
                    },
                    uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
                ),
                idDocumentType = type
            )

        fun createForSelfie(
            firstHighResResult: UploadedResult,
            firstLowResResult: UploadedResult,
            lastHighResResult: UploadedResult,
            lastLowResResult: UploadedResult,
            bestHighResResult: UploadedResult,
            bestLowResResult: UploadedResult,
            trainingConsent: Boolean,
            bestFaceScore: Float,
            faceScoreVariance: Float,
            numFrames: Int
        ) = CollectedDataParam(
            face = FaceUploadParam(
                bestHighResImage = requireNotNull(bestHighResResult.uploadedStripeFile.id),
                bestLowResImage = requireNotNull(bestLowResResult.uploadedStripeFile.id),
                firstHighResImage = requireNotNull(firstHighResResult.uploadedStripeFile.id),
                firstLowResImage = requireNotNull(firstLowResResult.uploadedStripeFile.id),
                lastHighResImage = requireNotNull(lastHighResResult.uploadedStripeFile.id),
                lastLowResImage = requireNotNull(lastLowResResult.uploadedStripeFile.id),
                bestFaceScore = bestFaceScore,
                faceScoreVariance = faceScoreVariance,
                numFrames = numFrames,
                trainingConsent = trainingConsent
            )
        )
    }
}
