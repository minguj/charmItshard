package com.example.demo.util

object TextUtils {
    fun extractCorkageInfo(inputText: String): List<String> {
        val corkagePattern = Regex(".*(콜키지.?프리|콜키지.?차지|콜키지|corkage|병입료|주류반입|와인|메그넘|위스키|사케).*", RegexOption.IGNORE_CASE)
        return inputText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { corkagePattern.matches(it) }
    }
}
