/*
 * Copyright 2023-2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.views.factories

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.get
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.extensions.displayString
import com.google.android.fhir.datacapture.extensions.identifierString
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.datacapture.views.QuestionTextConfiguration
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AutoCompleteViewHolderFactoryTest {
  private val parent =
    FrameLayout(
      RuntimeEnvironment.getApplication().apply {
        setTheme(com.google.android.material.R.style.Theme_Material3_DayNight)
      },
    )
  private val viewHolder = AutoCompleteViewHolderFactory.create(parent)

  @Test
  fun shouldSetQuestionHeader() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { text = "Question" },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.question).text.toString())
      .isEqualTo("Question")
  }

  @Test
  fun shouldHaveSingleAnswerChip() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        repeats = false
        addAnswerOption(
          Questionnaire.QuestionnaireItemAnswerOptionComponent()
            .setValue(Coding().setCode("test1-code").setDisplay("Test1 Code")),
        )
        addAnswerOption(
          Questionnaire.QuestionnaireItemAnswerOptionComponent()
            .setValue(Coding().setCode("test2-code").setDisplay("Test2 Code")),
        )
      }
    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                questionnaireItem.answerOption
                  .first { it.value.displayString(parent.context) == "Test1 Code" }
                  .valueCoding
            },
          )
        },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer).childCount)
      .isEqualTo(1)
  }

  @Test
  fun shouldHaveTwoAnswerChipWithExternalValueSet() {
    val answers =
      listOf(
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test1-code").setDisplay("Test1 Code")),
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test2-code").setDisplay("Test2 Code")),
      )

    val fakeAnswerValueSetResolver = { uri: String ->
      if (uri == "http://answwer-value-set-url") {
        answers
      } else {
        emptyList()
      }
    }

    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        repeats = true
        answerValueSet = "http://answwer-value-set-url"
      }
    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                answers.first { it.value.displayString(parent.context) == "Test1 Code" }.valueCoding
            },
          )

          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                answers.first { it.value.displayString(parent.context) == "Test2 Code" }.valueCoding
            },
          )
        },
        enabledAnswerOptions = fakeAnswerValueSetResolver.invoke(questionnaireItem.answerValueSet),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer).childCount)
      .isEqualTo(2)
  }

  @Test
  fun shouldHaveTwoAnswerChipWithAnswerOptionsHavingSameDisplayStringDifferentId() {
    val answers =
      listOf(
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(
            Coding().setCode("test1-code").setDisplay("Test Code").setId("test1-code") as Coding,
          ),
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(
            Coding()
              .setSystem("http://answers/test-codes")
              .setVersion("1.0")
              .setCode("test2-code")
              .setDisplay("Test Code") as Coding,
          ),
      )

    val fakeAnswerValueSetResolver = { uri: String ->
      if (uri == "http://answwer-value-set-url") {
        answers
      } else {
        emptyList()
      }
    }
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        repeats = true
        answerValueSet = "http://answwer-value-set-url"
      }
    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value = answers.first { it.value.id == "test1-code" }.valueCoding
            },
          )

          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                answers
                  .first {
                    it.value.identifierString(parent.context) ==
                      "http://answers/test-codes1.0|test2-code"
                  }
                  .valueCoding
            },
          )
        },
        enabledAnswerOptions = fakeAnswerValueSetResolver.invoke(questionnaireItem.answerValueSet),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer).childCount)
      .isEqualTo(2)
  }

  @Test
  fun shouldHaveSingleAnswerChipWithContainedAnswerValueSet() {
    val answers =
      listOf(
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test1-code").setDisplay("Test1 Code")),
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test2-code").setDisplay("Test2 Code")),
      )

    val fakeAnswerValueSetResolver = { uri: String ->
      if (uri == "http://answwer-value-set-url") {
        answers
      } else {
        emptyList()
      }
    }
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        repeats = false
        answerValueSet = "#ContainedValueSet"
      }

    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                answers.first { it.value.displayString(parent.context) == "Test1 Code" }.valueCoding
            },
          )
        },
        enabledAnswerOptions = fakeAnswerValueSetResolver.invoke(questionnaireItem.answerValueSet),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer).childCount)
      .isEqualTo(1)
  }

  @Test
  fun noDisplayString_shouldShowCode() {
    val answers =
      listOf(
        Questionnaire.QuestionnaireItemAnswerOptionComponent()
          .setValue(Coding().setCode("test1-code"))
          .setInitialSelected(true),
      )

    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent(),
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value = answers.first().valueCoding
            },
          )
        },
        enabledAnswerOptions = answers,
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat((viewHolder.itemView.findViewById<ChipGroup>(R.id.chipContainer)[0] as Chip).text)
      .isEqualTo("test1-code")
  }

  @Test
  fun displayValidationResult_error_shouldShowErrorMessage() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { required = true },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = Invalid(listOf("Missing answer for required field.")),
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.error).visibility)
      .isEqualTo(View.VISIBLE)
    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.error).text)
      .isEqualTo("Missing answer for required field.")
    assertThat(viewHolder.itemView.findViewById<TextInputLayout>(R.id.text_input_layout).error)
      .isNotNull()
  }

  @Test
  fun displayValidationResult_noError_shouldShowNoErrorMessage() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          required = true
          addAnswerOption(
            Questionnaire.QuestionnaireItemAnswerOptionComponent().apply {
              value = Coding().apply { display = "display" }
            },
          )
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
          addAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value = Coding().apply { display = "display" }
            },
          )
        },
        validationResult = Valid,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.error).visibility)
      .isEqualTo(View.GONE)
    assertThat(viewHolder.itemView.findViewById<TextInputLayout>(R.id.text_input_layout).error)
      .isNull()
  }

  @Test
  fun `hides error textview in the header`() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent(),
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.error_text_at_header).visibility)
      .isEqualTo(View.GONE)
  }

  @Test
  fun `show asterisk`() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          text = "Question"
          required = true
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
        questionViewTextConfiguration = QuestionTextConfiguration(showAsterisk = true),
      ),
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.question).text.toString())
      .isEqualTo("Question *")
  }

  @Test
  fun `hide asterisk`() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          text = "Question"
          required = true
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
        questionViewTextConfiguration = QuestionTextConfiguration(showAsterisk = false),
      ),
    )

    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.question).text.toString())
      .isEqualTo("Question")
  }

  @Test
  fun `shows required text`() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { required = true },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
        questionViewTextConfiguration = QuestionTextConfiguration(showRequiredText = true),
      ),
    )

    assertThat(
        viewHolder.itemView.findViewById<TextView>(R.id.required_optional_text).text.toString(),
      )
      .isEqualTo("Required")
  }

  @Test
  fun `hide required text`() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { required = true },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
        questionViewTextConfiguration = QuestionTextConfiguration(showRequiredText = false),
      ),
    )

    assertThat(
        viewHolder.itemView.findViewById<TextView>(R.id.required_optional_text).text.toString(),
      )
      .isEmpty()
    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.required_optional_text).visibility)
      .isEqualTo(View.GONE)
  }

  @Test
  fun `shows optional text`() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { text = "Question" },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
        questionViewTextConfiguration = QuestionTextConfiguration(showOptionalText = true),
      ),
    )

    assertThat(
        viewHolder.itemView.findViewById<TextView>(R.id.required_optional_text).text.toString(),
      )
      .isEqualTo("Optional")
  }

  @Test
  fun `hide optional text`() {
    viewHolder.bind(
      QuestionnaireViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { text = "Question" },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
        questionViewTextConfiguration = QuestionTextConfiguration(showOptionalText = false),
      ),
    )

    assertThat(
        viewHolder.itemView.findViewById<TextView>(R.id.required_optional_text).text.toString(),
      )
      .isEmpty()
    assertThat(viewHolder.itemView.findViewById<TextView>(R.id.required_optional_text).visibility)
      .isEqualTo(View.GONE)
  }
}
