package com.galvyx.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GalvyxModelsTest {
    @Test
    fun siteVisitSummaryCountsCapturedItems() {
        val visit = SiteVisit(
            notes = listOf(VisitNote(title = "Rack"), VisitNote(title = "MDF")),
            devices = listOf(DeviceInfo(hostname = "switch-1")),
            expenses = listOf(VisitExpense(vendor = "Fuel")),
            photos = listOf(VisitPhoto(path = "/tmp/photo.jpg"), VisitPhoto(path = "/tmp/receipt.jpg"))
        )

        assertEquals("2 notes • 1 devices • 1 expenses • 2 photos", visit.summary())
    }

    @Test
    fun visitsJsonRoundTripPreservesCoreVisitDetails() {
        val visits = listOf(
            SiteVisit(
                clientName = "Reset",
                projectName = "Lake Tahoe Network Survey",
                technicianName = "Omar",
                date = "2026-08-21",
                jobType = "Network Survey",
                notes = listOf(VisitNote(location = "MDF", category = "Switch / Firewall", title = "Firewall", notes = "FCC listener checked")),
                devices = listOf(DeviceInfo(location = "MDF", deviceType = "Firewall", manufacturer = "SonicWall", ipAddress = "10.0.31.31")),
                expenses = listOf(VisitExpense(date = "2026-08-21", category = "Meal", vendor = "Cafe", amount = "18.50")),
                photos = listOf(VisitPhoto(path = "/storage/photo.jpg", caption = "MDF rack"))
            )
        )

        val restored = visitsFromJson(visitsToJson(visits))

        assertEquals(1, restored.size)
        assertEquals("Reset", restored.first().clientName)
        assertEquals("Lake Tahoe Network Survey", restored.first().projectName)
        assertEquals("Network Survey", restored.first().jobType)
        assertEquals("Firewall", restored.first().notes.first().title)
        assertEquals("10.0.31.31", restored.first().devices.first().ipAddress)
        assertEquals("18.50", restored.first().expenses.first().amount)
        assertEquals("MDF rack", restored.first().photos.first().caption)
    }
}
