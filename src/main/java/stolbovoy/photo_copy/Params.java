package stolbovoy.photo_copy;
import com.beust.jcommander.*;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;
import java.util.*;

public class Params
{
    enum FileKind {
        UNKNOWN,
        JPEG,
        RAW,
        VIDEO,
    }

    @Parameter(names = {"-i"}, description = "Input directory", required = true)
    public String inputPath;

    @Parameter(names = {"-nr"}, description = "Don't search files in input directory recursively")
    public boolean nonRecursive;

    @Parameter(names = {"-rext"}, description = "Raw file extensions to copy", variableArity = true)
    public List<String> rawFileExt = new ArrayList<>(Arrays.asList(
            "3fr",
            "ari", "arw",
            "bay",
            "braw", "crw", "cr2", "cr3",
            "cap",
            "data", "dcs", "dcr", "dng",
            "drf",
            "eip", "erf",
            "fff",
            "gpr",
            "iiq",
            "k25", "kdc",
            "mdc", "mef", "mos", "mrw",
            "nef", "nrw",
            "obm", "orf",
            "pef", "ptx", "pxn",
            "r3d", "raf", "raw", "rwl", "rw2", "rwz",
            "sr2", "srf", "srw",
            "tif",
            "x3f"
    ));

    @Parameter(names = {"-rvid"}, description = "Video file extensions to copy", variableArity = true)
    public List<String> videoFileExt = new ArrayList<>(Arrays.asList(
            "mp4",
            "mpv"
    ));


    @Parameter(names = {"-o"}, description = "Output path for JPEG (e.g. C:\\\\photo\\{YYYY-MM-DD})", required = true)
    private String outputPathJpeg;

    @Parameter(names = {"-r"}, description = "Output path for raw (e.g. C:\\\\photo\\{YYYY-MM-DD}\\RAW) default is -o + \\RAW")
    private String outputPathRaw;

    @Parameter(names = {"-v"}, description = "Output path for video (e.g. C:\\\\photo\\{YYYY-MM-DD}\\RAW) default is -o + \\VIDEO")
    private String outputPathVideo;

    //// Path handling
    private PathFormatter pathFormatterJpeg;
    private PathFormatter pathFormatterRaw;
    private PathFormatter pathFormatterVideo;
    private Map<String, FileKind> extMap;

    private static void addToExtMap(Map<String, FileKind> map, List<String> extensions, FileKind kind) {
        for (String ext: extensions) {
            map.put(ext.toLowerCase(), kind);
        }
    }

    private Map<String, FileKind> getExtMap() {
        Map<String, FileKind> map = new HashMap<>();
        addToExtMap(map, Arrays.asList("jpg", "jpeg"), FileKind.JPEG);
        addToExtMap(map, rawFileExt, FileKind.RAW);
        addToExtMap(map, videoFileExt, FileKind.VIDEO);

        return map;
    }

    private String ensurePathSet(String path, String suffix) {
        if (!StringUtils.isEmpty(path)) {
            return path;
        }
        return Paths.get(outputPathJpeg, suffix).toString();
    }

    private void initPathsData() {
        pathFormatterJpeg = new PathFormatter(outputPathJpeg);
        pathFormatterRaw = new PathFormatter(ensurePathSet(outputPathRaw, "RAW"));
        pathFormatterVideo = new PathFormatter(ensurePathSet(outputPathVideo, "VIDEO"));

        extMap = getExtMap();
    }

    public PathFormatter getFileFormatter(String extension) {
        if (extMap == null) {
            initPathsData();
        }

        switch (extMap.getOrDefault(extension.toLowerCase(), FileKind.UNKNOWN)) {
            case JPEG:
                return pathFormatterJpeg;
            case RAW:
                return pathFormatterRaw;
            case VIDEO:
                return pathFormatterVideo;
            default:
                return null;
        }
    }
}
