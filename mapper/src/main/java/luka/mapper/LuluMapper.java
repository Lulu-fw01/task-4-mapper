package luka.mapper;

import ru.hse.homework4.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

public class LuluMapper implements Mapper {
    /**
     * Читает сохранённый экземпляр класса {@code clazz} из строки {@code input}
     * и возвращает восстановленный экземпляр класса {@code clazz}.
     * <p>
     * Пример вызова:
     *
     * <pre>
     *
     * String input = """
     *
     * {"comment":"Хорошая работа","resolved":false}""";
     *
     * ReviewComment reviewComment =
     * mapper.readFromString(ReviewComment.class, input);
     *
     * System.out.println(reviewComment);
     * </pre>
     *
     * @param clazz класс, сохранённый экземпляр которого находится в {@code input}
     * @param input строковое представление сохранённого экземпляра класса {@code
     *              clazz}
     * @return восстановленный экземпляр {@code clazz}
     */
    @Override
    public <T> T readFromString(Class<T> clazz, String input) {
        return null;
    }

    /**
     * Читает объект класса {@code clazz} из {@code InputStream}'а
     * и возвращает восстановленный экземпляр класса {@code clazz}.
     * <p>
     * Данный метод закрывает {@code inputStream}.
     * <p>
     * Пример вызова:
     *
     * <pre>
     *
     * String input = """
     *
     * {"comment":"Хорошая работа","resolved":false}""";
     *
     * ReviewComment reviewComment = mapper.read(ReviewComment.class,
     *
     * new
     * ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
     *
     * System.out.println(reviewComment);
     * </pre>
     *
     * @param clazz       класс, сохранённый экземпляр которого находится в {@code
     *                    inputStream}
     * @param inputStream поток ввода, содержащий строку в {@link
     *                    StandardCharsets#UTF_8} кодировке
     *                    5* @param <T> возвращаемый тип метода
     * @return восстановленный экземпляр класса {@code clazz}
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public <T> T read(Class<T> clazz, InputStream inputStream) throws IOException {
        return null;
    }

    /**
     * Читает сохранённое представление экземпляра класса {@code clazz} из {@code
     * File}'а
     * и возвращает восстановленный экземпляр класса {@code clazz}.
     * <p>
     * Пример вызова:
     *
     * <pre>
     *
     * ReviewComment reviewComment = mapper.read(ReviewComment.class, new
     * File("/tmp/review"));
     *
     * System.out.println(reviewComment);
     * </pre>
     *
     * @param clazz класс, сохранённый экземпляр которого находится в файле
     * @param file  файл, содержимое которого - строковое представление экземпляра
     *              {@code clazz}
     *              в {@link StandardCharsets#UTF_8} кодировке
     * @return восстановленный экземпляр {@code clazz}
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public <T> T read(Class<T> clazz, File file) throws IOException {
        return null;
    }

    /**
     * Сохраняет {@code object} в строку
     * <p>
     * Пример вызова:
     *
     * <pre>
     *
     * ReviewComment reviewComment = new ReviewComment();
     *
     * reviewComment.setComment("Хорошая работа");
     *
     * reviewComment.setResolved(false);
     *
     *
     * String string = mapper.writeToString(reviewComment);
     *
     * System.out.println(string);
     * </pre>
     *
     * @param object объект для сохранения
     * @return строковое представление объекта в выбранном формате
     */
    @Override
    public String writeToString(Object object) {
        if (object == null || !object.getClass().isAnnotationPresent(Exported.class)) {
            return "";
        }

        StringBuilder result = new StringBuilder("{");

        var objClass = object.getClass();
        var objFields = objClass.getFields();
        for (var field : objFields) {
            if (!field.isAnnotationPresent(Ignored.class)) {
                var jsonString = fieldToJsonString(object, field);
                result.append(String.format("%s,", jsonString));
            }
        }

        result.append("}");
        return result.toString();
    }

