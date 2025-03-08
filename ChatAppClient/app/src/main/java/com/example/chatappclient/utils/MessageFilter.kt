package com.example.chatappclient.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

data class ViolationType(
    val hasBullyWords: Boolean = false,
    val hasSexualHarassmentWords: Boolean = false,
    val hasBadWords: Boolean = false,
    val foundWords: Map<String, List<String>> = mapOf() // Category to found words mapping
) {
    fun hasViolation(): Boolean = hasBullyWords || hasSexualHarassmentWords || hasBadWords
}

object MessageFilter {
    private val firestore = FirebaseFirestore.getInstance()
    private var bullyWords = setOf<String>()
    private var sexualHarassmentWords = setOf<String>()
    private var badWords = setOf<String>()
    private var isInitialized = false
    private var listeners = mutableListOf<ListenerRegistration>()

    // Common character substitutions
    private val charSubstitutions = mapOf(
        'a' to setOf('4', '@'),
        'e' to setOf('3'),
        'i' to setOf('1', '!'),
        'o' to setOf('0'),
        's' to setOf('5', '$'),
        't' to setOf('7'),
        'l' to setOf('1'),
        'b' to setOf('8'),
        'g' to setOf('9'),
        'z' to setOf('2')
    )

    // Common word variations
    private val wordVariations = mapOf(
        "you" to setOf("u", "yu"),
        "are" to setOf("r", "ur"),
        "why" to setOf("y"),
        "your" to setOf("ur", "yr"),
        "please" to setOf("plz", "pls"),
        "because" to setOf("cuz", "bcuz", "bc")
    )

    suspend fun initialize() {
        if (isInitialized) return

        try {
            setupRealTimeListeners()
            fetchAllWords()
            isInitialized = true
        } catch (e: Exception) {
            throw Exception("Failed to initialize MessageFilter: ${e.message}")
        }
    }

