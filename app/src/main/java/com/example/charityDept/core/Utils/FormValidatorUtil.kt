// core/utils/FormValidatorUtil.kt
package com.example.core.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.format.DateTimeParseException

// ===== Generic result type =====
data class ValidationResult<T>(
    val value: T,            // cleaned/derived value
    val error: String? = null
) {
    val isValid get() = error == null
}

// ===== Common helpers =====
private val collapseWhitespace = Regex("\\s+")

// Add a class for *non-ASCII* spaces + tab/vertical/hard spaces.
private val nonAsciiSpaces = Regex(
    "[" +
            "\\u00A0" +        // NBSP
            "\\u1680" +        // Ogham space mark
            "\\u2000-\\u200A" +// EN/EM/3-per-em/... hair space
            "\\u202F" +        // narrow NBSP
            "\\u205F" +        // medium mathematical space
            "\\u3000" +        // ideographic space
            "\\t\\u000B\\u000C" + // tab, vertical tab, form feed
            "]"
)

// fun String.cleanWhitespace(): String = trim().replace(collapseWhitespace, " ")
fun String.cleanWhitespace(preserveGaps: Boolean = true): String =
    if (preserveGaps) {
        // Convert all non-ASCII spacing chars into a normal space, keep multiple spaces, trim edges.
        this.replace(nonAsciiSpaces, " ").trim()
    } else {
        // Old behavior: collapse any run of whitespace to a single space.
        this.trim().replace(collapseWhitespace, " ")
    }

// ===== Validators =====
object FormValidatorUtil {

    // --- Regexes (kept private) ---
// Letters, digits, underscore, spaces, dot, apostrophe, dash, colon; length 2–30
    private val nameRegex = Regex("^[\\p{L}0-9_.'\\-: ]{2,30}$")

    private val emailRegex = Regex("^[A-Za-z0-9+_.:]+@[A-Za-z0-9.-]+$") // simple, practical
    private val phoneRegex = Regex("^\\+?[0-9]{7,15}$")                 // E.164-ish

    // --- Name ---
    // --- Name ---
    fun validateName(raw: String?): ValidationResult<String> {
        val v = raw?.cleanWhitespace().orEmpty()   // 👈 ensure non-null
        val msg = when {
            v.isBlank()           -> "Required."
            v.length < 2          -> "Must be at least 2 characters."
            v.length > 30         -> "Must be at most 30 characters."
            !nameRegex.matches(v) -> "Letters, spaces, . ’ - _ : only."
            else                  -> null
        }
        return ValidationResult(v, msg)
    }

    // --- Email ---
    fun validateEmail(raw: String?): ValidationResult<String> {
        val v = raw?.cleanWhitespace().orEmpty()   // 👈 ensure non-null
        val msg = when {
            v.isBlank()            -> "Required."
            !emailRegex.matches(v) -> "Invalid email address."
            else                   -> null
        }
        return ValidationResult(v, msg)
    }

    // --- Phone ---
    fun validatePhone(raw: String?): ValidationResult<String> {
        val v = raw?.cleanWhitespace().orEmpty()   // 👈 ensure non-null
        val msg = when {
            v.isBlank()            -> "Required."
            !phoneRegex.matches(v) -> "Invalid phone number."
            else                   -> null
        }
        return ValidationResult(v, msg)
    }

    // ===== DOB / Age =====

    /** Years between dob and onDate (full years). */
    @RequiresApi(Build.VERSION_CODES.O)
    fun ageOn(dob: LocalDate, onDate: LocalDate = LocalDate.now()): Int =
        Period.between(dob, onDate).years

    /**
     * Compute DOB from age-in-years keeping the same month/day as `today`,
     * clamping to the end of month if needed (e.g., Feb 30 → Feb 29/28).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun dobFromAgeYears(ageYears: Int, today: LocalDate = LocalDate.now()): LocalDate {
        require(ageYears >= 0) { "ageYears must be >= 0" }
        val targetYear = today.year - ageYears
        val lengthOfMonth = LocalDate.of(targetYear, today.month, 1).lengthOfMonth()
        val day = minOf(today.dayOfMonth, lengthOfMonth)
        return LocalDate.of(targetYear, today.month, day)
    }

    /**
     * Validate DOB is not in the future and age is within bounds.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun validateDob(
        dob: LocalDate,
        minAge: Int = 0,
        maxAge: Int = 120,
        today: LocalDate = LocalDate.now()
    ): ValidationResult<LocalDate> {
        val msg = when {
            dob.isAfter(today) -> "Date of birth cannot be in the future."
            else -> {
                val years = ageOn(dob, today)
                when {
                    years < minAge -> "Minimum age is $minAge."
                    years > maxAge -> "Maximum age is $maxAge."
                    else -> null
                }
            }
        }
        return ValidationResult(dob, msg)
    }

    /**
     * Parse a DOB string by pattern (strict), then validate bounds.
     * Default "dd-MM-uuuu" → e.g., "31-08-2013".
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun validateDobString(
        raw: String,
        pattern: String = "dd-MM-uuuu",
        minAge: Int = 0,
        maxAge: Int = 120,
        today: LocalDate = LocalDate.now()
    ): ValidationResult<LocalDate> {
        val v = raw.cleanWhitespace()
        if (v.isBlank()) return ValidationResult(today, "Required.")
        val fmt = DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.SMART)
        val parsed = try {
            LocalDate.parse(v, fmt)
        } catch (_: DateTimeParseException) {
            return ValidationResult(today, "Invalid date. Use $pattern.")
        }
        return validateDob(parsed, minAge, maxAge, today)
    }

    /**
     * Validate age input (string). Ensures numeric and within bounds,
     * also returns the computed DOB for convenience.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun validateAgeString(
        raw: String,
        minAge: Int = 0,
        maxAge: Int = 120,
        today: LocalDate = LocalDate.now()
    ): ValidationResult<Pair<Int, LocalDate>> {
        val v = raw.cleanWhitespace()
        val years = v.toIntOrNull()
            ?: return ValidationResult(0 to today, "Age must be a whole number.")
        val msg = when {
            years < minAge -> "Minimum age is $minAge."
            years > maxAge -> "Maximum age is $maxAge."
            else -> null
        }
        val dob = dobFromAgeYears(years, today)
        return ValidationResult(years to dob, msg)
    }

    /** Format a LocalDate for UI (default "dd-MM-uuuu"). */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDate(d: LocalDate, pattern: String = "dd-MM-uuuu"): String =
        DateTimeFormatter.ofPattern(pattern).format(d)
}
