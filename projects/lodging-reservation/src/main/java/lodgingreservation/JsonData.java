package lodgingreservation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Base64;
import java.nio.file.StandardCopyOption;

/** Package-private syntax/type support. Domain validation remains in domain constructors. */
final class JsonData {
    private JsonData() { }

    static JsonObject object(String text) {
        if (text == null) throw new IllegalArgumentException("JSON is required.");
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement value = value(reader, 0);
            if (!value.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IllegalArgumentException("Expected one JSON object.");
            return value.getAsJsonObject();
        } catch (IOException | IllegalStateException ex) {
            throw new IllegalArgumentException("Invalid JSON.", ex);
        }
    }

    private static JsonElement value(JsonReader r, int depth) throws IOException {
        if (depth > 64) throw new IllegalArgumentException("JSON nesting is too deep.");
        switch (r.peek()) {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject();
                r.beginObject();
                while (r.hasNext()) {
                    String key = r.nextName();
                    if (object.has(key)) throw new IllegalArgumentException("Duplicate JSON member: " + key);
                    object.add(key, value(r, depth + 1));
                }
                r.endObject();
                return object;
            case BEGIN_ARRAY:
                JsonArray array = new JsonArray();
                r.beginArray();
                while (r.hasNext()) array.add(value(r, depth + 1));
                r.endArray();
                return array;
            case STRING: return new JsonPrimitive(r.nextString());
            case NUMBER: return new JsonPrimitive(new BigDecimal(r.nextString()));
            case BOOLEAN: return new JsonPrimitive(r.nextBoolean());
            case NULL: r.nextNull(); return JsonNull.INSTANCE;
            default: throw new IllegalArgumentException("Invalid JSON value.");
        }
    }

    static JsonObject read(Path file) {
        if (file == null) throw new IllegalArgumentException("File is required.");
        try { return object(Files.readString(file, StandardCharsets.UTF_8)); }
        catch (IOException ex) { throw new java.io.UncheckedIOException("Unable to read " + file, ex); }
    }

    static JsonElement required(JsonObject data, String field) {
        if (data == null || !data.has(field) || data.get(field).isJsonNull())
            throw new IllegalArgumentException("Required JSON field: " + field);
        return data.get(field);
    }

    static String string(JsonObject data, String field) {
        JsonElement v = required(data, field);
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException(field + " must be a string.");
        return v.getAsString();
    }

    static int integer(JsonObject data, String field) {
        try { return number(data, field).intValueExact(); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException(field + " must be a 32-bit integer.", ex); }
    }

    private static BigDecimal number(JsonObject data, String field) {
        JsonElement v = required(data, field);
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException(field + " must be a number.");
        return v.getAsBigDecimal();
    }

    static float money(JsonObject data, String field) {
        BigDecimal n = number(data, field);
        float value = n.floatValue();
        if (n.signum() < 0 || !Float.isFinite(value) || (n.signum() != 0 && value == 0))
            throw new IllegalArgumentException(field + " must be a representable nonnegative float.");
        return value;
    }

    static boolean bool(JsonObject data, String field) {
        JsonElement v = required(data, field);
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException(field + " must be a boolean.");
        return v.getAsBoolean();
    }

    static Address address(JsonObject data, String field, boolean nullable) {
        if (data != null && data.has(field) && data.get(field).isJsonNull() && nullable) return null;
        JsonElement v = required(data, field);
        if (!v.isJsonObject()) throw new IllegalArgumentException(field + " must be an address object.");
        return new Address(v.getAsJsonObject());
    }

    static Date date(JsonObject data, String field) {
        try {
            String text = string(data, field);
            if (!text.endsWith("Z")) throw new IllegalArgumentException(field + " must use UTC Z.");
            return Date.from(Instant.parse(text));
        } catch (java.time.DateTimeException | ArithmeticException ex) {
            throw new IllegalArgumentException(field + " must be a valid UTC instant.", ex);
        }
    }

    static void schema(JsonObject data) {
        if (integer(data, "schemaVersion") != 1) throw new IllegalArgumentException("Unsupported schema version.");
    }

    static void type(JsonObject data, String expected) {
        if (!expected.equals(string(data, "type"))) throw new IllegalArgumentException("Expected " + expected + " type.");
    }

    static List<String> strings(JsonObject data, String field) {
        JsonElement v = required(data, field);
        if (!v.isJsonArray()) throw new IllegalArgumentException(field + " must be an array.");
        List<String> result = new ArrayList<>();
        for (JsonElement element : v.getAsJsonArray()) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new IllegalArgumentException(field + " must contain filenames.");
            result.add(element.getAsString());
        }
        return result;
    }

    static String safeName(String value) {
        if (value == null) throw new IllegalArgumentException("Identifier is required.");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static void write(Path file, String text) {
        if (file == null || text == null) throw new IllegalArgumentException("File and text are required.");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent == null) throw new IllegalArgumentException("File must have a parent directory.");
            Files.createDirectories(parent);
            Path temp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temp, text, StandardCharsets.UTF_8);
                try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temp); }
        } catch (IOException ex) { throw new java.io.UncheckedIOException("Unable to write " + file, ex); }
    }

    static Path child(Path directory, String name) {
        if (name.isBlank() || name.contains("/") || name.contains("\\") || name.contains(":"))
            throw new IllegalArgumentException("Reservation filename must be a simple relative name.");
        try {
            Path root = directory.toRealPath();
            Path file = root.resolve(name).toRealPath();
            if (!file.startsWith(root) || !Files.isRegularFile(file))
                throw new IllegalArgumentException("Reservation file must stay in the storage directory.");
            return file;
        } catch (IOException ex) { throw new java.io.UncheckedIOException(ex); }
    }
}
