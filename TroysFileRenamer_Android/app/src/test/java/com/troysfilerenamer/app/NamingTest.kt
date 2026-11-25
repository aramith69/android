package com.troysfilerenamer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NamingTest {

    @Test fun standardPattern() = assertEquals(1 to 4, Naming.parseSeasonEpisode("Show.S01E04.1080p.mkv"))
    @Test fun spacedPattern() = assertEquals(2 to 14, Naming.parseSeasonEpisode("show s2 e14.mkv"))
    @Test fun crossPattern() = assertEquals(2 to 14, Naming.parseSeasonEpisode("Another.Show.2x14.DVDRip.avi"))
    @Test fun writtenOut() = assertEquals(1 to 4, Naming.parseSeasonEpisode("Season 1 Episode 4.mkv"))
    @Test fun compact() = assertEquals(1 to 4, Naming.parseSeasonEpisode("Show.104.mkv"))
    @Test fun compactTwoDigitSeason() = assertEquals(12 to 5, Naming.parseSeasonEpisode("Show 1205.mkv"))

    @Test fun yearIsNotAnEpisode() = assertNull(Naming.parseSeasonEpisode("Movie.2021.1080p.x264.mkv"))
    @Test fun codecIsNotAnEpisode() = assertNull(Naming.parseSeasonEpisode("Movie.H.264.720.mkv"))
    @Test fun resolutionIsNotAnEpisode() = assertNull(Naming.parseSeasonEpisode("Movie 1080.mkv"))
    @Test fun x264IsNotAnEpisode() = assertNull(Naming.parseSeasonEpisode("Clip.x264.mkv"))

    @Test fun showNameFromReleaseFolder() =
        assertEquals("My Show", Naming.showName(listOf("TV", "My.Show.1080p.BluRay")))

    @Test fun showNameStripsYear() =
        assertEquals("Some Show", Naming.showName(listOf("Some Show (2019)")))

    @Test fun seasonFolderUsesParent() =
        assertEquals("My Show", Naming.showName(listOf("TV", "My.Show", "Season 01")))

    @Test fun specialsFolderUsesParent() =
        assertEquals("Shows", Naming.showName(listOf("Shows", "Specials")))

    @Test fun illegalCharactersRemoved() =
        assertEquals("What Up", Naming.showName(listOf("What: Up?")))

    @Test fun newNameFormat() =
        assertEquals("My Show - S01E04.mkv", Naming.buildNewName("My Show", 1, 4, "my.show.s01e04.1080p.MKV"))

    @Test fun videoDetection() {
        assertTrue(Naming.isVideo("a.MKV"))
        assertTrue(Naming.isVideo("a.b.mp4"))
        assertFalse(Naming.isVideo("a.srt"))
        assertFalse(Naming.isVideo("mkv"))
    }
}
