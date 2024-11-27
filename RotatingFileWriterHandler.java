@Component
@Slf4j
public class RotatingFileWriterHandler {
    
    private final String outputDirectory;
    private final long maxFileSize;
    private final String filePrefix;
    private final Object fileLock = new Object();
    private File currentFile;
    private long currentFileSize;

    public RotatingFileWriterHandler(
            @Value("${app.file.output.directory:/tmp/output}") String outputDirectory,
            @Value("${app.file.max.size:100MB}") String maxFileSize,
            @Value("${app.file.prefix:syslog}") String filePrefix) {
        this.outputDirectory = outputDirectory;
        this.maxFileSize = parseSize(maxFileSize);
        this.filePrefix = filePrefix;
        createNewFile();
    }

    private long parseSize(String sizeStr) {
        long multiplier = switch (sizeStr.replaceAll("[0-9]", "").toUpperCase()) {
            case "KB" -> 1024L;
            case "MB" -> 1024L * 1024L;
            case "GB" -> 1024L * 1024L * 1024L;
            default -> 1L;
        };
        return Long.parseLong(sizeStr.replaceAll("[^0-9]", "")) * multiplier;
    }

    private void createNewFile() {
        try {
            File dir = new File(outputDirectory);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Failed to create directory: " + outputDirectory);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            currentFile = new File(dir, String.format("%s_%s.log", filePrefix, timestamp));
            currentFileSize = 0;
            log.info("Created new log file: {}", currentFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create new file", e);
            throw new MessageHandlingException(e);
        }
    }

    private void rotateFileIfNeeded(int contentLength) {
        if (currentFileSize + contentLength > maxFileSize) {
            try {
                File fileToRotate = currentFile;
                createNewFile();
                
                // Compress the rotated file in a separate thread
                CompletableFuture.runAsync(() -> {
                    try {
                        compressFile(fileToRotate);
                    } catch (IOException e) {
                        log.error("Failed to compress rotated file: {}", fileToRotate, e);
                    }
                });
            } catch (Exception e) {
                log.error("Failed to rotate file", e);
                throw new MessageHandlingException(e);
            }
        }
    }

    private void compressFile(File file) throws IOException {
        File gzipFile = new File(file.getPath() + ".gz");
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(gzipFile));
             FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.transferTo(gzipOutputStream);
        }
        if (!file.delete()) {
            log.warn("Failed to delete original file after compression: {}", file);
        }
    }

    public void write(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        synchronized (fileLock) {
            try {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                rotateFileIfNeeded(bytes.length);
                
                Files.write(currentFile.toPath(), 
                           (content + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                           StandardOpenOption.CREATE, 
                           StandardOpenOption.APPEND);
                
                currentFileSize += bytes.length;
                
            } catch (IOException e) {
                log.error("Failed to write to file: {}", currentFile, e);
                throw new MessageHandlingException(e);
            }
        }
    }
}