    String fieldToJsonString(Object object, Field field) {
        StringBuilder result = new StringBuilder("");

        String value = "";

        try {
            // Primitive, wrapper or String.
            if (field.getType() == String.class || field.getType().isPrimitive() || isWrapper(field.getType())) {
                field.setAccessible(true);
                value = field.get(object).toString();
                // Check property name.
                result.append(String.format("%s: \"%s\"", fieldNameToJsonString(field), value));
            } else {
                // Collection, another big class, datas.
            }
        } catch (IllegalAccessException ignored) {
        }

        return result.toString();
    }

    /**
     * Convert field name to jsn string.
     *
     * */
    static String fieldNameToJsonString(Field field) {
        StringBuilder result = new StringBuilder("\"");

        if (field.isAnnotationPresent(PropertyName.class)) {
            var annotation = field.getAnnotation(PropertyName.class);
            result.append(String.format("%s\"", annotation.value()));
        } else {
            result.append(String.format("%s\"", field.getName()));
        }
        return result.toString();
    }

    /*
    String collectionToString() {

    }

    String classToString(Object object) {

    }
    */

    /**
     * This Function converts date to json string.
     *
     * @param object - {@link LocalDate}, {@link LocalTime} or {@link LocalDateTime} object.
     * */
    static String dateToJsonString(Object object, Field field) {
        StringBuilder result = new StringBuilder("");

        field.setAccessible(true);
        try {
            var value = field.get(object);
            StringBuilder stringDate = new StringBuilder("");
            if (field.isAnnotationPresent(DateFormat.class)) {
                var annotation = field.getAnnotation(DateFormat.class);
                var formatter = DateTimeFormatter.ofPattern(annotation.value());

                if (LocalDate.class.equals(value.getClass())) {
                    var date = (LocalDate) value;
                    stringDate.append(date.format(formatter));
                } else if (LocalTime.class.equals(value.getClass())) {
                    var date = (LocalTime) value;
                    stringDate.append(date.format(formatter));
                } else if (LocalDateTime.class.equals(value.getClass())) {
                    var date = (LocalDateTime) value;
                    stringDate.append(date.format(formatter));
                }
            } else {
                stringDate.append(value.toString());
            }

            result.append(String.format("%s: \"%s\"", fieldNameToJsonString(field), stringDate.toString()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    private static boolean isWrapper(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        return clazz == Boolean.class || clazz == Character.class ||
                clazz == Byte.class || clazz == Short.class ||
                clazz == Integer.class || clazz == Long.class ||
                clazz == Float.class || clazz == Double.class;
    }

    /**
     * Сохраняет {@code object} в {@link OutputStream}.
     * <p>
     * 6* То есть после вызова этого метода в {@link OutputStream} должны оказаться
     * байты, соответствующие строковому
     * представлению {@code object}'а в кодировке {@link
     * StandardCharsets#UTF_8}
     * <p>
     * Данный метод закрывает {@code outputStream}
     * <p>
     * Пример вызова:
     *
     * <pre>
     *
     * ReviewComment reviewComment = new ReviewComment();
     *
     * reviewComment.setComment("Хорошая работа");
     *
     * reviewComment.setResolved(false);
     *
     *
     * mapper.write(reviewComment, new FileOutputStream("/tmp/review"));
     * </pre>
     *
     * @param object       объект для сохранения
     * @param outputStream
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public void write(Object object, OutputStream outputStream) throws IOException {

    }

    /**
     * Сохраняет {@code object} в {@link File}.
     * <p>
     * То есть после вызова этого метода в {@link File} должны оказаться байты,
     * соответствующие строковому
     * представлению {@code object}'а в кодировке {@link
     * StandardCharsets#UTF_8}
     * <p>
     * Данный метод закрывает {@code outputStream}
     * <p>
     * Пример вызова:
     *
     * <pre>
     *
     * ReviewComment reviewComment = new ReviewComment();
     *
     * reviewComment.setComment("Хорошая работа");
     *
     * reviewComment.setResolved(false);
     *
     *
     * mapper.write(reviewComment, new File("/tmp/review"));
     * </pre>
     *
     * @param object объект для сохранения
     * @param file
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public void write(Object object, File file) throws IOException {

    }
}
