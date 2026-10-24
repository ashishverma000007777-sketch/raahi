package `in`.raahi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NotificationIconResourceTest {

    @Test
    fun testNotificationIconXmlStructureAndColors() {
        // Resolve ic_notification.xml file across possible working directory locations
        val candidates = listOf(
            File("src/main/res/drawable/ic_notification.xml"),
            File("app/src/main/res/drawable/ic_notification.xml"),
            File("android/app/src/main/res/drawable/ic_notification.xml"),
            File("../app/src/main/res/drawable/ic_notification.xml"),
            File("/data/data/com.termux/files/home/raahi_repo/android/app/src/main/res/drawable/ic_notification.xml")
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("ic_notification.xml must exist", file)

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val root = doc.documentElement

        assertEquals("Root element must be vector", "vector", root.localName ?: root.tagName)
        val androidNs = "http://schemas.android.com/apk/res/android"

        val width = root.getAttributeNS(androidNs, "width")
        val height = root.getAttributeNS(androidNs, "height")
        assertEquals("Width must be 24dp for notification small icon", "24dp", width)
        assertEquals("Height must be 24dp for notification small icon", "24dp", height)

        val vpWidth = root.getAttributeNS(androidNs, "viewportWidth")
        val vpHeight = root.getAttributeNS(androidNs, "viewportHeight")
        assertEquals("1000", vpWidth)
        assertEquals("1000", vpHeight)

        val paths = root.getElementsByTagName("path")
        assertTrue("Vector must contain paths", paths.length >= 3)

        for (i in 0 until paths.length) {
            val pathElem = paths.item(i) as Element
            val fillColor = pathElem.getAttributeNS(androidNs, "fillColor")
            assertEquals("Path $i must have pure white fillColor for monochrome silhouette", "#FFFFFFFF", fillColor)

            val pathData = pathElem.getAttributeNS(androidNs, "pathData")
            assertTrue("Path $i pathData must not be empty", pathData.isNotBlank())
            assertTrue("Path $i pathData must start with Move command", pathData.trim().startsWith("M"))
            assertTrue("Path $i pathData must be closed with Z command", pathData.trim().endsWith("Z"))

            // Verify no gradient or color corruption
            assertFalse(fillColor.contains("gradient", ignoreCase = true))
        }
    }
}
