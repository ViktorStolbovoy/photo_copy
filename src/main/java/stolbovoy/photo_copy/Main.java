package stolbovoy.photo_copy;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Strings;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


public class Main
{
    static Params params = new Params();

    public static void main(String[] args) {
        System.out.println("Photo  copy");


        JCommander.newBuilder()
            .addObject(params)
            .build()
            .parse(args);
        parseDirectory();
    }

    private static void parseDirectory() {
        File dir = new File(params.inputPath);
        if (!dir.isDirectory()) {
            throw new ParameterException("Input directory (-i) is not a directory: " + params.inputPath);
        }
        traverseDir(dir);
    }

    private static void traverseDir(File dir) {
        for(File entry: dir.listFiles()) {
            if (entry.isDirectory() && !params.nonRecursive) {
                traverseDir(entry);
            } else {
                String[] tokens = StringUtils.split(entry.getName(), ".");
                int count = tokens.length;
                if (count == 1) {
                    return;
                }

                String ext = tokens[count - 1];
                PathFormatter pf = params.getFileFormatter(ext);
                if (pf == null) {
                    return;
                }

                try {
                    transferFile(entry, pf);
                } catch (IOException e) {
                    System.out.println("Can't copy " + entry.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void transferFile(File sourceFile, PathFormatter destinationPath)
            throws IOException
    {
        String destinationFileName = getDestinationPath(sourceFile, destinationPath);
        if (Strings.isStringEmpty(destinationFileName)) {
            System.out.println("\tFile " + sourceFile + " already exists in target directory");
            return;
        }

        Path destination = Paths.get(destinationFileName);
        String sourceFileName = sourceFile.getAbsolutePath();
        Path originalPath = Paths.get(sourceFileName);
        System.out.println("Transferring " + sourceFileName + " to " + destinationFileName);
        Files.copy(originalPath, destination);
    }

    private static void ensureDestinationDirExists(String destinationDir) {
        File directory = new File(destinationDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private static String getDestinationPath(File file, PathFormatter destinationPath)
            throws IOException
    {
        String fn = file.getName();
        String destinationDir = formatPath(destinationPath, extractDateShot(file));
        ensureDestinationDirExists(destinationDir);
        String fullDestinationPath = destinationDir + File.separatorChar + file.getName();
        String fullSourcePath = file.getAbsolutePath();
        int newIdx = 0;
        boolean fileExists = new File(fullDestinationPath).exists();
        if (fileExists) {
            int dotIdx = fullDestinationPath.lastIndexOf('.');
            String destinationPathNoExtension = fullDestinationPath.substring(0, dotIdx);
            String extension = fullDestinationPath.substring(dotIdx, fullDestinationPath.length());

            while (fileExists) {
                newIdx ++;
                if (compareFiles(fullSourcePath, fullDestinationPath)) return null; //the same file exists - don't copy
                fullDestinationPath = destinationPathNoExtension + "-" + Integer.toString(newIdx) + extension;
                fileExists = new File(fullDestinationPath).exists();
            }
        }

        return fullDestinationPath;
    }

    private static boolean compareFiles(String file1, String file2)
            throws IOException
    {

        if (getFileSize(file1) != getFileSize(file2)) return false;

        byte[] buf1 = new byte[10240];
        byte[] buf2 = new byte[10240];
        try (InputStream is1 = Files.newInputStream(Paths.get(file1))) {

            try (InputStream is2 = Files.newInputStream(Paths.get(file2))) {
                int len1 = 1;
                int len2 = 1;
                while (len1 > 0 && len2 > 0 && len1 == len2) {
                    len1 = is1.read(buf1);
                    len2 = is2.read(buf2);
                    if (len1 != len2)
                        return false;
                    for (int i = 0; i < len1; i++) {
                        if (buf1[i] != buf2[i])
                            return false;
                    }
                }
            }
        }

        return true;
    }

    private static long getFileSize(String fileName)
            throws IOException
    {
        Path path = Paths.get(fileName);
        BasicFileAttributes attr = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
        return attr.size();
    }

    private static String formatPath(PathFormatter path, Instant dateShot) {
        // TODO: Check timezone so file time created is local at the point of shooting
        LocalDateTime ldt = LocalDateTime.ofInstant(dateShot, ZoneId.systemDefault());
        return path.format(ldt);
    }
    private static Instant extractDateShot(File file)
            throws IOException
    {
        String fileName = file.getAbsolutePath();
        Path path = Paths.get(fileName);

        BasicFileAttributes attr = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
        FileTime fileTime = attr.lastModifiedTime();
        return fileTime.toInstant();
    }
}
