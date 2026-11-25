package com.troysfilerenamer.app

/**
 * Naming rules. This is a direct port of the desktop app's logic, so both versions
 * produce identical names for the same files.
 */
object Naming {

    private val VIDEO_EXTENSIONS = setOf(
        "mkv", "mp4", "avi", "mov", "wmv", "m4v", "ts", "mpg", "mpeg", "flv", "webm"
    )

    private val EPISODE_PATTERNS = listOf(
        Regex("""[Ss](\d{1,2})[Ee](\d{1,2})""", RegexOption.IGNORE_CASE),
        Regex("""[Ss](\d{1,2})\s*[Ee](\d{1,2})""", RegexOption.IGNORE_CASE),
        Regex("""(?<![A-Za-z0-9])(\d{1,2})x(\d{1,2})(?![A-Za-z0-9])""", RegexOption.IGNORE_CASE),
        Regex("""[Ss]eason\s*(\d{1,2})\s*[Ee]pisode\s*(\d{1,2})""", RegexOption.IGNORE_CASE),
    )

    // Compact "104" style (season 1, episode 04). Last resort, and guarded so resolutions
    // (720, 1080), years (2021) and codecs (x264, H.264) are never read as episode numbers.
    private val COMPACT_PATTERN = Regex("""(?<![A-Za-z0-9])(?<![Hh]\.)(\d{1,2})(\d{2})(?![A-Za-z0-9])""")
    private val NOT_EPISODES = setOf("480", "576", "720", "1080", "1440", "2160", "4320", "264", "265")

    private val SEASON_FOLDER = Regex("""^(season|series|s)[\s._-]*\d{1,2}$|^specials?$""", RegexOption.IGNORE_CASE)

    private val YEAR_PAREN = Regex("""\(\d{4}\)""")
    private val YEAR_BRACKET = Regex("""\[\d{4}\]""")
    private val QUALITY_TAGS = Regex(
        """\b(720p|1080p|2160p|4k|bluray|blu-ray|webrip|web-dl|hdtv|x264|x265|hevc|avc)\b""",
        RegexOption.IGNORE_CASE
    )
    private val WHITESPACE = Regex("""\s+""")
    private val ILLEGAL_CHARS = Regex("""[<>:"/\\|?*]""")

    fun isVideo(fileName: String): Boolean {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0) return false
        return fileName.substring(dot + 1).lowercase() in VIDEO_EXTENSIONS
    }

    private fun stem(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot > 0) fileName.substring(0, dot) else fileName
    }

    /** Returns (season, episode) or null if the file name has no recognisable pattern. */
    fun parseSeasonEpisode(fileName: String): Pair<Int, Int>? {
        val s = stem(fileName)
        for (pattern in EPISODE_PATTERNS) {
            val m = pattern.find(s)
            if (m != null) {
                return m.groupValues[1].toInt() to m.groupValues[2].toInt()
            }
        }
        for (m in COMPACT_PATTERN.findAll(s)) {
            val digits = m.groupValues[1] + m.groupValues[2]
            if (digits in NOT_EPISODES) continue
            if (digits.length == 4 && digits.toInt() in 1900..2099) continue   // looks like a year
            val season = m.groupValues[1].toInt()
            val episode = m.groupValues[2].toInt()
            if (episode == 0) continue
            return season to episode
        }
        return null
    }

    /**
     * Show name from the folder path (root-most name first, the folder holding the files last).
     * If the files sit in a "Season 01" style folder, the parent folder's name is used instead.
     */
    fun showName(pathNames: List<String>): String {
        var folder = pathNames.lastOrNull().orEmpty()
        if (SEASON_FOLDER.containsMatchIn(folder.trim()) && pathNames.size >= 2) {
            folder = pathNames[pathNames.size - 2]
        }
        var name = folder
        name = YEAR_PAREN.replace(name, "")
        name = YEAR_BRACKET.replace(name, "")
        name = QUALITY_TAGS.replace(name, "")
        name = name.replace('.', ' ').replace('_', ' ')
        name = WHITESPACE.replace(name, " ").trim()
        name = ILLEGAL_CHARS.replace(name, "").trim()
        return name
    }

    fun buildNewName(show: String, season: Int, episode: Int, originalFileName: String): String {
        val ext = "." + originalFileName.substringAfterLast('.').lowercase()
        val s = season.toString().padStart(2, '0')
        val e = episode.toString().padStart(2, '0')
        return "$show - S${s}E$e$ext"
    }
}
