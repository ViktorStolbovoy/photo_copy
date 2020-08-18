package stolbovoy.photo_copy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class PathFormatter
{
    public PathFormatter(String path) {
        String[] tokens = StringUtils.split(path, "{");
        pathTokens = new ArrayList<>(tokens.length * 2 - 1);

        // The first token is always a string prefix
        pathTokens.add(new ConstToken(tokens[0]));

        for(int i = 1; i < tokens.length; i ++) {
            String[] subTokens = StringUtils.splitPreserveAllTokens(tokens[i], "}", -1);
            if (subTokens.length != 2) {
                throw new IllegalArgumentException("Path format doesn't have a closing }: " + path);
            }
            pathTokens.add(new DataToken(subTokens[0]));
            if (subTokens[1].length() > 0) {
                pathTokens.add(new ConstToken(subTokens[1]));
            }
        }
    }

    private List<Token> pathTokens;

    public String format(LocalDateTime ts) {
        StringBuilder sb = new StringBuilder(pathTokens.size());
        for(Token token: pathTokens) {
            sb.append(token.getResult(ts));
        }

        return sb.toString();
    }

    private abstract class Token {
        public abstract String getResult(LocalDateTime ts);
    }

    private class ConstToken extends Token {
        private String data;
        public ConstToken(String data) {
            this.data = data;
        }

        public String getResult(LocalDateTime ts) {
            return data;
        }
    }

    private class DataToken extends Token {
        private DateTimeFormatter formatter;
        public DataToken(String pattern) {
            formatter = DateTimeFormatter.ofPattern(pattern);
        }

        public String getResult(LocalDateTime ts) {
            return ts.format(formatter);
        }
    }

}
