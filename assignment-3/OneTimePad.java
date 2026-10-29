import java.security.SecureRandom;

// One-time pad over a 27-symbol alphabet (A-Z plus space), so word lengths are hidden too.
// c = (p + k) mod 27, p = (c - k) mod 27. Key: random, same length as the message, used once.
public class OneTimePad {
    static final String A = "ABCDEFGHIJKLMNOPQRSTUVWXYZ ";
    static final int N = A.length();

    static int[] toNums(String s) {
        int[] out = new int[s.length()];
        for (int i = 0; i < s.length(); i++) out[i] = A.indexOf(s.charAt(i));
        return out;
    }

    static String toText(int[] v) {
        StringBuilder sb = new StringBuilder();
        for (int x : v) sb.append(A.charAt(x));
        return sb.toString().replace(' ', '_');        // show spaces as _ in the output
    }

    static int[] combine(int[] a, int[] b, int sign) {  // sign +1 = encrypt, -1 = decrypt
        int[] out = new int[a.length];
        for (int i = 0; i < a.length; i++) out[i] = Math.floorMod(a[i] + sign * b[i], N);
        return out;
    }

    public static void main(String[] args) {
        String msg = "MY NAME IS UNKNOWN";
        int[] p = toNums(msg);
        int[] key = new int[p.length];
        SecureRandom rng = new SecureRandom();
        for (int i = 0; i < key.length; i++) key[i] = rng.nextInt(N);

        int[] c = combine(p, key, +1);
        System.out.println("Plaintext : " + toText(p));
        System.out.println("Key       : " + toText(key));
        System.out.println("Ciphertext: " + toText(c));
        System.out.println("Decrypted : " + toText(combine(c, key, -1)));

        // Perfect secrecy: some key turns the same ciphertext into any other 18-symbol message.
        int[] fakeKey = combine(c, toNums("WE MEET AT NOON OK"), -1);
        System.out.println("With key " + toText(fakeKey) + " it decrypts to " + toText(combine(c, fakeKey, -1)));
        java.util.Arrays.fill(key, 0);                  // discard the key after use
    }
}
