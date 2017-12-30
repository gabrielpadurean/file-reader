# Name
File Reader

# Description
Project containing the main class for the processing of CSV files using a multi-thread approach and a sample usage.  
The file is read on a single thread, since reading a file multi-threaded makes no sense, but the handling of each column  
and conversion to a domain model instance is done on a separate thread from a thread pool (since these operations are CPU bound).  

Main class is `CSVFileReader`.  
Check class `AccountReaderSample` for a sample usage.  

# Usage
Configuration properties can be found in the `application.properties` file.  

CSV file structure:
```csv
id,firstName,lastName,age
"1233","John","Doe","35"
```

Domain model class:
```java
public class Account {
    private Long id;
    private String firstName;
    private String lastName;
    private Integer age;

    ...
}
```

Usage of the file reader:
```java
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

csvFileReader.process();
```