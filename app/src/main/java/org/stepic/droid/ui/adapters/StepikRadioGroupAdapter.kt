package org.stepic.droid.ui.adapters

import android.widget.Button
import org.stepic.droid.analytic.Analytic
import org.stepic.droid.base.App
import org.stepic.droid.model.Attempt
import org.stepic.droid.model.Reply
import org.stepic.droid.model.Submission
import org.stepic.droid.ui.custom.StepikCheckBox
import org.stepic.droid.ui.custom.StepikOptionView
import org.stepic.droid.ui.custom.StepikRadioButton
import org.stepic.droid.ui.custom.StepikRadioGroup
import java.lang.Math.min
import javax.inject.Inject


class StepikRadioGroupAdapter(private val group: StepikRadioGroup) {
    @Inject
    lateinit var analytic: Analytic

    init {
        App.component().inject(this)
    }

    var actionButton: Button? = null
        set(value) {
            field = value
            refreshActionButton()
        }

    private var isMultipleChoice = false

    fun setAttempt(attempt: Attempt?) {
        attempt?.dataset?.options?.let { options ->
            if (options.isEmpty()) return
            group.removeAllViews()
            group.clearCheck()

            isMultipleChoice = attempt.dataset.is_multiple_choice
            options.forEach {
                val item = if (isMultipleChoice) {
                    StepikCheckBox(group.context)
                } else {
                    StepikRadioButton(group.context)
                }
                item.setText(it)
                group.addView(item)
            }

            if (!isMultipleChoice) {
                group.setOnCheckedChangeListener { _, _ ->
                    refreshActionButton()
                    group.setOnCheckedChangeListener(null)
                }
            }

            refreshActionButton()
        }
    }

    fun setSubmission(submission: Submission?) {
        submission?.reply?.choices?.let { choices ->
            if (choices.size < group.childCount) {
                analytic.reportEventWithName(Analytic.Error.CHOICES_ARE_SMALLER, submission.id?.toString())
            }
            setChoices(choices)
            // no need to set up actionButton state it will be done for us
        }
    }

    fun setChoices(choices: List<Boolean>) {
        (0 until min(group.childCount, choices.size)).forEach {
            (group.getChildAt(it) as StepikOptionView).isChecked = choices[it]
        }
    }

    val reply: Reply
        get() {
            val selection = (0 until group.childCount)
                    .map {
                        (group.getChildAt(it) as StepikOptionView).isChecked
                    }
            return Reply.Builder().setChoices(selection).build()
        }

    fun setEnabled(isEnabled: Boolean) {
        (0 until group.childCount).forEach { group.getChildAt(it).isEnabled = isEnabled }
    }

    private fun refreshActionButton() {
        actionButton?.let {
            it.isEnabled = isMultipleChoice || group.checkedRadioButtonId != -1
        }
    }
}