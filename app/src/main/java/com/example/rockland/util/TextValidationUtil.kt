package com.example.rockland.util

object TextValidationUtil {

    const val MAP_COMMENT_ERROR = "Comment must be between 10 and 500 characters. " +
            "Please edit your comment and try again."
    const val EXPERT_ANNOTATION_ERROR = "Expert Annotation must be between 10 and 500 characters. " +
            "Please edit your annotation and try agaim."
    const val ROCK_DESCRIPTION_ERROR =
        "Description must be between 10 and 1000 characters. Please edit your description and try again."

    sealed class Result {
        data object Ok : Result()
        data class Error(val message: String) : Result()
    }

    /**
      LENGTH VALIDATION
      1) trim whitespace before counting AND
      2) acceptable range: [min, max]
     */
    fun validateLength(text: String, min: Int, max: Int, errorMessage: String): Result {
        val trimmed = text.trim()
        return if (trimmed.length in min..max) Result.Ok else Result.Error(errorMessage)
    }

    fun validateMapComment(text: String): Result =
        validateLength(text, min = 10, max = 500, errorMessage = MAP_COMMENT_ERROR)

    fun validateExpertAnnotation(text: String): Result =
        validateLength(text, min = 10, max = 1000, errorMessage = EXPERT_ANNOTATION_ERROR)

    fun validateRockDescription(text: String): Result =
        validateLength(text, min = 10, max = 1000, errorMessage = ROCK_DESCRIPTION_ERROR)
}
