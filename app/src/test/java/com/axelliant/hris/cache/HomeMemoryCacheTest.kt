package com.axelliant.hris.cache

import com.axelliant.hris.config.AppConst
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.dashboard.DashboardResponse
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.network.ApiResult
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HomeMemoryCacheTest {
    private val originalToken = AppConst.TOKEN
    private val originalZone = TimeZone.getDefault()

    @Before fun setup() { HomeMemoryCache.clear(); AppConst.TOKEN = "test-session" }
    @After fun cleanup() {
        HomeMemoryCache.clear()
        AppConst.TOKEN = originalToken
        TimeZone.setDefault(originalZone)
    }

    @Test fun successfulAttendanceInvalidatesBothSnapshots() = runBlocking {
        val key = HomeMemoryCache.key()!!
        HomeMemoryCache.dashboard.load(key, { true }) {
            ApiResult.Success(DashboardResponse(meta = Meta("OK", true)))
        }
        HomeMemoryCache.team.load(key, { true }) {
            ApiResult.Success(TodayTeamResponse(meta = Meta("OK", true)))
        }
        assertTrue(HomeMemoryCache.dashboard.isFresh(key))
        assertTrue(HomeMemoryCache.team.isFresh(key))
        HomeMemoryCache.attendanceChanged()
        assertNull(HomeMemoryCache.dashboard.peek(key))
        assertNull(HomeMemoryCache.team.peek(key))
    }

    @Test fun sameTokenAfterLogoutGetsNewIdentityAndBlankTokensHaveNoKey() {
        val previous = HomeMemoryCache.key()
        HomeMemoryCache.clear()
        assertNotEquals(previous, HomeMemoryCache.key())
        AppConst.TOKEN = ""
        assertNull(HomeMemoryCache.key())
    }

    @Test fun timezoneChangeCannotReusePreviousDashboard() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val previous = HomeMemoryCache.key()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Karachi"))
        assertNotEquals(previous, HomeMemoryCache.key())
    }
}
