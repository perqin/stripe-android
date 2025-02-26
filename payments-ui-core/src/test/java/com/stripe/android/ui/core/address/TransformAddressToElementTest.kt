package com.stripe.android.ui.core.address

import androidx.compose.ui.text.input.KeyboardCapitalization
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressRepository.Companion.supportedCountries
import com.stripe.android.ui.core.elements.AdministrativeAreaConfig
import com.stripe.android.ui.core.elements.AdministrativeAreaElement
import com.stripe.android.ui.core.elements.Capitalization
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KeyboardType
import com.stripe.android.ui.core.elements.RowElement
import com.stripe.android.ui.core.elements.SectionSingleFieldElement
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.TextFieldController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.security.InvalidParameterException

class TransformAddressToElementTest {

    @Test
    fun `Read US Json`() = runBlocking {
        val addressSchema = readFile("src/main/assets/addressinfo/US.json")!!
        val simpleTextList = addressSchema.transformToElementList("US")

        val addressLine1 = SimpleTextSpec(
            IdentifierSpec.Line1,
            R.string.address_label_address_line1,
            Capitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val addressLine2 = SimpleTextSpec(
            IdentifierSpec.Line2,
            R.string.address_label_address_line2,
            Capitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = true
        )

        val city = SimpleTextSpec(
            IdentifierSpec.City,
            R.string.address_label_city,
            Capitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val zip = SimpleTextSpec(
            IdentifierSpec.PostalCode,
            R.string.address_label_zip_code,
            Capitalization.None,
            KeyboardType.NumberPassword,
            showOptionalLabel = false
        )

        assertThat(simpleTextList.size).isEqualTo(4)
        verifySimpleTextSpecInTextFieldController(
            simpleTextList[0] as SectionSingleFieldElement,
            addressLine1
        )
        verifySimpleTextSpecInTextFieldController(
            simpleTextList[1] as SectionSingleFieldElement,
            addressLine2
        )
        val cityZipRow = simpleTextList[2] as RowElement
        verifySimpleTextSpecInTextFieldController(
            cityZipRow.fields[0],
            city
        )
        verifySimpleTextSpecInTextFieldController(
            cityZipRow.fields[1],
            zip
        )

        // US has state dropdown
        val stateDropdownElement = simpleTextList[3] as AdministrativeAreaElement
        val stateDropdownController = stateDropdownElement.controller
        assertThat(stateDropdownController.displayItems).isEqualTo(
            AdministrativeAreaConfig.Country.US().administrativeAreas.map { it.second }
        )
        assertThat(stateDropdownController.label.first()).isEqualTo(
            R.string.address_label_state
        )
    }

    private suspend fun verifySimpleTextSpecInTextFieldController(
        textElement: SectionSingleFieldElement,
        simpleTextSpec: SimpleTextSpec
    ) {
        val actualController = textElement.controller as TextFieldController
        assertThat(actualController.capitalization).isEqualTo(
            when (simpleTextSpec.capitalization) {
                Capitalization.None -> KeyboardCapitalization.None
                Capitalization.Characters -> KeyboardCapitalization.Characters
                Capitalization.Words -> KeyboardCapitalization.Words
                Capitalization.Sentences -> KeyboardCapitalization.Sentences
            }
        )
        assertThat(actualController.keyboardType).isEqualTo(
            when (simpleTextSpec.keyboardType) {
                KeyboardType.Text -> androidx.compose.ui.text.input.KeyboardType.Text
                KeyboardType.Ascii -> androidx.compose.ui.text.input.KeyboardType.Ascii
                KeyboardType.Number -> androidx.compose.ui.text.input.KeyboardType.Number
                KeyboardType.Phone -> androidx.compose.ui.text.input.KeyboardType.Phone
                KeyboardType.Uri -> androidx.compose.ui.text.input.KeyboardType.Uri
                KeyboardType.Email -> androidx.compose.ui.text.input.KeyboardType.Email
                KeyboardType.Password -> androidx.compose.ui.text.input.KeyboardType.Password
                KeyboardType.NumberPassword -> androidx.compose.ui.text.input.KeyboardType.NumberPassword
            }
        )
        assertThat(actualController.label.first()).isEqualTo(
            simpleTextSpec.label
        )
    }

    @Test
    fun `Make sure name schema is not found on fields not processed`() {
        supportedCountries.forEach { countryCode ->
            val schemaList = readFile("src/main/assets/addressinfo/$countryCode.json")
            val invalidNameType = schemaList?.filter { addressSchema ->
                addressSchema.schema?.nameType != null
            }
                ?.filter {
                    it.type == FieldType.AddressLine1 &&
                        it.type == FieldType.AddressLine2 &&
                        it.type == FieldType.Locality
                }
            invalidNameType?.forEach { println(it.type?.name) }
            assertThat(invalidNameType).isEmpty()
        }
    }

    @Test
    fun `Make sure sorting code and dependent locality is never required`() {
        // Sorting code and dependent locality are not actually sent to the server.
        supportedCountries.forEach { countryCode ->
            val schemaList = readFile("src/main/assets/addressinfo/$countryCode.json")
            val invalidNameType = schemaList?.filter { addressSchema ->
                addressSchema.required &&
                    (
                        addressSchema.type == FieldType.SortingCode ||
                            addressSchema.type == FieldType.DependentLocality
                        )
            }
            invalidNameType?.forEach { println(it.type?.name) }
            assertThat(invalidNameType).isEmpty()
        }
    }

    @Test
    fun `Make sure all country code json files are serializable`() {
        supportedCountries.forEach { countryCode ->
            val schemaList = readFile("src/main/assets/addressinfo/$countryCode.json")
            schemaList?.filter { addressSchema ->
                addressSchema.schema?.nameType != null
            }
                ?.filter {
                    it.type == FieldType.AddressLine1 &&
                        it.type == FieldType.AddressLine2 &&
                        it.type == FieldType.Locality
                }
                ?.forEach { println(it.type?.name) }
        }
    }

    private fun readFile(filename: String): List<CountryAddressSchema>? {
        val file = File(filename)

        if (file.exists()) {
            return parseAddressesSchema(file.inputStream())
        } else {
            throw InvalidParameterException("Error could not find the test files.")
        }
    }
}
