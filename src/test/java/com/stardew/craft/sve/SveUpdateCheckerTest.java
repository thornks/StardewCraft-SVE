package com.stardew.craft.sve;

/** Regression coverage for CurseForge release filtering and prompt state. */
public final class SveUpdateCheckerTest {
    private SveUpdateCheckerTest() {
    }

    public static void main(String[] args) {
        expectEquals("0.2.1", SveUpdateChecker.extractVersion(
                "stardewcraftsve-0.2.1.jar").orElseThrow(), "version extraction");
        expectEquals("0.3.0-beta.1", SveUpdateChecker.extractVersion(
                "StardewCraftSVE-v0.3.0-beta.1.jar").orElseThrow(), "prefixed version extraction");
        expect(SveUpdateChecker.extractVersion("another-mod-9.0.jar").isEmpty(),
                "unrelated filename rejected");

        String response = """
                {"data":{"latestFiles":[
                  {"id":101,"fileName":"stardewcraftsve-0.2.1.jar","releaseType":1,
                   "fileDate":"2026-08-01T01:00:00Z","gameVersions":["1.21.1","NeoForge"]},
                  {"id":102,"fileName":"stardewcraftsve-9.0.0.jar","releaseType":2,
                   "fileDate":"2026-08-02T01:00:00Z","gameVersions":["1.21.1","NeoForge"]},
                  {"id":103,"fileName":"stardewcraftsve-8.0.0.jar","releaseType":1,
                   "fileDate":"2026-08-03T01:00:00Z","gameVersions":["1.20.1","NeoForge"]},
                  {"id":104,"fileName":"stardewcraftsve-7.0.0.jar","releaseType":1,
                   "fileDate":"2026-08-04T01:00:00Z","gameVersions":["1.21.1","Forge"]}
                ]}}
                """;
        SveUpdateChecker.VersionStatus outdated = SveUpdateChecker.parseResponse(
                "0.2.0", 200, response);
        expect(outdated.isOutdated(), "newer compatible release detected");
        expectEquals("0.2.1", outdated.latestVersion(), "compatible release selected");
        expectEquals(101, outdated.fileId(), "file id selected");
        expectEquals(SveUpdateChecker.PROJECT_URL + "/files/101", outdated.fileUrl(),
                "file-specific URL");

        expectEquals(SveUpdateChecker.State.UP_TO_DATE,
                SveUpdateChecker.parseResponse("v0.2.1", 200, response).state(),
                "v prefix comparison");
        expectEquals(SveUpdateChecker.State.AHEAD,
                SveUpdateChecker.parseResponse("0.2.2", 200, response).state(),
                "ahead comparison");
        expectEquals(SveUpdateChecker.State.UNAVAILABLE,
                SveUpdateChecker.parseResponse("0.2.0", 503, response).state(),
                "non-200 rejected");
        expectEquals(SveUpdateChecker.State.UNAVAILABLE,
                SveUpdateChecker.parseResponse("0.2.0", 200, "{}").state(),
                "empty payload rejected");
        expectEquals(SveUpdateChecker.State.UNAVAILABLE,
                SveUpdateChecker.parseResponse("0.2.0", 200, "not-json").state(),
                "malformed payload rejected");

        SveUpdatePrompt.clearPromptedForTest();
        expect(SveUpdatePrompt.markPromptedForTest("0.2.1"), "first prompt accepted");
        expect(!SveUpdatePrompt.markPromptedForTest("0.2.1"), "duplicate prompt rejected");
        expect(SveUpdatePrompt.markPromptedForTest("0.2.2"), "new version prompt accepted");

        System.out.println("SVE update checker regression suite passed");
    }

    private static void expect(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void expectEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
