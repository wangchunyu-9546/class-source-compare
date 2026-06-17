package com.comparev.scanner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileScannerTest {
    @Test
    void detectsAnonymousInnerClassNames() {
        assertTrue(ClassFileScanner.isAnonymousInnerClassName("demo.FileDirectoryServiceImpl$1"));
        assertTrue(ClassFileScanner.isAnonymousInnerClassName("demo.FileDirectoryServiceImpl$2"));
    }

    @Test
    void doesNotTreatNamedInnerClassesAsAnonymous() {
        assertFalse(ClassFileScanner.isAnonymousInnerClassName("demo.FileDirectoryServiceImpl$Worker"));
        assertFalse(ClassFileScanner.isAnonymousInnerClassName("demo.FileDirectoryServiceImpl"));
    }
}
