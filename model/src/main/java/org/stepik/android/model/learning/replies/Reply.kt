package org.stepik.android.model.learning.replies

import com.google.gson.annotations.SerializedName
import org.stepik.android.model.learning.submissions.Attachment

class Reply(
        val choices: List<Boolean>? = null,
        val text: String? = null,
        val attachments: List<Attachment>? = null,
        val formula: String? = null,
        val number: String? = null,
        val ordering: List<Int>? = null,
        val language: String? = null,
        val code: String? = null,

        @SerializedName("solve_sql")
        val solveSql: String? = null,

        val blanks: List<String>? = null,
        var tableChoices: List<TableChoiceAnswer>? = null //this is not serialize by default, because  field 'choices' is already created by different type
)