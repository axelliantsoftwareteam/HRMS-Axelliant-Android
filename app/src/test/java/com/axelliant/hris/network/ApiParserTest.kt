package com.axelliant.hris.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiParserTest {

    private data class Slip(val name: String? = null, val net_pay: Double? = null)
    private data class Page(val items: List<Slip> = emptyList(), val total: Int? = null)

    private fun <T> parse(json: String, type: Class<T>): ApiResult<T> = ApiParser.parse(json, type)

    @Test
    fun `reads payload from message data`() {
        val json = """{"message":{"data":{"items":[{"name":"S1","net_pay":"24600.5"}],"total":"3","meta":{"status":true}}}}"""
        val result = parse(json, Page::class.java)
        assertTrue(result is ApiResult.Success)
        val page = (result as ApiResult.Success).data
        assertEquals(1, page.items.size)
        assertEquals(24600.5, page.items[0].net_pay!!, 0.0)
        assertEquals(3, page.total)
    }

    @Test
    fun `reads payload placed directly in message`() {
        val json = """{"message":{"items":[{"name":"S2"}],"total":1}}"""
        val result = parse(json, Page::class.java) as ApiResult.Success
        assertEquals("S2", result.data.items.single().name)
    }

    @Test
    fun `meta status false becomes a failure with the server message`() {
        val json = """{"message":{"data":{"meta":{"status":false,"message":"Nope"}}}}"""
        val result = parse(json, Page::class.java)
        assertTrue(result is ApiResult.Failure)
        assertEquals("Nope", (result as ApiResult.Failure).message)
    }

    @Test
    fun `success message comes from status_message`() {
        val json = """{"message":{"data":{"status_message":"Done","meta":{"status":true}}}}"""
        val result = parse(json, Page::class.java) as ApiResult.Success
        assertEquals("Done", result.message)
    }

    @Test
    fun `blank or invalid bodies fail instead of throwing`() {
        assertTrue(parse("", Page::class.java) is ApiResult.Failure)
        assertTrue(parse("not json", Page::class.java) is ApiResult.Failure)
        assertTrue(parse("[1,2]", Page::class.java) is ApiResult.Failure)
    }
}
