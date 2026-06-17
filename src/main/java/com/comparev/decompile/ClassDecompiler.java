package com.comparev.decompile;

import com.comparev.model.ClassInfo;

import java.io.IOException;

public interface ClassDecompiler {
    DecompiledSource decompile(ClassInfo classInfo) throws IOException;
}
