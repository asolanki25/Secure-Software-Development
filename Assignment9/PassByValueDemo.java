/*
 * Assignment 9, Q2: How Java passes variables and objects.
 * MSCS-535 Secure Software Development, Fall 2026 - Ananya Solanki
 *
 * The core idea: Java is ALWAYS pass-by-value. For a primitive, the value
 * itself is copied. For an object, the value that is copied is the reference
 * (the handle to the object), not the object. So a method receives a copy of
 * the reference that still points to the same object. This produces three
 * behaviors the security developer must keep straight.
 */
public class PassByValueDemo {

    // A small mutable object used to show shared state.
    static class Account {
        String owner;
        int balance;
        Account(String owner, int balance) { this.owner = owner; this.balance = balance; }
    }

    // 1. Primitive passed by value: the method gets a copy of the number.
    static void tryToChangePrimitive(int amount) {
        amount = 999;                       // changes only the local copy
    }

    // 2. Object passed by value-of-reference: the copy points to the same object,
    //    so mutating a field through it changes the caller's object.
    static void mutateAccount(Account a) {
        a.balance = a.balance - 100;        // same object, visible to the caller
    }

    // 3. Reassigning the parameter does NOT change the caller's reference,
    //    which proves the reference was passed by value, not by reference.
    static void reassignAccount(Account a) {
        a = new Account("Attacker", 1_000_000);   // only the local copy now points here
        a.balance = 0;
    }

    public static void main(String[] args) {
        int balance = 500;
        System.out.println("Primitive before call: " + balance);
        tryToChangePrimitive(balance);
        System.out.println("Primitive after call:  " + balance + "  (unchanged - value was copied)");

        Account acct = new Account("Ananya", 500);
        System.out.println("\nObject before mutate:  " + acct.owner + " = " + acct.balance);
        mutateAccount(acct);
        System.out.println("Object after mutate:   " + acct.owner + " = " + acct.balance
                + "  (changed - same object was shared)");

        System.out.println("\nReference before reassign: " + acct.owner + " = " + acct.balance);
        reassignAccount(acct);
        System.out.println("Reference after reassign:  " + acct.owner + " = " + acct.balance
                + "  (unchanged - the reference itself was copied, so Java is pass-by-value)");
    }
}
