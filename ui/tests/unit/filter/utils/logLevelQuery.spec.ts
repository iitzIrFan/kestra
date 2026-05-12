import {describe, expect, it} from "vitest";
import {
    hasUnsupportedRouteLevelComparator,
    normalizeRouteLevelFilter,
    readRouteLevelFilter,
} from "@kestra-io/design-system";

describe("log level query helpers", () => {
    it("reads AT_OR_BELOW level from route query", () => {
        expect(readRouteLevelFilter({"filters[level][AT_OR_BELOW]": "WARN"})).toBe("WARN");
    });

    it("normalizes level query to AT_OR_BELOW comparator", () => {
        expect(normalizeRouteLevelFilter(
            {
                "filters[level][EQUALS]": "INFO",
                "filters[level][NOT_EQUALS]": "ERROR",
                foo: "bar",
            },
            "DEBUG",
        )).toEqual({
            "filters[level][AT_OR_BELOW]": "DEBUG",
            foo: "bar",
        });
    });

    it("accepts EQUALS and AT_OR_BELOW and rejects other comparators", () => {
        expect(hasUnsupportedRouteLevelComparator({"filters[level][EQUALS]": "INFO"})).toBe(false);
        expect(hasUnsupportedRouteLevelComparator({"filters[level][AT_OR_BELOW]": "INFO"})).toBe(false);
        expect(hasUnsupportedRouteLevelComparator({"filters[level][NOT_EQUALS]": "INFO"})).toBe(true);
    });
});
