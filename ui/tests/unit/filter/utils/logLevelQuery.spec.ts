import {describe, expect, it} from "vitest"
import {
    hasUnsupportedRouteLevelComparator,
    normalizeRouteLevelFilter,
    readRouteLevelFilter,
} from "@kestra-io/design-system"

describe("log level query helpers", () => {
    it("reads GREATER_THAN_OR_EQUAL_TO level from route query", () => {
        expect(readRouteLevelFilter({"filters[level][GREATER_THAN_OR_EQUAL_TO]": "WARN"})).toBe("WARN")
    })

    it("normalizes level query to GREATER_THAN_OR_EQUAL_TO comparator", () => {
        expect(normalizeRouteLevelFilter(
            {
                "filters[level][EQUALS]": "INFO",
                "filters[level][NOT_EQUALS]": "ERROR",
                foo: "bar",
            },
            "DEBUG",
        )).toEqual({
            "filters[level][GREATER_THAN_OR_EQUAL_TO]": "DEBUG",
            foo: "bar",
        })
    })

    it("accepts EQUALS and LESS_THAN_OR_EQUAL_TO and rejects other comparators", () => {
        expect(hasUnsupportedRouteLevelComparator({"filters[level][EQUALS]": "INFO"})).toBe(false)
        expect(hasUnsupportedRouteLevelComparator({"filters[level][GREATER_THAN_OR_EQUAL_TO]": "INFO"})).toBe(false)
        expect(hasUnsupportedRouteLevelComparator({"filters[level][NOT_EQUALS]": "INFO"})).toBe(true)
    })
})
