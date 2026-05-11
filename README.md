# LINFO2132 Compiler Project

Java 21 + Gradle project implementing a compiler for the LINFO2132 language.

## Build
./gradlew build

## Run lexer
./gradlew run --args="-lexer file.lang"

## Run parser
./gradlew run --args="-parser file.lang"

## Run semantic analysis
./gradlew run --args="-semantic file.lang"

## Generate bytecode
./gradlew run --args="file.lang -o test.class"

## Run generated class
java -cp . test

## Run tests
./gradlew test

## Run a specific test class

./gradlew test --tests TestLexer
./gradlew test --tests TestParser
./gradlew test --tests TestSemantic_Analysis
./gradlew test --tests TestCodeGenerator

## Implemented features
- Lexer
- Recursive-descent parser
- Semantic analysis
- JVM bytecode generation with ASM
- Arrays
- Collections
- Functions and scopes
- Built-in functions