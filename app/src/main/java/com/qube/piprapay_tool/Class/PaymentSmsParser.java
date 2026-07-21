package com.qube.piprapay_tool.Class;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaymentSmsParser {

    public static String getSummary(String sender, String text) {
        try {
            // Amount
            String amount = extract(text, "Tk\\s*([\\d,.]+)");
            if (amount == null) return "New SMS from " + sender; // Not a standard payment SMS

            // TrxID
            String trxId = extract(text, "TrxID\\s+([A-Z0-9]+)");
            if (trxId == null) trxId = "N/A";

            // Reference
            String ref = extract(text, "Ref\\s+([^.]+)");
            String refStr = ref != null ? " - Ref: " + ref : "";

            // From (sender number or name)
            String from = "Unknown";
            Matcher fromMatcher = Pattern.compile("from\\s+(\\d{11})").matcher(text);
            if (fromMatcher.find()) {
                from = fromMatcher.group(1);
            } else {
                Matcher bankMatcher = Pattern.compile("from\\s+([A-Za-z\\s]+Bank[^.]*)").matcher(text);
                if (bankMatcher.find()) {
                    from = bankMatcher.group(1).trim();
                } else {
                    String fallbackFrom = extract(text, "from\\s+([^.]+)");
                    if (fallbackFrom != null) {
                        from = fallbackFrom.replace("iBanking of Tk", "").trim();
                    }
                }
            }

            // Balance
            String balance = extract(text, "Balance\\s*Tk\\s*([\\d,.]+)");
            String balStr = balance != null ? " - Bal: " + balance : "";

            // bkash - 250 tk - trxid - referenec (if found) - sender number - total balance
            return sender + " - " + amount + "Tk - " + trxId + refStr + " - " + from + balStr;

        } catch (Exception e) {
            return "New SMS from " + sender;
        }
    }

    private static String extract(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}
