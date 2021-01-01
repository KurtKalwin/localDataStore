package com.author.kurtkalwin.datastore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Eviction service class that caters to TTL requirements
 * @author kurtkalwin@gmail.com
 */
class TTLEvictionService extends TimerTask {

    private final ConcurrentHashMap<String, Integer> evictionMap;
    private final FileStoreManager fileStoreManager;

    {
        evictionMap = new ConcurrentHashMap<>();
    }

    public TTLEvictionService(FileStoreManager fileStoreManager) {
        this.fileStoreManager = fileStoreManager;
    }

    public ConcurrentHashMap<String, Integer> getEvictionMap() {
        return evictionMap;
    }

    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
        try {
            ArrayList<String> evictedKeys = new ArrayList<>();
            for (String key :
                    evictionMap.keySet()) {
                try {
                    if (shouldEvictKey(key)) {
                        fileStoreManager.deleteKey(key);
                        evictedKeys.add(key);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (String key :
                    evictedKeys) {
                evictionMap.remove(key);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean shouldEvictKey(String key) throws IOException {
        Path file = Paths.get(fileStoreManager.getDataStorePath() + "/" + key + "/" + "value.json");
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        long lastModifiedInSeconds = attr.creationTime().to(TimeUnit.SECONDS);
        long currentTimeInSeconds = Instant.now().getEpochSecond();
        long elapsedTimeInSeconds = currentTimeInSeconds - lastModifiedInSeconds;
        return elapsedTimeInSeconds >= evictionMap.get(key);
    }
}
