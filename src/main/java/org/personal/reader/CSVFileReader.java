package org.personal.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main class for the processing of CSV files with a multi-thread approach.
 * The file is read on a single thread, since reading a file multi-threaded makes no sense,
 * but the handling of each column and conversion to a domain model instance is done on a separate
 * thread from a thread pool (since these operations are more CPU bound).
 *
 * @author Gabriel Padurean
 */
public abstract class CSVFileReader<T> {
    private static final String UTF_8_BOM = "\uFEFF";
    private static final String SETTER = "set";

    private Class<T> clazz;
    private Map<Integer, Method> methods = new HashMap<>();

    private String fileName;
    private ExecutorService executorService;


    public CSVFileReader(String fileName, int numberOfThreads) {
        this.fileName = fileName;
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    /**
     * Method for starting the processing of the csv file.
     * This method blocks until all threads used for processing the csv file are done.
     */
    public void process() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(fileName)));

            /**
             * First line contains the headers used to establish what column goes to what field.
             */
            String line = bufferedReader.readLine();

            prepareProcessing(line);

            while ((line = bufferedReader.readLine()) != null) {
                /**
                 * Processing of each line is done on a separate thread
                 * because that is the time consuming part.
                 */
                String lineToProcess = line;

                executorService.execute(() -> {
                    processLine(lineToProcess);
                });
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
        } catch (Exception e) {
            onFail(e);
        }
    }

    private void prepareProcessing(String line) throws NoSuchMethodException {
        /**
         * Process line header columns.
         */
        String[] elements = line.split(",");
        Map<String, Integer> columnPosition = new HashMap<>();
        for (int i = 0; i < elements.length; i++) {
            String elem = elements[i];

            if (elem.startsWith(UTF_8_BOM)) {
                columnPosition.put(elem.substring(1), i);
            } else {
                columnPosition.put(elem, i);
            }
        }

        /**
         * Process what fields can be set based on column headers.
         */
        this.clazz = ((Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
        for (Field field : this.clazz.getDeclaredFields()) {
            String fieldName = field.getName();
            String methodName = SETTER + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            Method method = this.clazz.getMethod(methodName, field.getType());

            methods.put(columnPosition.get(fieldName), method);
        }
    }

    private void processLine(String line) {
        String[] elements = line.split(",");

        try {
            T t = this.clazz.newInstance();

            for (int i = 0; i < elements.length; i++) {
                String value = elements[i];

                if (value.startsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                Method method = methods.get(i);

                if (method != null) {
                    Class methodParameterType = method.getParameterTypes()[0];

                    /**
                     * When calling method, convert parameter to proper data type otherwise won't work.
                     */
                    if (Long.class.equals(methodParameterType)) {
                        method.invoke(t, Long.parseLong(value));
                    } else {
                        if (Integer.class.equals(methodParameterType)) {
                            method.invoke(t, Integer.parseInt(value));
                        } else {
                            if (String.class.equals(methodParameterType)) {
                                method.invoke(t, value);
                            } else {
                                throw new IllegalArgumentException("This data type is not supported");
                            }
                        }
                    }
                }
            }

            onSuccess(t);
        } catch (Exception e) {
            onFail(e);
        }
    }

    /**
     * Should be implemented with actions for what to do in case of success.
     * Since this is executed on a thread from a thread pool configured
     * for the file reader it can contain also code for heavy-processing,
     * but the file reader should be initialized accordingly.
     *
     * @param t Instance representing a line from the file.
     */
    public abstract void onSuccess(T t);

    /**
     * Should be implemented with actions for what to do in case of failure.
     *
     * @param e Exception that occurred while processing a line from the file.
     */
    public abstract void onFail(Exception e);
}