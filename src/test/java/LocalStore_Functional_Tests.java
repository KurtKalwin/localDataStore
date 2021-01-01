import com.author.kurtkalwin.datastore.Exceptions.*;
import com.author.kurtkalwin.datastore.Interfaces.KeyValueStore;
import com.author.kurtkalwin.datastore.LocalStore;
import org.apache.commons.io.FileUtils;
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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class LocalStore_Functional_Tests {

    private KeyValueStore localStorageService;

    @BeforeEach
    public void Setup() throws IOException {
        localStorageService = new LocalStore();
    }

    @AfterEach
    public void TearDown() {
        cleanupDataStore(localStorageService.getDataStorePath());
    }

    private void cleanupDataStore(String dataStorePath) {
        try {
            File dir = new File(dataStorePath);
            FileUtils.deleteDirectory(dir);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @NotNull
    private JSONObject getSampleJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", "user1");
        jsonObject.put("region", "chennai");
        jsonObject.put("company", "apple");
        return jsonObject;
    }

    @Test
    public void check_if_local_is_store_initialized_with_optional_default_path() throws IOException {
        KeyValueStore keyValueStore = new LocalStore();
        assert (keyValueStore.getDataStorePath().contains("/localDataStore"));
    }

    @Test
    public void check_if_local_is_store_initialized_with_given_path() throws IOException {
        KeyValueStore keyValueStore = new LocalStore("/tmp/fd");
        assert (keyValueStore.getDataStorePath().contains("/tmp/fd"));
    }

    @Test
    public void create_data_store_with_invalid_path_expect_exception() {
        Assertions.assertThrows(IOException.class, () -> new LocalStore("Z://tmp/fd"));
    }

    @Test
    public void create_key_with_16_chars_and_json_string_less_than_64_kb() throws IOException, JSONSizeLimitExceededException, InvalidKeyException, LocalStorageSizeExceededException {
        JSONObject sampleJsonObj = getSampleJsonObject();
        localStorageService.create("key1", sampleJsonObj);
        Path file = Paths.get(localStorageService.getDataStorePath() + "/key1/value.json");
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        assert (attr.size() > 0);
    }

    @Test
    public void create_key_with_more_than_16_chars_and_json_string_less_than_64_kb_expect_exception() {
        Assertions.assertThrows(InvalidKeyException.class, () -> localStorageService.create("This_is_a_longest_key_in_the_whole_world", getSampleJsonObject()));
    }

    @Test
    public void create_key_with_less_than_16_chars_and_json_string_more_than_64_kb_expect_exception() throws IOException {
        FileChannel fileChannel = new FileInputStream(SystemUtils.getUserDir() + "/src/test/java/20kb_Json_test.json").getChannel();
        MappedByteBuffer bb = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        String jsonValue = Charset.defaultCharset().decode(bb).toString();
        JSONObject largeJsonObj = new JSONObject(jsonValue);
        Assertions.assertThrows(JSONSizeLimitExceededException.class, () -> localStorageService.create("key2", largeJsonObj));
    }

    @Test
    public void create_entries_with_duplicate_keys_expect_exception() throws JSONSizeLimitExceededException, LocalStorageSizeExceededException, InvalidKeyException, IOException {
        JSONObject sampleJsonObj = getSampleJsonObject();
        localStorageService.create("key1", sampleJsonObj);
        Assertions.assertThrows(InvalidKeyException.class, () -> localStorageService.create("key1", sampleJsonObj));
    }

    @Test
    public void simple_create_and_read_from_data_store_expect_retrieved_json_object_to_be_same_as_original() throws JSONSizeLimitExceededException, LocalStorageSizeExceededException, InvalidKeyException, IOException, InterruptedException {
        JSONObject jsonObject = getSampleJsonObject();
        localStorageService.create("user1", jsonObject);
        JSONObject userObj = localStorageService.read("user1");
        Assertions.assertEquals(userObj.get("userId"), "user1");
        Assertions.assertEquals(userObj.get("region"), "chennai");
        Assertions.assertEquals(userObj.get("company"), "apple");
    }

    @Test
    public void simple_create_and_delete_expect_key_is_deleted() throws JSONSizeLimitExceededException, LocalStorageSizeExceededException, InvalidKeyException, IOException {
        JSONObject jsonObject = getSampleJsonObject();
        localStorageService.create("user1", jsonObject);
        localStorageService.delete("user1");
        Assertions.assertThrows(InvalidKeyException.class, () -> localStorageService.read("user1"));
    }

    @Test
    public void ttl_expiry_create_key_with_ttl_of_60_seconds_expect_read_after_60_seconds_expect_invalid_key_exception() throws JSONSizeLimitExceededException, LocalStorageSizeExceededException, InvalidKeyException, IOException, InterruptedException {
        JSONObject jsonObject = getSampleJsonObject();
        KeyValueStore keyValueStore = new LocalStore("/tmp/fd");
        keyValueStore.create("user1", jsonObject, 60);
        Thread.sleep(30000);

        //read before TTL expires - should be successful
        JSONObject jsonFromDataStore = keyValueStore.read("user1");
        Assertions.assertEquals(jsonFromDataStore.get("userId"), "user1");
        Assertions.assertEquals(jsonFromDataStore.get("region"), "chennai");
        Assertions.assertEquals(jsonFromDataStore.get("company"), "apple");

        //read after TTL expires - should throw exceptions
        Thread.sleep(40000);
        Assertions.assertThrows(InvalidKeyException.class, () -> keyValueStore.read("user1"));

        cleanupDataStore(keyValueStore.getDataStorePath());
    }
}