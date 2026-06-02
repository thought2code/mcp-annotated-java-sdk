package com.github.thought2code.mcp.annotated.enums;

/**
 * Maps common Java types to JSON Schema primitive type strings.
 *
 * <p>Used when reflecting tool parameter and output schemas. Unknown types default to {@code
 * string} via {@link #getJsonSchemaType(Class)}.
 *
 * @author codeboyzhou
 */
public enum JavaTypeToJsonSchemaMapper {

  /** Java {@code String} is mapped to JSON schema data type {@code string}. */
  STRING(String.class, "string"),

  /** Java {@code Number} is mapped to JSON schema data type {@code number}. */
  NUMBER(Number.class, "number"),

  /** Java {@code long} is mapped to JSON schema data type {@code number}. */
  LONG(long.class, "number"),

  /** Java {@code Long} is mapped to JSON schema data type {@code number}. */
  LONG_CLASS(Long.class, "number"),

  /** Java {@code float} is mapped to JSON schema data type {@code number}. */
  FLOAT(float.class, "number"),

  /** Java {@code Float} is mapped to JSON schema data type {@code number}. */
  FLOAT_CLASS(Float.class, "number"),

  /** Java {@code double} is mapped to JSON schema data type {@code number}. */
  DOUBLE(double.class, "number"),

  /** Java {@code Double} is mapped to JSON schema data type {@code number}. */
  DOUBLE_CLASS(Double.class, "number"),

  /** Java {@code int} is mapped to JSON schema data type {@code integer}. */
  INT(int.class, "integer"),

  /** Java {@code Integer} is mapped to JSON schema data type {@code integer}. */
  INTEGER(Integer.class, "integer"),

  /** Java {@code boolean} is mapped to JSON schema data type {@code boolean}. */
  BOOLEAN(boolean.class, "boolean"),

  /** Java {@code Boolean} is mapped to JSON schema data type {@code boolean}. */
  BOOLEAN_CLASS(Boolean.class, "boolean"),

  /** Java {@code Object} is mapped to JSON schema data type {@code object}. */
  OBJECT(Object.class, "object"),
  ;

  /** The Java class that is mapped to the JSON schema data type. */
  private final Class<?> javaType;

  /** The JSON schema data type. */
  private final String jsonSchemaType;

  /**
   * Creates a new instance of {@code JsonSchemaDataType} with the specified Java class and JSON
   * schema data type.
   *
   * @param javaType the Java class that is mapped to the JSON schema data type
   * @param jsonSchemaType the JSON schema data type
   */
  JavaTypeToJsonSchemaMapper(Class<?> javaType, String jsonSchemaType) {
    this.javaType = javaType;
    this.jsonSchemaType = jsonSchemaType;
  }

  /**
   * Returns the JSON schema data type.
   *
   * @return the JSON schema data type
   */
  public String getJsonSchemaType() {
    return jsonSchemaType;
  }

  /**
   * Returns the JSON schema data type for the specified Java class. If the Java class is not
   * supported or mapped to a JSON schema data type, {@code "string"} will be returned.
   *
   * @param javaType the Java class
   * @return the JSON schema data type
   */
  public static String getJsonSchemaType(Class<?> javaType) {
    JavaTypeToJsonSchemaMapper[] values = values();
    for (JavaTypeToJsonSchemaMapper mapper : values) {
      if (mapper.javaType.equals(javaType)) {
        return mapper.jsonSchemaType;
      }
    }
    return STRING.jsonSchemaType;
  }
}
