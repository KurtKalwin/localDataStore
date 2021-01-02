# Key-Value Local DataStore #

This Key-Value Local DataStore library is written in Java by **Kurt Kalwin**
For license please check [![license](https://img.shields.io/github/license/DAVFoundation/captain-n3m0.svg?style=flat-square)](https://github.com/KurtKalwin/localDataStore/blob/main/LICENSE)

## Table of Contents ##
=======================
* [Library Usage](#library-usage-)
* [Design Approach](#design-approach-)
  * [Class Diagram](#class-diagram)
  * [Sequence Diagram for create API](#sequence-diagram-for-create-api)
  * [Sequence Diagram for read API](#sequence-diagram-for-read-api)
  * [Sequence Diagram for delete API](#sequence-diagram-for-delete-api)
  * [Handling complexity in Inter-thread and Inter-process locks](#handling-complexity-in-inter-thread-and-inter-process-locks)
  * [Error Handling](#error-handling)
* [Clean Code and TDD Approach](#clean-code-and-tdd-approach-)
  * [Code Coverage](#code-coverage)
  * [Functional and Non-Functional Test results](#functional-and-non-functional-test-results)
  * [Performance Test Result](#performance-test-result)
* [Build and Dependencies](#build-and-dependencies)

## Library usage : ##

```Java
import com.author.kurtkalwin.datastore.Exceptions.*;
import com.author.kurtkalwin.datastore.Interfaces.KeyValueStore;
import com.author.kurtkalwin.datastore.LocalStore;
```

```
KeyValueStore localStore = new LocalStore(); //assumes default path and 1GB storage
JSONObject jsonObject = new JSONObject();
jsonObject.put("userId", "user1");

localStore.create("keyabc1", jsonObject); //create a key-value in data store
JSONObject jsonFromDataStore = localStore.read("keyabc1"); //retrieve key from data store
localStore.delete("keyabc1"); //delete key from data store

localStore.create("keywithttl", jsonObject, 60) // create a key with TTL = 60 seconds
```

You can also create data store with an optional path and size :
```
KeyValueStore localStore = new LocalStore("/home/user/fd"); //assumes default 1GB storage size
KeyValueStore localStore = new LocalStore("/home/user/fd", 100000);
```

## Design Approach : ##

:star: Key design principle is to store Key/Value pair in a folder/file hierarchy instead of storing all the key-value 
pairs in a single file. The design ensures consumer of this library will consume little memory and provide the 
best performance for storing and retrieving data from key-value store.

This design prevents locking issues on a single file as different values are stored under respective "Key" 
folders.:star:

![Key Value stored in a folder / file hierarchy ](/images/datastore_hierarchy.png?raw=true)

### Class Diagram 

![Class Diagram](/images/datastore_classdiagram.png?raw=true "Class Diagram")

* **create API** simply creates a new folder with a key name and stores JSONObject as value.json under that folder. 
  
* **read API** easily locates value.json by concatenating data storage path(parent folder) with key (folder name). 
  Easy to locate and yields better performance 
  
* **delete API** operates by deleting the folder with key name. It's that simple with this approach   
  

:hourglass:
***Considering datastore can go up to 1 GB it's not a good idea to use HashMap as serializing and de-serializing 
1 GB file can lead to major performance issues.***:hourglass:

### Sequence Diagram for create API ###
![Sequence Diagram for create API](/images/sequence_create.png?raw=true "Sequence Diagram for create API")
### Sequence Diagram for read API ###
![Sequence Diagram for read API](/images/sequence_read.png?raw=true "Sequence Diagram for read API")
### Sequence Diagram for delete API ###
![Sequence Diagram for delete API](/images/sequence_delete.png?raw=true "Sequence Diagram for delete API")


### Handling complexity in Inter-thread and Inter-process locks ###

* For **inter-thread** file locking create, read and delete methods are marked as synchronized. 
* For **inter-process** FileChannel.tryLock() with retry mechanism is used for CRD operations.

Just using synchronization will not have any effect on threads running on a different JVM. 
Hence, FileChannel.trylock() is used to acquire a lock on file that prevents another process from getting a 
lock on it. :+1:

### Error Handling ###
Appropriate error responses are thrown as exceptions if data store is used in unexpected ways
or breaches any limits.

* InvalidKeyException() - Gets thrown if key is more than 32 chars or when read
or delete is tried to access without a valid key. This is also thrown if an attempt is made to create duplicate keys.

* JSONSizeLimitExceededException() - Gets thrown when JSONObject more than 16 kb is passed to create API

* LocalStorageSizeExceededException() - Gets thrown once data storage limit is reached

## Clean Code and TDD Approach : ##
This library was developed following test-driven development principles and has 100% code coverage. 
Tests cover both functional 
and non-functional requirements. Single-responsibility principle has been adopted throughout the modules

When it comes to writing code ***Less is more***. Entire library is written in about 280 lines of code 
(excluding comments / blank lines).

### Code Coverage ###

* 100% code coverage has been attained covering both functional and non-functional tests.

![Code Coverage](/images/codecoverage.png?raw=true "Code Coverage")

### Functional and Non-Functional Test results ###

* Scenarios covering all functional and non-functional tests have been written with meaningful test method names that
describes the test scenario and expected result.

![Test Results](/images/intellijtestresults.png?raw=true "Code Coverage")

### Performance Test Result ###

Performance and Load tests are covered as part of JUnit tests and below are those test methods

* multiple_threads_create_read_delete_in_parallel
* multiple_threads_create_read_delete_in_parallel_without_delay_for_read_and_delete_expect_InvalidKeyException
* load_testing_create_read_delete_1000_keys_parallel_threads

Performance test results for creating, reading and deleting 1000 keys is posted in this image :
![Performance Test Results](/images/performance_results.png?raw=true)

## Build and Dependencies ##
Gradle is the build automation tool for this project.
build.gradle file shows the dependencies used for this project.
