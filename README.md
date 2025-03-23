# Multi-Threaded File Backup & Restore using Java

This project demonstrates efficient file backup and restore using Java multithreading. Large files are split into chunks, processed in parallel for speed, and stored in unique timestamp directories to prevent overwriting. It uses `ExecutorService` for managing threads and `FileChannel` for I/O, supporting files like `.txt`, `.jpg`, `.png`, `.mp4`, `.mp3`, etc.

## Aim
This project shows a simple demonstration of:
- File backup and restore using multithreading.
- By using multiple threads, we can speed up the backup and restore process.

## Description
In this project, the given large file is divided into chunks and each chunk is processed in parallel using multiple threads. Each backup file is stored in a unique directory using a timestamp technique, so that the previous backup files do not overwrite. 

In this project, I used:
1. `ExecutorService` for managing threads.
2. Logger for efficient debugging and error tracking.
3. `FileChannel` for I/O operations to support backup and restoration for various types of files (`.txt`, `.jpg`, `.png`, `.mp4`, `.mp3`, etc.).

## Input 
I have given the source file path and the restore file path(which will be empty initially) as command line arguments.
## Working of the Code

### 1. Backup Process
#### Step 1: Divide the File into Chunks
- The program checks the size of the file. If the file is small (less than 1 MB), it processes it in a single thread.
- If the file is large, it divides the file into smaller chunks (1 MB each) so that each chunk can be processed by a separate thread.

#### Step 2: Create a Backup File
- A backup file is created in a unique directory based on the current date and time (e.g., `C:/AKA/2023/10/25/1698234567890/backup_1698234567890.dat`).
- This ensures that each backup is stored in a separate folder, preventing overwriting of previous backups.

#### Step 3: Backup Each Chunk
- Each chunk of the file is processed by a separate thread.
- The program uses `FileChannel` to read the data from the source file and write it to the backup file.

#### Step 4: Log Progress
- The program logs the progress of the backup operation, including the time taken and any errors encountered.

### 2. Restore Process
#### Step 1: Divide the Backup File into Chunks
- The backup file is divided into smaller chunks, just like during the backup process.

#### Step 2: Restore Each Chunk
- Each chunk is processed by a separate thread.
- The program uses `FileChannel` to read the data from the backup file and write it to the restored file.

#### Step 3: Log Progress
- The program logs the progress of the restore operation, including the time taken and any errors encountered.

### How Multithreading Works
- The program uses multiple threads to process chunks of the file in parallel.
- The number of threads is determined by the number of CPU cores available on your system (e.g., if your system has 4 cores, it will use 4 threads).
- Java's `ExecutorService` is used to manage these threads efficiently.
