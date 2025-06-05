package shared;

public class XMLMessageBuilder {

    public static String buildJoinMessage(String sender, String mood) {
        return "<message type=\"join\"><from>" + escape(sender) + "</from><mood>" + escape(mood) + "</mood></message>";
    }

    public static String buildLeaveMessage(String sender) {
        return "<message type=\"leave\"><from>" + escape(sender) + "</from></message>";
    }

    public static String buildBroadcastMessage(String sender, String body) {
        return "<message type=\"broadcast\"><from>" + escape(sender) + "</from><body>" + escape(body) + "</body></message>";
    }

    public static String buildPrivateMessage(String sender, String receiver, String body) {
        return "<message type=\"private\"><from>" + escape(sender) + "</from><to>" + escape(receiver) + "</to><body>" + escape(body) + "</body></message>";
    }

   public static String buildHugMessage(String sender, String receiver) {
        String body = sender + " надіслав обійми 🤗 до " + receiver;
        return "<message type=\"hug\"><from>" + escape(sender) + "</from><to>" + escape(receiver) + "</to><body>" + escape(body) + "</body></message>";
    }

   public static String buildRenameMessage(String oldName, String newName) {
        return "<message type=\"rename\"><from>" + escape(oldName) + "</from><body>" + escape(oldName + "=>" + newName) + "</body></message>";
    }

   public static String buildMoodChangeMessage(String sender, String mood) {
        return "<message type=\"mood\"><from>" + escape(sender) + "</from><mood>" + escape(mood) + "</mood></message>";
    }

    public static String buildFileMessage(String sender, String receiver, String filename, String filedata) {
        return "<message type=\"file\">" +
                   "<from>" + escape(sender) + "</from>" +
                   "<to>" + escape(receiver) + "</to>" +
                   "<filename>" + escape(filename) + "</filename>" +
                   "<filedata>" + escape(filedata) + "</filedata>" +
               "</message>";
    }

   public static String buildEmotionMessage(String sender, String receiver, String emoji) {
        return "<message type=\"emotion\">" +
                   "<from>" + escape(sender) + "</from>" +
                   "<to>" + escape(receiver) + "</to>" +
                   "<body>" + escape(emoji) + "</body>" +
               "</message>";
    }

   public static String parseType(String xml) {
        return parseAttributeValue(xml, "type");
    }

   public static String parseSender(String xml) {
        return parseTagContent(xml, "from");
    }

    public static String parseReceiver(String xml) {
        return parseTagContent(xml, "to");
    }

   public static String parseBody(String xml) {
        return parseTagContent(xml, "body");
    }

   public static String parseMood(String xml) {
        return parseTagContent(xml, "mood");
    }

   public static String parseFileName(String xml) {
        return parseTagContent(xml, "filename");
    }

   public static String parseFileContent(String xml) {
        return parseTagContent(xml, "filedata");
    }

    private static String parseAttributeValue(String xml, String attribute) {
        String start = attribute + "=\"";
        int startIndex = xml.indexOf(start);
        if (startIndex >= 0) {
            int valueStart = startIndex + start.length();
            int valueEnd = xml.indexOf("\"", valueStart);
            if (valueEnd > valueStart) {
                return xml.substring(valueStart, valueEnd);
            }
        }
        return "";
    }

    private static String parseTagContent(String xml, String tag) {
        String startTag = "<" + tag + ">";
        String endTag = "</" + tag + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start >= 0 && end > start) {
            return unescape(xml.substring(start + startTag.length(), end));
        }
        return "";
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String unescape(String text) {
        if (text == null) return "";
        return text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
    }
}
