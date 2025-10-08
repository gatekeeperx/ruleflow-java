package utils;

import com.gatekeeperx.ruleflow.utils.StringUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {
    @Test
    void testIdenticalStrings() {
        assertEquals(100, StringUtils.stringDistance("hello", "hello"));
        assertEquals(4, StringUtils.levenshtein("hello", "world")); // sanity check, not similarity
    }

    @Test
    void testCompletelyDifferentStrings() {
        assertEquals(20, StringUtils.stringDistance("hello", "world")); // 4/5 edits, so 1/5 similarity
        assertEquals(4, StringUtils.levenshtein("hello", "world"));
    }

    @Test
    void testEmptyStrings() {
        assertEquals(100, StringUtils.stringDistance("", ""));
        assertEquals(0, StringUtils.stringDistance("", "test"));
        assertEquals(0, StringUtils.stringDistance("abc", ""));
        assertEquals(4, StringUtils.levenshtein("", "test"));
        assertEquals(3, StringUtils.levenshtein("abc", ""));
    }

    @Test
    void testSingleEdit() {
        assertEquals(75, StringUtils.stringDistance("test", "tent")); // 1/4 edit
        assertEquals(80, StringUtils.stringDistance("tests", "test")); // 1/5 edit
        assertEquals(75, StringUtils.stringDistance("test", "est")); // 1/4 edit
        assertEquals(1, StringUtils.levenshtein("test", "tent"));
        assertEquals(1, StringUtils.levenshtein("tests", "test"));
        assertEquals(1, StringUtils.levenshtein("test", "est"));
    }

    @Test
    void testFuzzyCases() {
        assertEquals(57, StringUtils.stringDistance("kitten", "sitting")); // 3/7 edits
        assertEquals(50, StringUtils.stringDistance("flaw", "lawn")); // 2/4 edits
        assertEquals(67, StringUtils.stringDistance("abc", "ab")); // 1/3 edit
        assertEquals(3, StringUtils.levenshtein("kitten", "sitting"));
        assertEquals(2, StringUtils.levenshtein("flaw", "lawn"));
        assertEquals(1, StringUtils.levenshtein("abc", "ab"));
    }

    @Test
    void testNullInputs() {
        assertEquals(0, StringUtils.stringDistance(null, "abc"));
        assertEquals(0, StringUtils.stringDistance("abc", null));
        assertEquals(100, StringUtils.stringDistance(null, null));
        assertEquals(3, StringUtils.levenshtein(null, "abc"));
        assertEquals(3, StringUtils.levenshtein("abc", null));
        assertEquals(0, StringUtils.levenshtein(null, null));
    }

    @Test
    void testFuzzyWuzzyExample_ganchozojose572_vs_JOSE_GANCHOZO() {
        String s1 = "ganchozojose572";
        String s2 = "JOSEGANCHOZO";
        int ratio = StringUtils.stringDistance(s1, s2);
        int partial = StringUtils.partialRatio(s1, s2);
        int sort = StringUtils.tokenSortRatio(s1, s2);
        int set = StringUtils.tokenSetRatio(s1, s2);
        System.out.println("string_distance: " + ratio);
        System.out.println("partial_ratio: " + partial);
        System.out.println("token_sort_ratio: " + sort);
        System.out.println("token_set_ratio: " + set);
        assertTrue(ratio >= 0 && ratio <= 100);
        assertTrue(partial >= 0 && partial <= 100);
        assertTrue(sort >= 0 && sort <= 100);
        assertTrue(set >= 0 && set <= 100);
    }

    @Test
    void testFuzzyWuzzyExample_19a8193_vs_ricardoanorve() {
        String s1 = "19a8193";
        String s2 = "ricardoanorve";
        int ratio = StringUtils.stringDistance(s1, s2);
        int partial = StringUtils.partialRatio(s1, s2);
        int sort = StringUtils.tokenSortRatio(s1, s2);
        int set = StringUtils.tokenSetRatio(s1, s2);
        System.out.println("string_distance: " + ratio);
        System.out.println("partial_ratio: " + partial);
        System.out.println("token_sort_ratio: " + sort);
        System.out.println("token_set_ratio: " + set);
        assertTrue(ratio >= 0 && ratio <= 100);
        assertTrue(partial >= 0 && partial <= 100);
        assertTrue(sort >= 0 && sort <= 100);
        assertTrue(set >= 0 && set <= 100);
    }

    @Test
    void testStringStringDistance_ganchozojose572_vs_JOSEGANCHOZO() {
        String s1 = "ganchozojose572";
        String s2 = "JOSEGANCHOZO";
        int score = StringUtils.stringSimilarityScore(s1, s2);
        System.out.println("string_similarity_score: " + score);
        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    void testStringStringDistance_19A8193_vs_ricardoanorve() {
        String s1 = "19a8193";
        String s2 = "ricardoanorve";
        int score = StringUtils.stringSimilarityScore(s1, s2);
        System.out.println("string_similarity_score: " + score);
        assertTrue(score >= 0 && score <= 100);
    }
} 