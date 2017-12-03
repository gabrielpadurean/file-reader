package org.personal.sample;

import org.personal.reader.CSVFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
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
     * Sample method on how to use the {@link CSVFileReader} class.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        CSVFileReader csvFileReader = new CSVFileReader<Account>(fileName, numberOfThreads) {
            @Override
            public void onSuccess(Account account) {
                LOG.info("Successfully read: {}", account);
            }

            @Override
            public void onFail(Exception e) {
                LOG.error("Unsuccessfully read: ", e);
            }
        };

        csvFileReader.process();
    }
}