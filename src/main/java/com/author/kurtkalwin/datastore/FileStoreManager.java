package com.author.kurtkalwin.datastore;

import com.author.kurtkalwin.datastore.Exceptions.InvalidKeyException;
import com.author.kurtkalwin.datastore.Exceptions.LocalStorageSizeExceededException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;

import static java.lang.Thread.sleep;

/**
 * File store manager that manages create, read and delete operations.
 * @author kurtkalwin@gmail.com
 */
class FileStoreManager {

    private final int dataStoreSize;
    private String dataStorePath;

    /**
     * @param dataStorePath Data storage path for local store. If none provided a default path will be chosen
     * @param dataStoreSize Max Storage size for data store
     * @throws IOException When Data store cannot be created.
     */
    public FileStoreManager(String dataStorePath, int dataStoreSize) throws IOException {
        this.dataStoreSize = dataStoreSize;
        setupDataStore(dataStorePath);
    }

    /**
     * @return Max Data store size
     */
    public int getDataStoreSize() {
        return dataStoreSize;
    }

    /**
     * @return Data storage path
     */
    public String getDataStorePath() {
        return dataStorePath;
    }

    /**
     * @param key Key to be written to data store
     * @param jsonObject JSON Object to be stored in data store
     * @throws IOException IOException when writing to data sore
     * @throws LocalStorageSizeExceededException When storage exceeds the allowed size
     * @throws IllegalStateException File lock exception
     * @throws InvalidKeyException If key already exists then write with same key will fail
     */
    public void writeToDataStore(String key,
                                 JSONObject jsonObject) throws IOException, LocalStorageSizeExceededException, IllegalStateException, InvalidKeyException {
        checkDataStoreSize();
        File file = new File(createKeyStore(key) + "/" + "value.json");
        try (FileChannel fileChannel = new FileOutputStream(file, true).getChannel()) {
            acquireFileLock(fileChannel);
            ByteBuffer buf = ByteBuffer.wrap(jsonObject.toString().getBytes());
            buf.put(jsonObject.toString().getBytes());
            buf.flip();
            while (buf.hasRemaining()) {
                fileChannel.write(buf);
            }
        } catch (OverlappingFileLockException | IOException | InterruptedException ex) {
            throw new IllegalStateException("Unable to create key entry in data store, Retry operation in few seconds");
        }
    }

    /**
     * @param key Key to be retrieved from data store
     * @return JSONObject from the data store
     * @throws IOException IO exception when reading from data store
     * @throws IllegalStateException File Lock exceptions
     */
    public JSONObject readFromDataStore(String key) throws IOException, IllegalStateException {
        RandomAccessFile randomaccessfile = new RandomAccessFile(getDataStorePath() + "/" + key + "/" + "value.json", "rw");
        FileChannel fileChannel = randomaccessfile.getChannel();
        try (fileChannel) {
            acquireFileLock(fileChannel);
            MappedByteBuffer bb = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            String jsonValue = Charset.defaultCharset().decode(bb).toString();
            return new JSONObject(jsonValue);
        } catch (OverlappingFileLockException | IOException | InterruptedException ex) {
            throw new IllegalStateException("Unable to create key entry in data store, Retry operation in few seconds");
        }
    }

    /**
     * @param key Key to be deleted from data store
     * @throws IOException IO Exception when deleting from data store
     */
    public void deleteKey(String key) throws IOException {
        RandomAccessFile randomaccessfile = new RandomAccessFile(getDataStorePath() + "/" + key + "/" + "value.json", "rw");
        FileChannel fileChannel = randomaccessfile.getChannel();
        try (fileChannel) {
            acquireFileLock(fileChannel);
            File dir = new File(getDataStorePath() + "/" + key);
            FileUtils.deleteDirectory(dir);
        } catch (OverlappingFileLockException | IOException | InterruptedException ex) {
            throw new IllegalStateException("Unable to create key entry in data store, Retry operation in few seconds");
        }
    }

    private void setupDataStore(String dataStorePath) throws IOException {
        if (dataStorePath.equals("")) {
            this.dataStorePath = createLocalStore(SystemUtils.getUserHome().getCanonicalPath() + "/localDataStore");
        } else {
            this.dataStorePath = createLocalStore(dataStorePath);
        }
    }

    private String createLocalStore(String fileStorePath) throws IOException {
        boolean isDirectoryCreated = true;
        File dataStorePath = new File(String.valueOf(fileStorePath));
        if (!dataStorePath.exists()) {
            isDirectoryCreated = dataStorePath.mkdir();
        }
        if (isDirectoryCreated) {
            return dataStorePath.getCanonicalPath();
        } else {
            throw new IOException("Unable to create a datastore");
        }
    }

    public boolean checkIfKeyExists(String key) {
        return new File(getDataStorePath() + "/" + key).exists();
    }

    private void acquireFileLock(FileChannel fileChannel) throws IOException, InterruptedException {
        int retryCount = 0;
        while (fileChannel.tryLock() == null) {
            //noinspection BusyWait
            sleep(1000);
            if (retryCount > 10) {
                break;
            } else {
                retryCount++;
            }
        }
    }

    private void checkDataStoreSize() throws LocalStorageSizeExceededException {
        if (FileUtils.sizeOfDirectory(new File(getDataStorePath())) > getDataStoreSize()) {
            throw new LocalStorageSizeExceededException();
        }
    }

    private String createKeyStore(String key) throws IOException, InvalidKeyException {
        String keyStorePath = "%s/%s".formatted(getDataStorePath(), key);
        File dataStorePath = new File(keyStorePath);
        if (dataStorePath.exists()) {
            throw new InvalidKeyException("Given key already present in datastore");
        } else {
            if (dataStorePath.mkdir()) {
                return dataStorePath.getCanonicalPath();
            }
            throw new IOException("Unable to create key entry in data store, Retry operation in few seconds");
        }
    }
}
