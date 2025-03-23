import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.*;

public class MultiThreadedFileBackupRestore {

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors(); // Use available CPU cores
    private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB chunk size
    private static final Logger logger = Logger.getLogger(MultiThreadedFileBackupRestore.class.getName());

    // Get the destination path based on timestamp and system details
    private static String getDestinationPath() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        int year = currentDateTime.getYear();
        int month = currentDateTime.getMonthValue(); // 1 (January) to 12 (December)
        int date = currentDateTime.getDayOfMonth();
        long millisec = System.currentTimeMillis(); // Get current time in milliseconds

        // Constructing the destination path
        String destinationPath =
                "C:" + File.separator + "AKA" + File.separator + year + File.separator + month + File.separator + date + File.separator + millisec + File.separator;
        return destinationPath;
    }

    // Creating the destination file and its parent directories
    private static void createDestinationFile(String destinationFile, long fileSize) throws IOException {
        Path path = Paths.get(destinationFile);
        Files.createDirectories(path.getParent()); // Creating parent directories if they don't exist
        if (Files.notExists(path)) {
            // Creating the file with the correct size
            try (RandomAccessFile file = new RandomAccessFile(destinationFile, "rw")) {
                file.setLength(fileSize); // Set the file size
            }
        }
    }

    // Backup a chunk of the file using FileChannel
    private static void backupChunk(String sourceFile, String backupFile, long startOffset, long chunkSize) throws IOException {
        try (FileChannel sourceChannel = FileChannel.open(Paths.get(sourceFile), StandardOpenOption.READ);
             FileChannel backupChannel = FileChannel.open(Paths.get(backupFile), StandardOpenOption.WRITE)) {
            backupChannel.position(startOffset); // Set the position to write to
            long transferred = sourceChannel.transferTo(startOffset, chunkSize, backupChannel);
            if (transferred < chunkSize) {
                logger.warning("Transferred fewer bytes than expected: " + transferred + " < " + chunkSize);
            }
        }
    }

    // Restore a chunk of the backup file using FileChannel
    private static void restoreChunk(String backupFile, String restoredFile, long startOffset, long chunkSize) throws IOException {
        try (FileChannel backupChannel = FileChannel.open(Paths.get(backupFile), StandardOpenOption.READ);
             FileChannel restoredChannel = FileChannel.open(Paths.get(restoredFile), StandardOpenOption.WRITE)) {
            restoredChannel.position(startOffset); // Set the position to write to
            long transferred = backupChannel.transferTo(startOffset, chunkSize, restoredChannel);
            if (transferred < chunkSize) {
                logger.warning("Transferred fewer bytes than expected: " + transferred + " < " + chunkSize);
            }
        }
    }

    // Perform the backup operation with multiple threads
    public static String backup(String sourceFile) throws InterruptedException, IOException {
        File source = new File(sourceFile);
        long fileSize = source.length();

        // If the file is small, process it in a single thread
        if (fileSize <= CHUNK_SIZE) {
            String backupFilePath = getDestinationPath() + "backup_" + System.currentTimeMillis() + ".dat";
            createDestinationFile(backupFilePath, fileSize);
            backupChunk(sourceFile, backupFilePath, 0, fileSize);
            logger.info("Backup completed successfully (single-threaded). Backup file: " + backupFilePath);
            return backupFilePath;
        }

        // Otherwise, use multiple threads
        ExecutorService executor = Executors.newCachedThreadPool();
        String backupFilePath = getDestinationPath() + "backup_" + System.currentTimeMillis() + ".dat";
        createDestinationFile(backupFilePath, fileSize);

        List<Throwable> exceptions = new CopyOnWriteArrayList<>();
        long chunkSize = Math.min((fileSize + NUM_THREADS - 1) / NUM_THREADS, 1024 * 1024 * 1024); // 1 GB max chunk size
        for (int i = 0; i < NUM_THREADS; i++) {
            long startOffset = i * chunkSize;
            long size = Math.min(chunkSize, fileSize - startOffset); // Handling the last chunk

            if (size > 0) {
                executor.submit(() -> {
                    try {
                        backupChunk(sourceFile, backupFilePath, startOffset, size);
                    } catch (IOException e) {
                        exceptions.add(e);
                    }
                });
            }
        }

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            logger.warning("Some threads did not finish in time.");
            executor.shutdownNow(); // Forcefully shut down remaining tasks
        }

        if (!exceptions.isEmpty()) {
            Throwable firstException = exceptions.get(0);
            logger.severe("Error occurred in thread: " + firstException.getMessage());
            throw new IOException("One or more threads failed during execution.", firstException);
        }

        logger.info("Backup completed successfully (multithreaded). Backup file: " + backupFilePath);
        return backupFilePath;
    }

    // Performing the restore operation with multiple threads
    public static void restore(String backupFile, String restoredFile) throws InterruptedException, IOException {
        File backup = new File(backupFile);
        long fileSize = backup.length();
        ExecutorService executor = Executors.newCachedThreadPool();

        List<Throwable> exceptions = new CopyOnWriteArrayList<>();
        long chunkSize = Math.min((fileSize + NUM_THREADS - 1) / NUM_THREADS, 1024 * 1024 * 1024); // 1 GB max chunk size
        for (int i = 0; i < NUM_THREADS; i++) {
            long startOffset = i * chunkSize;
            long size = Math.min(chunkSize, fileSize - startOffset); // Handle the last chunk

            if (size > 0) {
                executor.submit(() -> {
                    try {
                        restoreChunk(backupFile, restoredFile, startOffset, size);
                    } catch (IOException e) {
                        exceptions.add(e);
                    }
                });
            }
        }

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            logger.warning("Some threads did not finish in time.");
            executor.shutdownNow(); // Forcefully shut down remaining tasks
        }

        if (!exceptions.isEmpty()) {
            Throwable firstException = exceptions.get(0);
            logger.severe("Error occurred in thread: " + firstException.getMessage());
            throw new IOException("One or more threads failed during execution.", firstException);
        }

        logger.info("Restore completed successfully. Restored file: " + restoredFile);
    }

    // Perform backup and log time taken
    private static String performBackup(String sourceFile) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        String backupFilePath = backup(sourceFile);
        long endTime = System.nanoTime();
        logger.info("Backup completed in " + (endTime - startTime) / 1_000_000 + " ms");
        logger.info("Backup file path: " + backupFilePath);
        return backupFilePath; // Return the backup file path
    }

    // Performing restore and log time taken
    private static void performRestore(String backupFilePath, String restoredFile) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        restore(backupFilePath, restoredFile);
        long endTime = System.nanoTime();
        logger.info("Restore completed in " + (endTime - startTime) / 1_000_000 + " ms");
    }

    // Main method to test the backup and restore process
    public static void main(String[] args) {
        // Check if the correct number of arguments are provided
        if (args.length != 2) {
            System.err.println("Usage: java MultiThreadedFileBackupRestore <sourceFilePath> <restoredFilePath>");
            System.exit(1);
        }

        // Get the source file path and restored file path from command-line arguments
        String sourceFile = args[0]; // First argument: source file path
        String restoredFile = args[1]; // Second argument: restored file path

        try {
            // Perform backup and store the backup file path
            String backupFilePath = performBackup(sourceFile);

            // Perform restore using the stored backup file path
            performRestore(backupFilePath, restoredFile);
        } catch (IOException | InterruptedException e) {
            logger.severe("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
          }
