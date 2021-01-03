package com.author.kurtkalwin.datastore.Interfaces;

import com.author.kurtkalwin.datastore.Exceptions.InvalidKeyException;
import com.author.kurtkalwin.datastore.Exceptions.JSONSizeLimitExceededException;
import com.author.kurtkalwin.datastore.Exceptions.LocalStorageSizeExceededException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * CRD (create, read and delete) Interface for LocalDataStore
 * @author kurtkalwin@gmail.com
 */
public interface KeyValueStore {

    /**
     * @return Storage path of local data store. (Currently used for unit testing purposes)
     */
    String getDataStorePath();

    /**
     * @param key   - Key for the data store. Must be capped to 32 chars
     * @param value - JSON Object (value). Value cannot exceed 16 KB
     * @throws InvalidKeyException               - Gets thrown when key is more than 32 chars or null
     * @throws JSONSizeLimitExceededException    - Gets thrown when JSON object exceeds 64kb
     * @throws LocalStorageSizeExceededException - Gets thrown when data store size exceeds 1 GB
     * @throws IOException - Gets thrown if File cannot be created due to File lock issues
     */
    void create(String key, JSONObject value) throws JSONSizeLimitExceededException, LocalStorageSizeExceededException, InvalidKeyException, IOException;

    /**
     * @param key        - Key for the data store. Must be capped to 32 chars
     * @param value      - JSON Object (value). Value cannot exceed 16 KB
     * @param timeToLive - Time to Live represented in number of seconds the key must retain the value post
     *                   which data gets deleted from cache
     * @throws InvalidKeyException               - Gets thrown when key is more than 32 chars or null
     * @throws JSONSizeLimitExceededException    - Gets thrown when JSON object exceeds 64kb
     * @throws LocalStorageSizeExceededException - Gets thrown when data store size exceeds 1 GB
     * @throws IOException - Gets thrown if File cannot be created due to File lock issues
     */
    void create(String key, JSONObject value, int timeToLive) throws IOException, LocalStorageSizeExceededException, JSONSizeLimitExceededException, InvalidKeyException;

    /**
     * @param key - Key for retrieving data from store
     * @return - JSON Object retrieved from key
     * @throws InvalidKeyException - Gets thrown when invalid key is specified to access data store
     * @throws IOException - Gets thrown if File cannot be created due to File lock issues
     */
    JSONObject read(String key) throws InvalidKeyException, IOException, InterruptedException;

    /**
     * @param key - Key for deleting data from store
     * @throws InvalidKeyException - Gets thrown when invalid key is specified to access data store
     * @throws IOException - Gets thrown if File cannot be created due to File lock issues
     */
    void delete(String key) throws InvalidKeyException, IOException;
}
