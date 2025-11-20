import java.util.ArrayList;
import java.util.List;

/**
 * Simple test framework without dependencies
 * Provides basic assertion methods and test execution
 */
public class SimpleTestFramework {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static List<String> failures = new ArrayList<>();

    /**
     * Assert that a condition is true
     */
    public static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  ✓ " + testName);
        } else {
            failedTests++;
            String error = "FAILED: " + testName + " - Expected true but was false";
            failures.add(error);
            System.out.println("  ✗ " + testName);
        }
    }

    /**
     * Assert that a condition is false
     */
    public static void assertFalse(String testName, boolean condition) {
        assertTrue(testName, !condition);
    }

    /**
     * Assert that two objects are equal
     */
    public static void assertEquals(String testName, Object expected, Object actual) {
        totalTests++;
        boolean equal = (expected == null && actual == null) ||
                       (expected != null && expected.equals(actual));
        if (equal) {
            passedTests++;
            System.out.println("  ✓ " + testName);
        } else {
            failedTests++;
            String error = "FAILED: " + testName + " - Expected: " + expected + ", but was: " + actual;
            failures.add(error);
            System.out.println("  ✗ " + testName + " (expected: " + expected + ", got: " + actual + ")");
        }
    }

    /**
     * Assert that two objects are not equal
     */
    public static void assertNotEquals(String testName, Object notExpected, Object actual) {
        totalTests++;
        boolean notEqual = (notExpected == null && actual != null) ||
                          (notExpected != null && !notExpected.equals(actual));
        if (notEqual) {
            passedTests++;
            System.out.println("  ✓ " + testName);
        } else {
            failedTests++;
            String error = "FAILED: " + testName + " - Expected not equal to: " + notExpected;
            failures.add(error);
            System.out.println("  ✗ " + testName);
        }
    }

    /**
     * Assert that an object is null
     */
    public static void assertNull(String testName, Object obj) {
        assertEquals(testName, null, obj);
    }

    /**
     * Assert that an object is not null
     */
    public static void assertNotNull(String testName, Object obj) {
        totalTests++;
        if (obj != null) {
            passedTests++;
            System.out.println("  ✓ " + testName);
        } else {
            failedTests++;
            String error = "FAILED: " + testName + " - Expected non-null value";
            failures.add(error);
            System.out.println("  ✗ " + testName);
        }
    }

    /**
     * Assert that a string contains a substring
     */
    public static void assertContains(String testName, String haystack, String needle) {
        totalTests++;
        if (haystack != null && haystack.contains(needle)) {
            passedTests++;
            System.out.println("  ✓ " + testName);
        } else {
            failedTests++;
            String error = "FAILED: " + testName + " - String '" + haystack + "' does not contain '" + needle + "'";
            failures.add(error);
            System.out.println("  ✗ " + testName);
        }
    }

    /**
     * Assert that code throws an exception
     */
    public static void assertThrows(String testName, Class<? extends Throwable> expectedException, Runnable code) {
        totalTests++;
        try {
            code.run();
            failedTests++;
            String error = "FAILED: " + testName + " - Expected exception " + expectedException.getName() + " but none was thrown";
            failures.add(error);
            System.out.println("  ✗ " + testName);
        } catch (Throwable t) {
            if (expectedException.isInstance(t)) {
                passedTests++;
                System.out.println("  ✓ " + testName);
            } else {
                failedTests++;
                String error = "FAILED: " + testName + " - Expected " + expectedException.getName() + " but got " + t.getClass().getName();
                failures.add(error);
                System.out.println("  ✗ " + testName + " (got: " + t.getClass().getName() + ")");
            }
        }
    }

    /**
     * Print test section header
     */
    public static void section(String name) {
        System.out.println("\n" + name);
        printChars('-', name.length());
    }

    /**
     * Print test suite header
     */
    public static void suite(String name) {
        System.out.println();
        printChars('=', 60);
        System.out.println(name);
        printChars('=', 60);
    }

    /**
     * Helper to repeat a character (Java 8 compatible)
     */
    private static void printChars(char c, int count) {
        for (int i = 0; i < count; i++) {
            System.out.print(c);
        }
        System.out.println();
    }

    /**
     * Print summary and return exit code
     */
    public static int printSummary() {
        System.out.println();
        printChars('=', 60);
        System.out.println("Test Summary");
        printChars('=', 60);
        System.out.println("Total tests:  " + totalTests);
        System.out.println("Passed:       " + passedTests + " ✓");
        System.out.println("Failed:       " + failedTests + (failedTests > 0 ? " ✗" : ""));
        System.out.println("Success rate: " + (totalTests > 0 ? (passedTests * 100 / totalTests) : 0) + "%");

        if (failedTests > 0) {
            System.out.println("\nFailures:");
            for (String failure : failures) {
                System.out.println("  - " + failure);
            }
        }

        printChars('=', 60);

        return failedTests > 0 ? 1 : 0;
    }

    /**
     * Reset test counters
     */
    public static void reset() {
        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        failures.clear();
    }

    /**
     * Fail a test with a message
     */
    public static void fail(String testName, String message) {
        totalTests++;
        failedTests++;
        String error = "FAILED: " + testName + " - " + message;
        failures.add(error);
        System.out.println("  ✗ " + testName + " (" + message + ")");
    }
}
