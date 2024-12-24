package org.zwobble.toml;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.zwobble.toml.errors.TomlParseError;
import org.zwobble.toml.values.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TomlParserTomlTestTests {
    @TestFactory
    public Stream<DynamicTest> tests() throws IOException {
        var tomlTestPath = Path.of("toml-test/tests");
        var testTomlFilePaths = Files.readAllLines(tomlTestPath.resolve("files-toml-1.0.0"));

        return testTomlFilePaths.stream()
            .filter(testTomlFilePath -> testTomlFilePath.endsWith(".toml"))
            .filter(testTomlFilePath -> testTomlFilePath.startsWith("valid"))
            .map(testTomlFilePath -> {
                return DynamicTest.dynamicTest(testTomlFilePath, () -> {
                    var inputTomlPath = tomlTestPath.resolve(testTomlFilePath);

                    var testType = testTomlFilePath.substring(0, testTomlFilePath.indexOf('/'));

                    switch (testType) {
                        case "valid": {
                            var tomlValue = TomlParser.parseFile(inputTomlPath);

                            var outputJsonPath = replaceExtension(inputTomlPath, "json");
                            var jsonValue = tomlValueToJsonValue(tomlValue);

                            var expectedJsonText = Files.readString(outputJsonPath);
                            var expectedJsonValue = JsonParser.parseString(expectedJsonText);
                            assertEquals(expectedJsonValue, jsonValue);

                            break;
                        }
                        case "invalid": {
                            assertThrows(
                                TomlParseError.class,
                                () -> TomlParser.parseFile(inputTomlPath)
                            );
                            break;
                        }
                        default: {
                            throw new IllegalArgumentException("Unexpected test type: " + testType);
                        }
                    }
                });
            });
    }

    private static Path replaceExtension(Path path, String newExtension) {
        var fileName = path.getFileName().toString();
        var fileNameNoExtension = fileName.substring(0, fileName.lastIndexOf('.'));
        var newFileName = fileNameNoExtension + "." + newExtension;
        return path
            .getParent()
            .resolve(newFileName);
    }

    private JsonElement tomlValueToJsonValue(TomlValue tomlValue) {
        return switch (tomlValue) {
            case TomlArray tomlArray -> {
                var jsonArray = new JsonArray();
                for (var tomlElement : tomlArray) {
                    jsonArray.add(tomlValueToJsonValue(tomlElement));
                }
                yield jsonArray;
            }

            case TomlBool tomlBool -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "bool");
                jsonObject.addProperty("value", tomlBool.value() ? "true" : "false");
                yield jsonObject;
            }

            case TomlInt tomlInt -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "integer");
                jsonObject.addProperty("value", Long.toString(tomlInt.value()));
                yield jsonObject;
            }

            case TomlString tomlString -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "string");
                jsonObject.addProperty("value", tomlString.value());
                yield jsonObject;
            }

            case TomlTable tomlTable -> {
                var jsonObject = new JsonObject();
                for (var pair : tomlTable) {
                    jsonObject.add(pair.key(), tomlValueToJsonValue(pair.value()));
                }
                yield jsonObject;
            }
        };
    }
}
