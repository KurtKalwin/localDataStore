import com.author.kurtkalwin.datastore.Exceptions.InvalidKeyException;
import com.author.kurtkalwin.datastore.Exceptions.JSONSizeLimitExceededException;
import com.author.kurtkalwin.datastore.Exceptions.LocalStorageSizeExceededException;
import com.author.kurtkalwin.datastore.Interfaces.KeyValueStore;
import com.author.kurtkalwin.datastore.LocalStore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LocalStore_Non_Functional_Tests {
    private KeyValueStore localStorageService;
    private static final int SUCCESS = 0;
    private static final int FAILED = 1;
    private static final int EXCEPTION_THROWN = 2;

    @BeforeEach
    public void Setup() throws IOException {
        localStorageService = new LocalStore();
    }

    @AfterEach
    public void TearDown() {
        String dataStorePath = localStorageService.getDataStorePath();
        cleanupStorage(dataStorePath);
    }

    private void cleanupStorage(String dataStorePath) {
        try {
            File dir = new File(dataStorePath);
            FileUtils.deleteDirectory(dir);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @NotNull
    private JSONObject getSampleJsonObject() throws IOException {
        FileInputStream fis = new FileInputStream(SystemUtils.getUserDir() + "/src/test/java/10kb_Json_test.json");
        String jsonValue = IOUtils.toString(fis, StandardCharsets.UTF_8);
        return new JSONObject(jsonValue);
    }

    @Test
    public void storage_test_store_data_more_than_what_data_store_can_store_expect_size_exceeded_exception() throws IOException {
        KeyValueStore keyValueStore = new LocalStore("/tmp/fd", 100000);
        FileInputStream fis = new FileInputStream(SystemUtils.getUserDir() + "/src/test/java/10kb_Json_test.json");
        String jsonValue = IOUtils.toString(fis, StandardCharsets.UTF_8);
        JSONObject jsonObject = new JSONObject(jsonValue);
        Assertions.assertThrows(LocalStorageSizeExceededException.class, () -> {
            for (int i = 0; i < 100; i++) {
                keyValueStore.create("key" + i, jsonObject);
            }
        });
        cleanupStorage(keyValueStore.getDataStorePath());
    }

    @Test
    public void multiple_threads_create_read_delete_in_parallel() throws InterruptedException, IOException {
        KeyValueStore keyValueStore = new LocalStore("/tmp/fd2");
        final int[] returnValue = new int[3];

        Thread createKeysThread = new Thread(() -> {
            try {
                createKeys(keyValueStore, 5);
                returnValue[0] = SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
                returnValue[0] = FAILED;
            }
        });

        Thread readKeysThread = new Thread(() -> {
            try {
                readKeys(keyValueStore, 1000, 5);
                returnValue[1] = SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
                returnValue[1] = FAILED;
            }
        });

        Thread deleteKeysThread = new Thread(() -> {
            try {
                deleteKeys(keyValueStore, 3000, 5);
                returnValue[2] = SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
                returnValue[2] = FAILED;
            }
        });

        createKeysThread.start();
        readKeysThread.start();
        deleteKeysThread.start();

        createKeysThread.join();
        readKeysThread.join();
        deleteKeysThread.join();

        cleanupStorage(localStorageService.getDataStorePath());

        Assertions.assertEquals(SUCCESS, returnValue[0],"Multi thread test failed for creation keys");
        Assertions.assertEquals(SUCCESS, returnValue[1],"Multi thread test failed for read keys");
        Assertions.assertEquals(SUCCESS, returnValue[2], "Multi thread test failed for delete keys");
    }

    @Test
    public void multiple_threads_create_read_delete_in_parallel_without_delay_for_read_and_delete_expect_InvalidKeyException() throws InterruptedException, IOException {
        KeyValueStore keyValueStore = new LocalStore("/tmp/fd1");
        final int[] returnValue = new int[3];

        Thread createKeysThread = new Thread(() -> {
            try {
                createKeys(keyValueStore, 5);
            } catch (Exception e) {
                e.printStackTrace();
                returnValue[0] = EXCEPTION_THROWN;
            }
        });

        Thread readKeysThread = new Thread(() -> {
            try {
                readKeys(keyValueStore, 0, 5);
            } catch (Exception e) {
                e.printStackTrace();
                returnValue[1] = EXCEPTION_THROWN;
            }
        });

        Thread deleteKeysThread = new Thread(() -> {
            try {
                deleteKeys(keyValueStore, 0, 5);
            } catch (Exception e) {
                e.printStackTrace();
                returnValue[2] = EXCEPTION_THROWN;
            }
        });

        createKeysThread.start();
        readKeysThread.start();
        deleteKeysThread.start();

        createKeysThread.join();
        readKeysThread.join();
        deleteKeysThread.join();

        cleanupStorage(localStorageService.getDataStorePath());

        System.out.println("[0] = " + returnValue[0]);
        System.out.println("[1] = " + returnValue[1]);
        System.out.println("[2] = " + returnValue[2]);

        Assertions.assertTrue((returnValue[0] == EXCEPTION_THROWN || returnValue[1] == EXCEPTION_THROWN || returnValue[2] == EXCEPTION_THROWN), "Multi thread test failed for creation keys");
    }

    @Test
    public void load_testing_create_read_delete_1000_keys_parallel_threads() throws InterruptedException, IOException, JSONSizeLimitExceededException, InvalidKeyException, LocalStorageSizeExceededException {
        KeyValueStore keyValueStore = new LocalStore("/tmp/loadtesting");

        JSONObject sampleObj = getSampleJsonObject();

        System.out.println("*****************************************************************");
        System.out.println("******************* LOAD TESTING RESULT *************************");
        System.out.printf("Number of Keys for Load Testing : %d%n", 100);
        System.out.printf("Size of each JSON Object for Load Testing : %s%n", sampleObj.toString().getBytes().length + " KB");

        long startTime = System.nanoTime();
        createKeys(keyValueStore, 100);
        long endTime = System.nanoTime();
        double elapsed = (endTime - startTime) / 1000000000.00;
        System.out.printf("Duration to CREATE 100 keys : %s Seconds%n", elapsed);

        startTime = System.nanoTime();
        readKeys(keyValueStore, 0, 100);
        endTime = System.nanoTime();
        elapsed = (endTime - startTime) / 1000000000.00;
        System.out.printf("Duration to READ 100 keys : %s Seconds%n", elapsed);

        startTime = System.nanoTime();
        deleteKeys(keyValueStore, 0, 100);
        endTime = System.nanoTime();
        elapsed = (endTime - startTime) / 1000000000.00;
        System.out.printf("Duration to DELETE 100 keys : %s Seconds%n", elapsed);
        System.out.println("*****************************************************************");

        cleanupStorage(localStorageService.getDataStorePath());
   }

    private void createKeys(KeyValueStore keyValueStore, int numberOfKeys) throws JSONSizeLimitExceededException, LocalStorageSizeExceededException, InvalidKeyException, IOException {
        for (int i = 0; i < numberOfKeys; i++) {
            JSONObject jsonObject = getSampleJsonObject();
            keyValueStore.create("key_" + i, jsonObject);
        }
    }

    private void readKeys(KeyValueStore keyValueStore, int delay, int numberOfKeys) throws InvalidKeyException, IOException, InterruptedException {
        Thread.sleep(delay);
        for (int i = 0; i < numberOfKeys; i++) Assertions.assertNotNull(keyValueStore.read("key_" + i));
    }

    private void deleteKeys(KeyValueStore keyValueStore, int delay, int numberOfKeys) throws InterruptedException, InvalidKeyException, IOException {
        Thread.sleep(delay);
        for (int i = 0; i < numberOfKeys; i++) keyValueStore.delete("key_" + i);
    }
}
