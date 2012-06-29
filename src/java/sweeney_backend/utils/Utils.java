package sweeney_backend.utils;

import java.io.IOException;
import java.io.StringReader;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sweeney_backend.sax.ExtractTextHandler;

public class Utils {

    public static final ThreadLocal<Parser> parser = new ThreadLocal<Parser>() {
        protected Parser initialValue() {
            Parser p = new Parser();
            try {
                p.setFeature(Parser.defaultAttributesFeature, false);
            } catch (Exception ignore) {
            }
            return p;
        }
    };

    public static String extractText(String html) throws IOException, SAXException {
        Parser p = parser.get();
        ExtractTextHandler h = new ExtractTextHandler();
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getText();
    }

}
