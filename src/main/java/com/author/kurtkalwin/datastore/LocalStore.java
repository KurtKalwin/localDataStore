package com.author.kurtkalwin.datastore;

import com.author.kurtkalwin.datastore.Exceptions.LocalStorageSizeExceededException;
import com.author.kurtkalwin.datastore.Exceptions.InvalidKeyException;
import com.author.kurtkalwin.datastore.Exceptions.JSONSizeLimitExceededException;
import com.author.kurtkalwin.datastore.Interfaces.KeyValueStore;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;

/**
 * CRD (create, read and delete) implementation for LocalDataStore
 * @author kurtkalwin@gmail.com
 */
public class LocalStore implements KeyValueStore {

    private static final int DEFAULT_MAX_DATASTORE_SIZE = 1073741824;
    private static final int EVICTION_PERIOD = 2000;
    private static final int MAX_KEY_CHARS = 32;
    private static final int MAX_JSON_OBJECT_SIZE = 16384;
    private final FileStoreManager fileStoreManager;
    private TTLEvictionService ttlEvictionService;

    public LocalStore() throws IOException {
        this("");
    }

    public LocalStore(String fileStorePath) throws IOException {
        this(fileStorePath, DEFAULT_MAX_DATASTORE_SIZE);
    }

    public LocalStore(String fileStorePath, int dataStoreSize) throws IOException {
        fileStoreManager = new FileStoreManager(fileStorePath, dataStoreSize);
        startTTLEvictionService();
    }

    /**
     * @return Storage path of local data store
     */
    @Override
    public String getDataStorePath() {
        return fileStoreManager.getDataStorePath();
    }

    /**
     * @param key   - Key for the data store. Must be capped to 32 chars
     * @param value - JSON Object (value). Value cannot exceed 16 KB
     * @throws InvalidKeyException               - Gets thrown when key is more than 32 chars or null
     * @throws JSONSizeLimitExceededException    - Gets thrown when JSON object exceeds 64kb
     * @throws LocalStorageSizeExceededException - Gets thrown when data store size exceeds 1 GB
     * @throws IOException - Gets thrown if File cannot be created due to File lock issues
     */
    @Override
    public synchronized void create(String key, JSONObject value) throws JSONSizeLimitExceededException, LocalStorageSizeExceededException, InvalidKeyException, IOException {
        create(key, value, 0);
    }

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
    @Override
    public synchronized void create(String key, JSONObject value, int timeToLive) throws IOException, LocalStorageSizeExceededException, JSONSizeLimitExceededException, InvalidKeyException {
        validateKeyAndValue(key, value);
        fileStoreManager.writeToDataStore(key, value);
        if (timeToLive > 0) {
            ttlEvictionService.getEvictionMap().put(key, timeToLive);
        }
    }

    /**
     * @param key - Key for retrieving data from store
     * @return - JSON Object retrieved from key
     * @throws InvalidKeyException - Gets thrown when invalid key is specified to access data store
     * @throws IOException - Gets thrown if File cannot be created due to File lock issues
     */
    @Override
    public synchronized JSONObject read(String key) throws InvalidKeyException, IOException {
        if (key == null) throw new InvalidKeyException("Key cannot be NULL");
        if (!fileStoreManager.checkIfKeyExists(key)) throw new InvalidKeyException("Given key is not present in the datastore");
        return fileStoreManager.readFromDataStore(key);
    }

    /**
     * @param key - Key for deleting data from store
     * @throws InvalidKeyException - Gets thrown when invalid key is specified to access data store
     * @throws IOException - Gets thrown if File cannot be created due to File lock issues
     */
    @Override
    public synchronized void delete(String key) throws InvalidKeyException, IOException {
        if (key == null) throw new InvalidKeyException("Key cannot be NULL");
        if (!fileStoreManager.checkIfKeyExists(key)) throw new InvalidKeyException("Given key is not present in the datastore");
        fileStoreManager.deleteKey(key);
    }

    private void startTTLEvictionService() {
        Timer timer = new Timer();
        ttlEvictionService = new TTLEvictionService(fileStoreManager);
        timer.schedule(ttlEvictionService, 0, EVICTION_PERIOD);
    }

    /**
     * @param key   Check for valid key
     * @throws InvalidKeyException            Gets thrown if key is null
     * @throws JSONSizeLimitExceededException Gets thrown if JSON Object (VALUE) size is more than allowed
     */
    private void validateKeyAndValue(String key, JSONObject value) throws InvalidKeyException,
            JSONSizeLimitExceededException {
        if (key == null) throw new InvalidKeyException("Key cannot be NULL");
        if (key.length() > MAX_KEY_CHARS) throw new InvalidKeyException("Key cannot be more than " + MAX_KEY_CHARS + " chars" );
        if (fileStoreManager.checkIfKeyExists(key)) throw new InvalidKeyException("Key already exists in DataStore");
        validateSizeofJsonObject(value);
    }

    private void validateSizeofJsonObject(JSONObject value) throws JSONSizeLimitExceededException {
        if (value.toString().getBytes().length > MAX_JSON_OBJECT_SIZE) throw new JSONSizeLimitExceededException();
    }
}
