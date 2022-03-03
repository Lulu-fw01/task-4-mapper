package luka.mapper.deconverter;

import luka.mapper.converter.Converter;
import ru.hse.homework4.DateFormat;
import ru.hse.homework4.Exported;
import ru.hse.homework4.Ignored;
import ru.hse.homework4.PropertyName;

import java.lang.reflect.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Deconverter {

    public static <T> T getObjectFromString(Class<T> clazz, String input) {
        T clearObject;
        var constructors = clazz.getConstructors();
        if (checkPublicClearConstructor(constructors)) {
            try {
                var constructor = clazz.getConstructor();
                // Crate clear object.
                clearObject = constructor.newInstance();
                if (!clearObject.getClass().isAnnotationPresent(Exported.class)) {
                    // TODO throw smth.
                    return null;
                }
                setFields(clearObject, input);
                return clearObject;
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static void setFields(Object object, String fieldsString) {
        if (!object.getClass().isAnnotationPresent(Exported.class)) {
            return;
        }
        if (fieldsString.charAt(0) != '{' || fieldsString.charAt(fieldsString.length() - 1) != '}') {
            // TODO throw smth.
        }

        var content = getStringWithoutBorders(fieldsString, '{', '}');

        var clazz = object.getClass();

        // Filter all class fields.
        var classFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(elem -> !elem.isSynthetic()
                        && !elem.isAnnotationPresent(Ignored.class)
                        && !Modifier.isStatic(elem.getModifiers()))
                .toList();

        var strFields = content.split(",");
        for (var strField : strFields) {
            // Get array where 0 elem is field name
            // and 1 elem it's value in string format.
            var nameVal = strField.split(": ");
            var fieldName = new StringBuilder(nameVal[0]);
            // Remove " and ".
            fieldName.deleteCharAt(0);
            fieldName.deleteCharAt(fieldName.length() - 1);
            // Find field with this name or propertyName annotation.
            var optField = classFields
                    .stream()
                    .filter(elem ->
                            elem.getName().equals(fieldName.toString())
                                    || (elem.isAnnotationPresent(PropertyName.class)
                                    && elem.getAnnotation(PropertyName.class).value().equals(fieldName.toString())))
                    .findFirst();
            Field field;
            if (optField.isPresent()) {
                field = optField.get();
            } else {
                continue;
            }
            // Set field value.
            try {
                field.set(object, getValueFromString(field, field.getType(), nameVal[1]));
            } catch (IllegalAccessException e) {

            }
        }
    }

    /**
     * Method which returns value of field.
     *
     * */
    public static Object getValueFromString(Field field, Class<?> type, String value) {
        field.setAccessible(true);

        if (String.class.equals(type) || type.isPrimitive() || Converter.isWrapper(type)) {
            return getEasyValue(type, value);
        } else if (LocalDate.class.equals(type) || LocalTime.class.equals(type) || LocalDateTime.class.equals(type)) {
            return getDateValue(field, type, value);
        } else if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
            return getCollectionValue(field, value);
        } else if (type.isEnum()) {
            return getEnumValue(type, value);
        } else {
            return getObjectFromString(type, value);
        }
    }


    /**
     * Method for getting values of List or Set from String.
     *
     * @param field field.
     * @param value value in string format.
     */
    public static Collection<?> getCollectionValue(Field field, String value) {
        // TODO check first and last symbol.
        var type = field.getType();
        var elemType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
        // TODO exported checking.
        ArrayList<Object> result = new ArrayList<>();

        var elementsString = getStringWithoutBorders(value, '[', ']');
        var elements = elementsString.split(",");

        for (var element : elements) {
            var elem = getValueFromString(field, Integer.class, element);
            result.add(elem);
        }

        if (Set.class.isAssignableFrom(type)) {
            return Set.copyOf(result);
        } else if (List.class.isAssignableFrom(type)) {
            return result.stream().toList();
        }
        return null;
    }

    /**
     * Method for getting values of LocalDate, LocalTime or LocalDateTime from string.
     *
     * @param field field.
     * @param type  type of field.
     * @param value value in string format.
     */
    public static Object getDateValue(Field field, Class<?> type, String value) {
        var content = getStringWithoutBorders(value, '\"', '\"');

        if (field.isAnnotationPresent(DateFormat.class)) {
            var annotation = field.getAnnotation(DateFormat.class);

            var formatter = DateTimeFormatter.ofPattern(annotation.value());
            if (LocalDate.class.equals(type)) {
                return LocalDate.parse(content, formatter);
            } else if (LocalTime.class.equals(type)) {
                return LocalTime.parse(content, formatter);
            } else if (LocalDateTime.class.equals(type)) {
                return LocalDateTime.parse(content, formatter);
            }
        } else {
            if (LocalDate.class.equals(type)) {
                return LocalDate.parse(content);
            } else if (LocalTime.class.equals(type)) {
                return LocalTime.parse(content);
            } else if (LocalDateTime.class.equals(type)) {
                return LocalDateTime.parse(content);
            }
        }
        return null;
    }

    /**
     * Method for getting enum values from string.
     *
     * @param type  field type.
     * @param value value in string format.
     */
    public static Enum<?> getEnumValue(Class<?> type, String value) {

        var content = getStringWithoutBorders(value, '\"', '\"');

        var constants = type.getEnumConstants();
        // Find enum constant with such value.
        var constant = Arrays
                .stream(constants)
                .filter(elem -> elem.toString().equals(content)).findFirst();
        return (Enum<?>) constant.orElse(null);
    }

    /**
     * Method for getting values of string, primitive and wrapper from string.
     *
     * @param type  field value
     * @param value value in string format.
     */
    public static Object getEasyValue(Class<?> type, String value) {
        var content = getStringWithoutBorders(value, '\"', '\"');
        return toObject(type, content);
    }

    /**
     * Convert string into primitives, wrappers.
     *
     * @param clazz output object type.
     * @param value value in string format.
     */
    public static Object toObject(Class<?> clazz, String value) {
        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            return Boolean.parseBoolean(value);
        }
        if (Byte.class.equals(clazz) || byte.class.equals(clazz)) {
            return Byte.parseByte(value);
        }
        if (Short.class.equals(clazz) || short.class.equals(clazz)) {
            return Short.parseShort(value);
        }
        if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            return Integer.parseInt(value);
        }
        if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            return Long.parseLong(value);
        }
        if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            return Float.parseFloat(value);
        }
        if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            return Double.parseDouble(value);
        }
        return value;
    }

    public static String getStringWithoutBorders(String str, Character leftBorder, Character rightBorder) {
        var left = str.indexOf(leftBorder);
        var right = str.lastIndexOf(rightBorder);
        return str.substring(left + 1, right);
    }

    /**
     * Check if class has standard constructor without parameters.
     *
     * */
    public static boolean checkPublicClearConstructor(Constructor<?>[] constructors) {
        for (var constructor : constructors) {
            if (constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
                return true;
            }
        }

        return false;
    }
}
