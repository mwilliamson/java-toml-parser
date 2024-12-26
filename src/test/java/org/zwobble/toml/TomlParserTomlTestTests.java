package org.zwobble.toml;

import com.google.gson.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.zwobble.toml.errors.TomlParseError;
import org.zwobble.toml.values.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TomlParserTomlTestTests {
    // Potentially missing tests in toml-test:
    // * Multiple newlines at start of multiline string

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

                            var expectedJsonValue = readTomlTestJson(outputJsonPath);
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

    private static JsonElement readTomlTestJson(Path outputJsonPath) throws IOException {
        var jsonText = Files.readString(outputJsonPath);
        var jsonValue = JsonParser.parseString(jsonText);
        return normalizeTomlTestJson(jsonValue);
    }

    private static JsonElement normalizeTomlTestJson(JsonElement value) {
        if (value.isJsonArray()) {
            var normalizedArray = new JsonArray();
            for (var element : value.getAsJsonArray()) {
                normalizedArray.add(normalizeTomlTestJson(element));
            }
            return normalizedArray;
        } else if (value.isJsonObject()) {
            var jsonObject = value.getAsJsonObject();
            if (Objects.equals(jsonObject.get("type"), new JsonPrimitive("float"))) {
                var floatValueString = jsonObject.get("value").getAsString();
                if (floatValueString.contains("inf") || floatValueString.contains("nan")) {
                    return value;
                }
                var floatValue = Double.parseDouble(floatValueString);
                var normalizedObject = new JsonObject();
                normalizedObject.addProperty("type", "float");
                normalizedObject.addProperty("value", floatToString(floatValue));
                return normalizedObject;
            } else if (Objects.equals(jsonObject.get("type"), new JsonPrimitive("datetime"))) {
                var offsetDateTimeValue = OffsetDateTime.parse(jsonObject.get("value").getAsString());
                var normalizedObject = new JsonObject();
                normalizedObject.addProperty("type", "datetime");
                normalizedObject.addProperty("value", offsetDateTimeValue.toString());
                return normalizedObject;
            } else if (Objects.equals(jsonObject.get("type"), new JsonPrimitive("datetime-local"))) {
                var localDateTimeValue = LocalDateTime.parse(jsonObject.get("value").getAsString());
                var normalizedObject = new JsonObject();
                normalizedObject.addProperty("type", "datetime-local");
                normalizedObject.addProperty("value", localDateTimeValue.toString());
                return normalizedObject;
            } else if (Objects.equals(jsonObject.get("type"), new JsonPrimitive("time-local"))) {
                var localTimeValue = LocalTime.parse(jsonObject.get("value").getAsString());
                var normalizedObject = new JsonObject();
                normalizedObject.addProperty("type", "time-local");
                normalizedObject.addProperty("value", localTimeValue.toString());
                return normalizedObject;
            } else {
                var normalizedObject = new JsonObject();
                for (var property : jsonObject.entrySet()) {
                    normalizedObject.add(
                        property.getKey(),
                        normalizeTomlTestJson(property.getValue())
                    );
                }
                return normalizedObject;
            }
        } else {
            return value;
        }
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

            case TomlFloat tomlFloat -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "float");
                jsonObject.addProperty("value", floatToString(tomlFloat.value()));
                yield jsonObject;
            }

            case TomlInt tomlInt -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "integer");
                jsonObject.addProperty("value", Long.toString(tomlInt.value()));
                yield jsonObject;
            }

            case TomlLocalDate tomlLocalDate -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "date-local");
                jsonObject.addProperty("value", tomlLocalDate.value().toString());
                yield jsonObject;
            }

            case TomlLocalDateTime tomlLocalDateTime -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "datetime-local");
                jsonObject.addProperty("value", tomlLocalDateTime.value().toString());
                yield jsonObject;
            }

            case TomlLocalTime tomlLocalTime -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "time-local");
                jsonObject.addProperty("value", tomlLocalTime.value().toString());
                yield jsonObject;
            }

            case TomlOffsetDateTime tomlOffsetDateTime -> {
                var jsonObject = new JsonObject();
                jsonObject.addProperty("type", "datetime");
                jsonObject.addProperty("value", tomlOffsetDateTime.value().toString());
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

    private static String floatToString(double value) {
        var decimalFormat = new DecimalFormat("0");

        if (Double.isNaN(value)) {
            return "nan";
        }

        if (Double.isInfinite(value)) {
            if (value > 0) {
                return "inf";
            } else {
                return "-inf";
            }
        }

        decimalFormat.setMaximumFractionDigits(Integer.MAX_VALUE);
        return decimalFormat.format(value);
    }
}