    private fun setupRealTimeListeners() {
        // Listen for bully words changes
        val bullyWordsListener = firestore.collection("filtered_words")
            .document("bully_words")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MessageFilter", "Listen failed for bully words.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    bullyWords = (snapshot.get("words") as? List<String>)?.toSet() ?: setOf()
                    Log.d("MessageFilter", "Bully words updated: $bullyWords")
                }
            }
        listeners.add(bullyWordsListener)

        // Listen for sexual harassment words changes
        val sexualHarassmentWordsListener = firestore.collection("filtered_words")
            .document("sexual_harassment_words")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MessageFilter", "Listen failed for sexual harassment words.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    sexualHarassmentWords = (snapshot.get("words") as? List<String>)?.toSet() ?: setOf()
                    Log.d("MessageFilter", "Sexual harassment words updated: $sexualHarassmentWords")
                }
            }
        listeners.add(sexualHarassmentWordsListener)

        // Listen for bad words changes
        val badWordsListener = firestore.collection("filtered_words")
            .document("bad_words")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MessageFilter", "Listen failed for bad words.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    badWords = (snapshot.get("words") as? List<String>)?.toSet() ?: setOf()
                    Log.d("MessageFilter", "Bad words updated: $badWords")
                }
            }
        listeners.add(badWordsListener)
    }

    private suspend fun fetchAllWords() {
        // Fetch initial words from Firestore
        val bullyWordsDoc = firestore.collection("filtered_words")
            .document("bully_words")
            .get()
            .await()

        val sexualHarassmentWordsDoc = firestore.collection("filtered_words")
            .document("sexual_harassment_words")
            .get()
            .await()

        val badWordsDoc = firestore.collection("filtered_words")
            .document("bad_words")
            .get()
            .await()

        // Update local sets
        bullyWords = (bullyWordsDoc.get("words") as? List<String>)?.toSet() ?: setOf()
        sexualHarassmentWords = (sexualHarassmentWordsDoc.get("words") as? List<String>)?.toSet() ?: setOf()
        badWords = (badWordsDoc.get("words") as? List<String>)?.toSet() ?: setOf()

        // Initialize with default sets if empty
        if (bullyWords.isEmpty()) {
            val defaultBullyWords = setOf(
                "loser", "stupid", "dumb", "idiot", "worthless", "ugly",
                "fat", "wimp", "failure", "pathetic", "weak", "coward"
            )
            updateBullyWords(defaultBullyWords)
        }

        if (sexualHarassmentWords.isEmpty()) {
            val defaultSexualHarassmentWords = setOf(
                // Add appropriate default words
                "inappropriate"
            )
            updateSexualHarassmentWords(defaultSexualHarassmentWords)
        }

        if (badWords.isEmpty()) {
            val defaultBadWords = setOf(
                // Add appropriate default words
                "inappropriate"
            )
            updateBadWords(defaultBadWords)
        }
    }

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove special characters
            .replace(Regex("\\s+"), " ")        // Normalize spaces
            .trim()
    }

    private fun generateWordVariations(word: String): Set<String> {
        val variations = mutableSetOf(word)
        
        // Add common variations
        wordVariations[word]?.let { variations.addAll(it) }

        // Generate character substitution variations
        val chars = word.toCharArray()
        for (i in chars.indices) {
            charSubstitutions[chars[i]]?.forEach { substitute ->
                variations.add(word.substring(0, i) + substitute + word.substring(i + 1))
            }
        }

        // Add variations with repeated characters removed
        variations.add(word.replace(Regex("(.)\\1+"), "$1"))

        // Add variations with spaces or dots between characters
        variations.add(word.map { it.toString() }.joinToString("."))
        variations.add(word.map { it.toString() }.joinToString(" "))

        return variations
    }

    private fun findMatchingWords(text: String, wordSet: Set<String>): List<String> {
        val normalizedText = normalizeText(text)
        val words = normalizedText.split(" ")
        val foundWords = mutableSetOf<String>()

        // Check each word in the text
        for (word in words) {
            // Direct match
            if (word in wordSet) {
                foundWords.add(word)
                continue
            }

            // Check for partial matches (substrings)
            wordSet.forEach { bannedWord ->
                if (word.length >= 3 && (word.contains(bannedWord) || bannedWord.contains(word))) {
                    foundWords.add(word)
                }
            }

            // Check for variations of each banned word
            wordSet.forEach { bannedWord ->
                val variations = generateWordVariations(bannedWord)
                if (variations.any { variation -> 
                    word.contains(variation) || 
                    variation.contains(word) ||
                    calculateSimilarity(word, variation) >= 0.8 // 80% similarity threshold
                }) {
                    foundWords.add(word)
                }
            }
        }

        return foundWords.toList()
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        
        if (longer.isEmpty()) return 1.0
        
        return (longer.length - levenshteinDistance(longer, shorter)) / longer.length.toDouble()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        
        for (i in 0..s2.length) costs[i] = i
        
        for (i in 1..s1.length) {
            var lastValue = i
            for (j in 1..s2.length) {
                val oldValue = costs[j]
                val minValue = minOf(
                    lastValue + 1,
                    costs[j - 1] + 1,
                    if (s1[i - 1] == s2[j - 1]) costs[j - 1] else costs[j - 1] + 1
                )
                costs[j] = minValue
                lastValue = oldValue
            }
        }
        
        return costs[s2.length]
    }

    suspend fun checkMessage(message: String): ViolationType {
        if (!isInitialized) {
            initialize()
        }

        // Ensure we have the latest words
        fetchAllWords()

        val foundBullyWords = findMatchingWords(message, bullyWords)
        val foundSexualHarassmentWords = findMatchingWords(message, sexualHarassmentWords)
        val foundBadWords = findMatchingWords(message, badWords)

        // Log the found violations for debugging
        if (foundBullyWords.isNotEmpty()) {
            Log.d("MessageFilter", "Found bully words: $foundBullyWords")
        }
        if (foundSexualHarassmentWords.isNotEmpty()) {
            Log.d("MessageFilter", "Found sexual harassment words: $foundSexualHarassmentWords")
        }
        if (foundBadWords.isNotEmpty()) {
            Log.d("MessageFilter", "Found bad words: $foundBadWords")
        }

        return ViolationType(
            hasBullyWords = foundBullyWords.isNotEmpty(),
            hasSexualHarassmentWords = foundSexualHarassmentWords.isNotEmpty(),
            hasBadWords = foundBadWords.isNotEmpty(),
            foundWords = mapOf(
                "bully" to foundBullyWords,
                "sexual_harassment" to foundSexualHarassmentWords,
                "bad" to foundBadWords
            )
        )
    }

    suspend fun updateBullyWords(words: Set<String>) {
        firestore.collection("filtered_words")
            .document("bully_words")
            .set(mapOf("words" to words.toList()))
            .await()
        bullyWords = words
        Log.d("MessageFilter", "Updated bully words: $words")
    }

    suspend fun updateSexualHarassmentWords(words: Set<String>) {
        firestore.collection("filtered_words")
            .document("sexual_harassment_words")
            .set(mapOf("words" to words.toList()))
            .await()
        sexualHarassmentWords = words
        Log.d("MessageFilter", "Updated sexual harassment words: $words")
    }

    suspend fun updateBadWords(words: Set<String>) {
        firestore.collection("filtered_words")
            .document("bad_words")
            .set(mapOf("words" to words.toList()))
            .await()
        badWords = words
        Log.d("MessageFilter", "Updated bad words: $words")
    }

    suspend fun getBullyWords(): Set<String> {
        if (!isInitialized) {
            initialize()
        }
        return bullyWords
    }

    suspend fun getSexualHarassmentWords(): Set<String> {
        if (!isInitialized) {
            initialize()
        }
        return sexualHarassmentWords
    }

    suspend fun getBadWords(): Set<String> {
        if (!isInitialized) {
            initialize()
        }
        return badWords
    }

    fun cleanup() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
} 