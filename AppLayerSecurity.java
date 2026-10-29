import com.sun.net.httpserver.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

// Layer 7 protection: rate limiting, size limit, HMAC integrity,
// replay protection, whitelist validation, output encoding, generic errors.
public class AppLayerSecurity {
    static final int PORT = 8081, MAX_BODY = 2048, LIMIT_PER_MIN = 20;
    static final long WINDOW_MS = 30_000;                  // freshness window
    static final byte[] KEY = new byte[32];                // from a key vault in production
    static final Map<String, Long> nonces = new ConcurrentHashMap<>();
    static final Map<String, long[]> rate = new ConcurrentHashMap<>();
    static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{0,49}$");
    static final Pattern EMAIL = Pattern.compile("^[\\w.%+-]{1,64}@[\\w.-]{1,180}\\.[A-Za-z]{2,10}$");

    public static void main(String[] args) throws Exception {
        new SecureRandom().nextBytes(KEY);
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        s.createContext("/register", AppLayerSecurity::handle);
        s.start();
        demo();
        s.stop(0);
    }

    static void handle(HttpExchange ex) throws java.io.IOException {
        try {
            String ip = ex.getRemoteAddress().getAddress().getHostAddress();
            if (!allow(ip)) { reply(ex, 429, "Too many requests"); return; }
            byte[] raw = ex.getRequestBody().readNBytes(MAX_BODY + 1);
            if (raw.length > MAX_BODY) { reply(ex, 413, "Request too large"); return; }
            String body = new String(raw, StandardCharsets.UTF_8);

            Headers h = ex.getRequestHeaders();
            String ts = h.getFirst("X-Timestamp"), nonce = h.getFirst("X-Nonce"), sig = h.getFirst("X-Signature");
            if (ts == null || nonce == null || sig == null
                    || !MessageDigest.isEqual(sig.getBytes(), sign(ts + "|" + nonce + "|" + body).getBytes())) {
                reply(ex, 401, "Unauthorized"); return;           // tampered or unsigned
            }
            if (!fresh(ts, nonce)) { reply(ex, 401, "Unauthorized"); return; }   // replayed

            Map<String, String> f = new HashMap<>();
            for (String p : body.split("&")) {                      // parse, never execute
                String[] kv = p.split("=", 2);
                if (kv.length == 2) f.putIfAbsent(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
            String name = f.get("name"), email = f.get("email");
            if (name == null || email == null || !NAME.matcher(name).matches() || !EMAIL.matcher(email).matches()) {
                reply(ex, 422, "Invalid input"); return;            // whitelist failed
            }
            reply(ex, 201, "<p>Welcome, " + encode(name) + "</p>");
        } catch (Exception e) {
            System.err.println("internal error: " + e);            // details stay in the server log
            reply(ex, 500, "Something went wrong");
        }
    }

    static boolean allow(String ip) {
        long now = System.currentTimeMillis();
        long[] w = rate.computeIfAbsent(ip, k -> new long[]{now, 0});
        synchronized (w) {
            if (now - w[0] > 60_000) { w[0] = now; w[1] = 0; }
            return ++w[1] <= LIMIT_PER_MIN;
        }
    }

    static boolean fresh(String ts, String nonce) {
        long now = System.currentTimeMillis();
        try { if (Math.abs(now - Long.parseLong(ts)) > WINDOW_MS) return false; }
        catch (NumberFormatException e) { return false; }
        nonces.values().removeIf(t -> now - t > 2 * WINDOW_MS);
        return nonces.putIfAbsent(nonce, now) == null;               // each nonce works once
    }

    static String sign(String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(msg.getBytes(StandardCharsets.UTF_8)));
    }

    static String encode(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }

    static void reply(HttpExchange ex, int code, String body) throws java.io.IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "text/html; charset=utf-8");
        h.set("Content-Security-Policy", "default-src 'none'");
        h.set("X-Content-Type-Options", "nosniff");
        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }

    // ---- demo client: one good request, then attacks ----
    static final HttpClient C = HttpClient.newBuilder().proxy(ProxySelector.of(null)).build();

    static HttpRequest req(String body, long ts, String nonce, String sig) throws Exception {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + PORT + "/register"))
                .header("X-Timestamp", "" + ts).header("X-Nonce", nonce)
                .header("X-Signature", sig != null ? sig : sign(ts + "|" + nonce + "|" + body))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    static void send(String label, HttpRequest r) throws Exception {
        HttpResponse<String> res = C.send(r, HttpResponse.BodyHandlers.ofString());
        System.out.printf("%-30s -> %d %s%n", label, res.statusCode(), res.body());
    }

    static void demo() throws Exception {
        String good = "name=Ananya+Solanki&email=ananya%40example.edu";
        long now = System.currentTimeMillis();
        String n = UUID.randomUUID().toString();
        HttpRequest valid = req(good, now, n, null);
        send("1. valid signed request", valid);
        send("2. replay of request 1", valid);
        send("3. body changed in transit", req(good.replace("Ananya", "Mallory"), now, "x1", sign(now + "|x1|" + good)));
        send("4. old captured timestamp", req(good, now - 120_000, "x2", null));
        send("5. SQL injection", req("name=x%27+OR+%271%27%3D%271&email=a%40b.com", now, "x3", null));
        send("6. script injection", req("name=%3Cscript%3Ealert(1)%3C%2Fscript%3E&email=a%40b.com", now, "x4", null));
        send("7. oversized body", req("name=" + "A".repeat(5000), now, "x5", null));
        int blocked = 0;
        for (int i = 0; i < 25; i++)
            if (C.send(req(good, System.currentTimeMillis(), "f" + i, null),
                    HttpResponse.BodyHandlers.ofString()).statusCode() == 429) blocked++;
        System.out.printf("%-30s -> %d of 25 blocked (429)%n", "8. flood of 25 requests", blocked);
    }
}
