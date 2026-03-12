package com.axelliant.hris.extention

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtentionsTest {

    @Test
    fun `blank message maps to generic supportable error`() {
        val message = "   "

        assertEquals(
            "Something went wrong. Please try again.",
            message.toUserSafeErrorMessage()
        )
        assertTrue(message.shouldOfferSupportForError())
    }

    @Test
    fun `null string literal maps to generic supportable error`() {
        val message = "null"

        assertEquals(
            "Something went wrong. Please try again.",
            message.toUserSafeErrorMessage()
        )
        assertTrue(message.shouldOfferSupportForError())
    }

    @Test
    fun `permission errors map to access guidance`() {
        val message = "User does not have doctype access for Expense Claim"

        assertEquals(
            "Your account does not have access to this action yet. Please contact HR or your administrator.",
            message.toUserSafeErrorMessage()
        )
        assertTrue(message.shouldOfferSupportForError())
    }

    @Test
    fun `network errors map to connection guidance`() {
        val message = "Unable to resolve host hris.axelliant.com"

        assertEquals(
            "We could not connect to HRIS right now. Please check your internet connection and try again.",
            message.toUserSafeErrorMessage()
        )
        assertTrue(message.shouldOfferSupportForError())
    }

    @Test
    fun `server html errors map to supportable generic failure`() {
        val message = "<html><body>500 Server Error</body></html>"

        assertEquals(
            "We could not complete this request. Please try again. If it keeps happening, report the issue to HRIS support.",
            message.toUserSafeErrorMessage()
        )
        assertTrue(message.shouldOfferSupportForError())
    }

    @Test
    fun `non technical messages pass through without support escalation`() {
        val message = "Only Pending request can modified"

        assertEquals(message, message.toUserSafeErrorMessage())
        assertFalse(message.shouldOfferSupportForError())
    }
}
