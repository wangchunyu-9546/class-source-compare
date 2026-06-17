# CompareV

CompareV is a JavaFX desktop tool for comparing field `.class` files with local Java source code.

## First-stage scope

- Select a field class directory.
- Select a local Java source directory.
- Drag field `.class` files, `.jar` files, or directories into the class input.
- Drag local `.java` files or source directories into the source input.
- Skip anonymous inner classes such as `Outer$1.class` by default, with an optional UI switch to include them.
- Compare class existence and method signatures.
- Optionally decompile field classes and compare method implementation similarity.
- Show incompatibilities in the desktop UI.
- Export HTML and Excel reports.

## Run

```bash
mvn exec:java
```

## Validate

```bash
mvn test
```

## Package

```bash
mvn clean package
```

The Windows executable is generated at:

```text
target/release/CompareV.exe
```

The executable requires Java 8 on the target machine.
