package dev.capriguard.passwordguard.audit

/**
 * Small embedded corpora, on device, so a reading exists in airplane mode.
 *
 * Both are deliberately short. A short list can only ever confirm a hit, never
 * clear a miss, and every caller has to say so: "not in our list" is not the same
 * statement as "not breached".
 */
object Known {

    /**
     * Ordered worst-first, from the long-running public "most common passwords"
     * corpora. Position is the guess budget: entry n is reachable in about n
     * tries, which is why this is a List and not a Set.
     */
    val common: List<String> = listOf(
        "123456", "password", "123456789", "12345678", "1234567", "12345", "1234567890",
        "1234", "121212", "123123", "000000", "qwerty", "zzzzzz", "adobe123", "111111",
        "photoshop", "money", "shadow", "michael", "monkey", "trustno1", "dragon",
        "654321", "test", "pass", "iloveyou", "princess", "123qwe", "welcome", "hello",
        "charlie", "donald", "password1", "qwerty123", "1q2w3e4r", "admin", "login",
        "master", "hello123", "freedom", "whatever", "qazwsx", "jordan", "jordan23",
        "hunter", "hunter2", "buster", "soccer", "andrew", "tinkle", "golden",
        "garfield", "access", "flower", "hottie", "abigail", "cheese", "matthew",
        "patience", "password123", "qwertyuiop", "222222", "arsenal", "loveme",
        "112358", "fuckyou", "pumpkin", "snoopy", "buffy", "555555", "secret",
        "superman", "212121", "987654321", "888888", "sunshine", "7777777", "987654",
        "forever", "mustang", "123456a", "liverpool", "justin", "11111111", "131313",
        "firebird", "passw0rd", "matrix", "12341234", "00000000", "1111", "1111111",
        "123321", "88888888", "666666", "159753", "william", "computer", "maverick",
        "phantom", "marlboro", "internet", "arizona", "teens", "kevin", "bright",
        "addison", "samsung", "android", "google", "apple", "skippy", "chelsea",
        "timothy", "corvette", "austin", "veronica", "goldfish", "samuel", "sapphire",
        "yankees", "victoria", "biteme", "trucks", "george", "monica", "carmen",
        "159357", "1q2w3e", "102030", "101010", "123qweasd", "qwe123", "asd123",
        "aaa111", "pass123", "test123", "qwerty1", "qwerty12", "abc12345", "abcd1234",
        "123456789a", "12345a", "pa55word", "pa55w0rd", "p@ssword", "p@ssw0rd",
        "iloveyou1", "princess1", "ncc1701", "genesis", "redskins", "lucky", "lucky1",
        "scooter", "stupid", "dallas", "pepper", "ginger", "starwars", "abcdefg",
        "abcdef", "112233", "12121212", "13131313", "66666666", "77777777", "1111111111",
        "1234321", "232323", "123123123", "1q1q1q", "asdfgh", "zxcvbnm", "qwerqwer",
        "qweasdzxc", "147258369", "12345q", "qwerty12345", "12345abc", "password12",
        "welcome1", "welcome123", "super123", "fish", "magic", "sammy", "robert",
        "jessica", "jennifer", "michelle", "america", "player", "sparky", "western",
        "jason", "cameron", "sierra", "peanut", "taylor", "diablo", "guitar", "daniel",
        "thomas", "crystal", "anthony", "james", "porsche", "forest", "winston",
        "madison", "letmein123", "qwe123qwe", "abcd12345", "123456b", "skeleton",
    )

    /**
     * Plain words used to catch "one English word plus a digit". A shape match,
     * so any finding built on it is reported as possible, never certain.
     */
    val words: Set<String> = setOf(
        "love", "hate", "life", "time", "money", "power", "secret", "magic", "angel",
        "devil", "tiger", "lion", "bear", "wolf", "hawk", "eagle", "shark", "dragon",
        "phoenix", "sunny", "cloudy", "storm", "river", "ocean", "water", "fire",
        "earth", "moon", "star", "sun", "sky", "rain", "snow", "winter", "summer",
        "spring", "autumn", "monday", "friday", "january", "december", "march",
        "hello", "world", "admin", "root", "user", "guest", "test", "demo", "home",
        "house", "apple", "banana", "coffee", "cookie", "pepper", "ginger", "honey",
        "sugar", "spice", "candy", "cheese", "bread", "pizza", "pasta", "music",
        "rock", "metal", "jazz", "blues", "dancer", "singer", "guitar", "piano",
        "movie", "comic", "game", "play", "winner", "champion", "player", "soccer",
        "tennis", "boxing", "racing", "speed", "fast", "quick", "strong", "brave",
        "happy", "lucky", "crazy", "wild", "free", "trust", "faith", "hope", "dream",
        "wish", "heart", "soul", "mind", "brain", "smile", "laugh", "friend",
        "family", "mother", "father", "sister", "brother", "baby", "child", "people",
        "person", "name", "number", "letter", "color", "black", "white", "green",
        "blue", "red", "orange", "purple", "silver", "golden", "diamond", "ruby",
        "pearl", "copper", "steel", "iron", "pass", "sword", "shield", "knight",
        "castle", "tower", "gate", "door", "window", "garden", "forest", "desert",
        "island", "valley", "mount", "hill", "lake", "field", "road", "street",
        "city", "town", "village", "school", "office", "work", "team", "company",
        "market", "bank", "doctor", "nurse", "lawyer", "chef", "writer", "reader",
        "travel", "voyage", "pilot", "captain", "sailor", "soldier", "hero", "legend",
        "master", "monkey", "rabbit", "panda", "koala", "kitten", "puppy", "horse",
        "zebra", "camel", "snake", "spider", "mouse", "donald", "princess", "prince",
        "queen", "king", "royal", "peace", "revenge", "shadow", "ghost", "spirit",
        "vampire", "zombie", "alien", "planet", "cosmos", "galaxy", "rocket",
        "laser", "cyber", "digital", "binary", "vector", "matrix", "iphone", "windows",
        "linux", "unix",
    )

    /** Row-major keyboard runs, including each row reversed. */
    private val rows: List<String> = listOf(
        "qwertyuiop", "asdfghjkl", "zxcvbnm", "1234567890",
        "poiuytrewq", "lkjhgfdsa", "mnbvcxz", "0987654321",
    )

    /** True for [s] when it contains a straight run of [min] or more keys on one row. */
    fun keyboardRun(s: String, min: Int = 4): Boolean {
        val t = s.lowercase()
        return rows.any { row ->
            (min..t.length).any { len ->
                (0..t.length - len).any { i ->
                    val piece = t.substring(i, i + len)
                    row.contains(piece) || row.reversed().contains(piece)
                }
            }
        }
    }

    /**
     * Fold the lookalike substitutions guessers try first, so "p@ssw0rd" can be
     * recognised as the word underneath it.
     */
    fun deleet(s: String): String {
        val map = mapOf(
            '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's',
            '7' to 't', '8' to 'b', '@' to 'a', '$' to 's', '!' to 'i', '(' to 'c',
        )
        return s.lowercase().map { map[it] ?: it }.joinToString("")
    }
}
