package com.gatekeeperx.ruleflow.utils;

import java.util.*;

/**
 * Utility class for string operations, including fuzzy string distance and ratios.
 */
public class StringUtils {
    /**
     * FuzzyWuzzy-style ratio similarity (0-100), based on normalized Levenshtein distance.
     * 100 = identical, 0 = completely different.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return similarity score (0-100)
     */
    public static int stringDistance(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        // Case-insensitive comparison
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 100;
        int lev = levenshtein(s1, s2);
        return (int) Math.round((1.0 - (double) lev / maxLen) * 100);
    }

    /**
     * Computes the raw Levenshtein distance between two strings.
     * This is the minimum number of single-character edits (insertions, deletions or substitutions)
     * required to change one string into the other.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the Levenshtein distance
     */
    public static int levenshtein(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[len1][len2];
    }

    /**
     * Computes the best substring match ratio (like FuzzyWuzzy's partial_ratio).
     * Finds the best matching substring of the longer string against the shorter string.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return similarity score (0-100)
     */
    public static int partialRatio(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        String shorter = s1.length() < s2.length() ? s1 : s2;
        String longer = s1.length() < s2.length() ? s2 : s1;
        int slen = shorter.length();
        if (slen == 0) return longer.length() == 0 ? 100 : 0;
        int best = 0;
        for (int i = 0; i <= longer.length() - slen; i++) {
            String window = longer.substring(i, i + slen);
            int score = stringDistance(window, shorter);
            if (score > best) best = score;
        }
        return best;
    }

    /**
     * Computes the token sort ratio (ignores word order).
     * Sorts the tokens in each string and then compares.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return similarity score (0-100)
     */
    public static int tokenSortRatio(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        String sorted1 = sortTokens(s1);
        String sorted2 = sortTokens(s2);
        return stringDistance(sorted1, sorted2);
    }

    /**
     * Computes the token set ratio (ignores word order and duplicates).
     * Compares the intersection and differences of unique tokens.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return similarity score (0-100)
     */
    public static int tokenSetRatio(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        Set<String> tokens1 = new HashSet<>(tokenize(s1));
        Set<String> tokens2 = new HashSet<>(tokenize(s2));
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);
        Set<String> diff1 = new HashSet<>(tokens1);
        diff1.removeAll(tokens2);
        Set<String> diff2 = new HashSet<>(tokens2);
        diff2.removeAll(tokens1);
        String sortedIntersection = sortTokens(String.join(" ", intersection));
        String sortedDiff1 = sortTokens(String.join(" ", diff1));
        String sortedDiff2 = sortTokens(String.join(" ", diff2));
        String combined1 = sortedIntersection + " " + sortedDiff1;
        String combined2 = sortedIntersection + " " + sortedDiff2;
        int ratio1 = stringDistance(sortedIntersection, combined1);
        int ratio2 = stringDistance(sortedIntersection, combined2);
        int ratio3 = stringDistance(combined1, combined2);
        return Math.max(Math.max(ratio1, ratio2), ratio3);
    }

    private static List<String> tokenize(String s) {
        return Arrays.asList(s.trim().toLowerCase().split("\\s+"));
    }

    private static String sortTokens(String s) {
        List<String> tokens = tokenize(s);
        Collections.sort(tokens);
        return String.join(" ", tokens);
    }

    /**
     * Computes the best overall similarity score between two strings, using all available string similarity algorithms.
     * Returns the maximum of string_distance, partial_ratio, token_sort_ratio, and token_set_ratio.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the best similarity score (0-100)
     */
    public static int stringSimilarityScore(String s1, String s2) {
        return Math.max(
            Math.max(stringDistance(s1, s2), partialRatio(s1, s2)),
            Math.max(tokenSortRatio(s1, s2), tokenSetRatio(s1, s2))
        );
    }
} 