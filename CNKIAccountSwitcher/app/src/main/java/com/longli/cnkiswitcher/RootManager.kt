package com.longli.cnkiswitcher

import java.io.BufferedReader
import java.io.InputStreamReader

data class Account(
    val folder: String,
    val displayName: String,
    val isCurrent: Boolean
)

class RootManager {
    companion object {
        const val ROOT_DIR = "/data/adb/cnki-accounts"
        const val APP_ID = "com.cnki.android.cnkimobile"
        const val PREF_PATH = "/data/data/com.cnki.android.cnkimobile/shared_prefs/cnki_mobile.xml"
    }

    fun runRoot(script: String): String {
        val p = ProcessBuilder("su", "-c", script).redirectErrorStream(true).start()
        val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
        val code = p.waitFor()
        if (code != 0) throw IllegalStateException(out.ifBlank { "Root 命令失败：$code" })
        return out.trim()
    }

    fun checkRoot(): Boolean = try { runRoot("id"); true } catch (_: Exception) { false }

    fun loadAccounts(): List<Account> {
        val script = """
            mkdir -p '$ROOT_DIR'
            LIVE_HASH=
            [ -s '$PREF_PATH' ] && LIVE_HASH=${'$'}(sha256sum '$PREF_PATH' | cut -d' ' -f1)
            for d in '$ROOT_DIR'/*; do
              [ -d "${'$'}d" ] || continue
              f="${'$'}d/cnki_mobile.xml"
              [ -s "${'$'}f" ] || continue
              h=${'$'}(sha256sum "${'$'}f" | cut -d' ' -f1)
              n=${'$'}(basename "${'$'}d")
              v=${'$'}(cat "${'$'}d/display_name" 2>/dev/null)
              [ -n "${'$'}v" ] || v="${'$'}n"
              c=0; [ "${'$'}h" = "${'$'}LIVE_HASH" ] && c=1
              printf '%s\t%s\t%s\n' "${'$'}n" "${'$'}v" "${'$'}c"
            done
        """.trimIndent()
        val raw = runRoot(script)
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull {
            val p = it.split('\t')
            if (p.size < 3) null else Account(p[0], p[1], p[2] == "1")
        }.toList()
    }

    fun switchAccount(folder: String) {
        require(folder.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "账号目录名不合法" }
        val src = "$ROOT_DIR/$folder/cnki_mobile.xml"
        runRoot("""
            set -e
            test -s '$src'
            am force-stop '$APP_ID'
            sleep 1
            test -d '/data/data/com.cnki.android.cnkimobile/shared_prefs'
            cp '$src' '$PREF_PATH'
            test -s '$PREF_PATH'
            monkey -p '$APP_ID' 1 >/dev/null 2>&1
        """.trimIndent())
    }

    fun renameAccount(folder: String, name: String) {
        require(folder.matches(Regex("[A-Za-z0-9._-]{1,100}")))
        val safe = name.trim().take(60).replace("'", "'\\''")
        require(safe.isNotBlank()) { "备注不能为空" }
        runRoot("printf '%s' '$safe' > '$ROOT_DIR/$folder/display_name'")
    }

    fun deleteAccount(folder: String) {
        require(folder.matches(Regex("[A-Za-z0-9._-]{1,100}")))
        runRoot("rm -rf '$ROOT_DIR/$folder'")
    }
}
