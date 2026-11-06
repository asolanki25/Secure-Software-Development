import java.util.ArrayList;
import java.util.List;

/*
 * Assignment 6, Question 1 (supporting example)
 * MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.
 *
 * Shows why generic classes are useful (one reusable, type-checked class)
 * and why they are dangerous (raw types and type erasure let the wrong
 * object in, and the failure appears later, far from the cause).
 * Compile together with VehicleDemo.java.
 */
class Garage<T extends Vehicle> {
    private final List<T> spaces = new ArrayList<>();

    void park(T vehicle) {
        spaces.add(vehicle);
    }

    T get(int index) {
        return spaces.get(index);
    }
}

public class GenericsDemo {
    public static void main(String[] args) {
        // Useful: one Garage class works for any Vehicle type, and the
        // compiler checks every use. No casts are needed.
        Garage<SUV> suvGarage = new Garage<>();
        suvGarage.park(new SUV());
        // suvGarage.park(new SportsCar());   // would not compile: wrong type
        SUV suv = suvGarage.get(0);
        System.out.println("Useful: retrieved " + suv.getName() + " with no cast");

        // Dangerous 1, type erasure: at run time both garages are the same class,
        // so the program cannot check what a garage is supposed to hold.
        Garage<Hybrid> hybridGarage = new Garage<>();
        System.out.println("Same runtime class: " + (suvGarage.getClass() == hybridGarage.getClass()));

        // Dangerous 2, raw type: the compiler only warns, and a SportsCar
        // gets into the SUV garage (heap pollution).
        Garage rawGarage = suvGarage;
        rawGarage.park(new SportsCar());
        System.out.println("Raw type accepted a SportsCar into Garage<SUV>");

        // The failure appears only when the object is read back.
        try {
            SUV wrong = suvGarage.get(1);
            System.out.println("Not reached: " + wrong.getName());
        } catch (ClassCastException e) {
            System.out.println("ClassCastException at read time: " + e.getMessage());
        }
    }
}
