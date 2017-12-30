package org.personal.sample;

import org.personal.reader.CSVFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Used to demonstrate the usage of the {@link CSVFileReader} class.
 *
 * @author Gabriel Padurean
 */
@Component
public class AccountReaderSample {
    private static final Logger LOG = LoggerFactory.getLogger(AccountReaderSample.class);

    @Value("${fileName}")
    private String fileName;

    @Value("${numberOfThreads}")
    private Integer numberOfThreads;


    /**
     * Method runs when the application has started and creates a {@link CSVFileReader} instance
     * that allows to read a CSV file and automatically create {@link Account} instances.
     * The column names from the CSV file should map to the field names from the {@link Account} class.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        CSVFileReader<Account> csvFileReader = new CSVFileReader<Account>(fileName, numberOfThreads) {
            @Override
            public void onSuccess(Account account) {
                LOG.info("Successfully read: {}", account);
            }

            @Override
            public void onFail(Exception e) {
                LOG.error("Unsuccessfully read: ", e);
            }
        };

        /**
         * Once the instance is created we can start the processing.
         */
        csvFileReader.process();
    }
}