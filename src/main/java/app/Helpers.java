package app;

import io.javalin.http.Context;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Random;

public class Helpers {

    // equivalent of getValueFromCookie
    static String getValueFromCookie(String cookieStr, String name) {
        String prefix = name + "=";
        int startOfPair = cookieStr.indexOf(name);
        if (startOfPair == -1) return "";

        int startOfValue = startOfPair + prefix.length();
        int endOfBoth = cookieStr.indexOf(";", startOfPair);

        // if no semicolon found, take till end of string
        // in C++ substr with npos does this automatically
        if (endOfBoth == -1) return cookieStr.substring(startOfValue);
        return cookieStr.substring(startOfValue, endOfBoth);
    }

    // equivalent of is_admin
    static boolean isAdmin(Context ctx, String secretKey) {
        String cookieStr = ctx.header("Cookie");
        if (cookieStr == null) return false;
        String sessionId = getValueFromCookie(cookieStr, "session_id");
        return sessionId.equals(secretKey);
    }

    // equivalent of custom_redirect — void because Javalin is response-by-mutation
    static void customRedirect(Context ctx, String path) {
        ctx.status(303).redirect(path);
    }

    // equivalent of getFormattedTime
    // C++ took time_t (seconds since epoch), same as Java's long here
    static String getFormattedTime(long t) {
        LocalDateTime dt = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(t),
            ZoneId.systemDefault()
        );

        int day = dt.getDayOfMonth();
        String suffix = "th";
        if      (day % 10 == 1 && day != 11) suffix = "st";
        else if (day % 10 == 2 && day != 12) suffix = "nd";
        else if (day % 10 == 3 && day != 13) suffix = "rd";

        // equivalent of put_time("%H:%M") + day + suffix + put_time("%b")
        String time  = dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        String month = dt.format(DateTimeFormatter.ofPattern("MMM"));

        return time + " " + day + suffix + " " + month;
    }

    // equivalent of random_id
    static int randomId() {
        Random rand = new Random();
        return rand.nextInt(900000) + 100000;  // range [100000, 999999]
    }
}