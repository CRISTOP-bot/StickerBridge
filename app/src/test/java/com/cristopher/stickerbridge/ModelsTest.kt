package com.cristopher.stickerbridge

import com.cristopher.stickerbridge.domain.Platform
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {
    @Test fun routeLabelsAreStable() { assertEquals("TikTok", Platform.TIKTOK.label); assertEquals("WhatsApp", Platform.WHATSAPP.label) }
}
