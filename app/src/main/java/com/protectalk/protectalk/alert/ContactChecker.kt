package com.protectalk.protectalk.alert

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object ContactChecker {

    private const val TAG = "ContactChecker"

    /**
     * Checks if a phone number belongs to a known contact in the device's contact list
     * @param context The application context
     * @param phoneNumber The phone number to check
     * @return true if the number is found in contacts, false otherwise
     */
    fun isKnownContact(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) {
            return false
        }

        // Clean the phone number (remove spaces, dashes, etc.)
        val cleanNumber = cleanPhoneNumber(phoneNumber)

        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contactNumber = it.getString(
                        it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    )

                    val cleanContactNumber = cleanPhoneNumber(contactNumber)

                    // Check if numbers match (considering different formats)
                    if (phoneNumbersMatch(cleanNumber, cleanContactNumber)) {
                        Log.d(TAG, "Found matching contact for number: $phoneNumber")
                        return true
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to read contacts", e)
            // If we can't read contacts, assume it's unknown for safety
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking contacts", e)
            return false
        }

        Log.d(TAG, "No matching contact found for number: $phoneNumber")
        return false
    }

    /**
     * Cleans a phone number by removing non-digit characters except +
     */
    private fun cleanPhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^+\\d]"), "")
    }

    /**
     * Checks if two phone numbers match, considering different formats
     */
    private fun phoneNumbersMatch(number1: String, number2: String): Boolean {
        // Direct match
        if (number1 == number2) return true

        // Remove country codes and compare
        val num1WithoutCountry = removeCountryCode(number1)
        val num2WithoutCountry = removeCountryCode(number2)

        if (num1WithoutCountry == num2WithoutCountry) return true

        // Check if one is a suffix of the other (for partial matches)
        val minLength = 7 // Minimum significant digits to compare
        if (num1WithoutCountry.length >= minLength && num2WithoutCountry.length >= minLength) {
            val suffix1 = num1WithoutCountry.takeLast(minLength)
            val suffix2 = num2WithoutCountry.takeLast(minLength)
            return suffix1 == suffix2
        }

        return false
    }

    /**
     * Removes common country codes to normalize phone numbers
     */
    private fun removeCountryCode(phoneNumber: String): String {
        var cleaned = phoneNumber

        // Remove + prefix
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1)
        }

        // Remove common country codes
        when {
            cleaned.startsWith("1") && cleaned.length == 11 -> cleaned = cleaned.substring(1) // US/Canada
            cleaned.startsWith("44") && cleaned.length >= 12 -> cleaned = cleaned.substring(2) // UK
            cleaned.startsWith("972") && cleaned.length >= 12 -> cleaned = cleaned.substring(3) // Israel
            // Add more country codes as needed
        }

        return cleaned
    }
}
