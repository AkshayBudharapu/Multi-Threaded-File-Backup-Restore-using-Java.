import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.logging.*;

public class SingleThreadedFileBackupRestore {

    private static final int CHUNK_SIZE = 1024 * 1024; // 1 MB chunk size
    private static final Logger logger = Logger.getLogger(SingleThreadedFileBackupRestore.class.getName());

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

    // Backup the entire file using FileChannel
    private static void backupFile(String sourceFile, String backupFile) throws IOException {
        try (FileChannel sourceChannel = FileChannel.open(Paths.get(sourceFile), StandardOpenOption.READ);
             FileChannel backupChannel = FileChannel.open(Paths.get(backupFile), StandardOpenOption.WRITE)) {
            long fileSize = sourceChannel.size();
            long transferred = sourceChannel.transferTo(0, fileSize, backupChannel);
            if (transferred < fileSize) {
                logger.warning("Transferred fewer bytes than expected: " + transferred + " < " + fileSize);
            }
        }
    }

    // Restore the entire file using FileChannel
    private static void restoreFile(String backupFile, String restoredFile) throws IOException {
        try (FileChannel backupChannel = FileChannel.open(Paths.get(backupFile), StandardOpenOption.READ);
             FileChannel restoredChannel = FileChannel.open(Paths.get(restoredFile), StandardOpenOption.WRITE)) {
            long fileSize = backupChannel.size();
            long transferred = backupChannel.transferTo(0, fileSize, restoredChannel);
            if (transferred < fileSize) {
                logger.warning("Transferred fewer bytes than expected: " + transferred + " < " + fileSize);
            }
        }
    }

    // Performing the backup operation in a single thread
    public static String backup(String sourceFile) throws IOException {
        File source = new File(sourceFile);
        long fileSize = source.length();
        String backupFilePath = getDestinationPath() + "backup_" + System.currentTimeMillis() + ".dat";
        createDestinationFile(backupFilePath, fileSize);
        backupFile(sourceFile, backupFilePath);
        logger.info("Backup completed successfully. Backup file: " + backupFilePath);
        return backupFilePath;
    }

    // Performing the restore operation in a single thread
    public static void restore(String backupFile, String restoredFile) throws IOException {
        File backup = new File(backupFile);
        long fileSize = backup.length();
        createDestinationFile(restoredFile, fileSize);
        restoreFile(backupFile, restoredFile);
        logger.info("Restore completed successfully. Restored file: " + restoredFile);
    }

    // Performing backup and log time taken
    private static String performBackup(String sourceFile) throws IOException {
        long startTime = System.nanoTime();
        String backupFilePath = backup(sourceFile);
        long endTime = System.nanoTime();
        logger.info("Backup completed in " + (endTime - startTime) / 1_000_000 + " ms");
        logger.info("Backup file path: " + backupFilePath);
        return backupFilePath; // Return the backup file path
    }

    // Performing restore and log time taken
    private static void performRestore(String backupFilePath, String restoredFile) throws IOException {
        long startTime = System.nanoTime();
        restore(backupFilePath, restoredFile);
        long endTime = System.nanoTime();
        logger.info("Restore completed in " + (endTime - startTime) / 1_000_000 + " ms");
    }

    // Main method to test the backup and restore process
    public static void main(String[] args) {
        // Check if the correct number of arguments are provided
        if (args.length != 2) {
            System.err.println("Usage: java SingleThreadedFileBackupRestore <sourceFilePath> <restoredFilePath>");
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
        } catch (IOException e) {
            logger.severe("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
  }
