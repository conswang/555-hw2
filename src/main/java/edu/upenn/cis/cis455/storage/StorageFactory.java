package edu.upenn.cis.cis455.storage;

public class StorageFactory {
    // Not really a factory?
    public static StorageInterface getDatabaseInstance(String directory) {
        return new Storage(directory);
    }
}
