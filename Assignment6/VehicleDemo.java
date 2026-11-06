/*
 * Assignment 6, Question 2
 * MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.
 *
 * Vehicle is the parent class with forward() and reverse(). SUV, SportsCar,
 * and Hybrid extend Vehicle and override both methods. Every speed change
 * goes through the parent's final drive() method, so no subclass can skip
 * or loosen the parent's safety checks; a subclass can only be stricter.
 */
class Vehicle {
    static final int MAX_FORWARD_MPH = 160; // hard limit for every vehicle
    static final int MAX_REVERSE_MPH = 10;  // hard limit for every vehicle

    private final String name;
    private int speed; // mph; positive = forward, negative = reverse

    Vehicle(String name) {
        this.name = name;
    }

    public void forward(int mph) {
        drive(mph, 100, false, "rolls forward");
    }

    public void reverse(int mph) {
        drive(mph, MAX_REVERSE_MPH, true, "backs up");
    }

    // final: subclasses cannot override this method, so they cannot remove
    // the input check or raise the parent's hard limits.
    protected final void drive(int mph, int limit, boolean backward, String action) {
        if (mph < 0 || mph > 200) {
            throw new IllegalArgumentException(
                "Rejected " + mph + " mph: speed must be between 0 and 200");
        }
        int hardLimit = backward ? MAX_REVERSE_MPH : MAX_FORWARD_MPH;
        int applied = Math.min(mph, Math.min(limit, hardLimit));
        speed = backward ? -applied : applied;
        String capped = (applied < mph) ? " (capped from " + mph + ")" : "";
        System.out.println(name + " " + action + " at " + applied + " mph" + capped);
    }

    public int getSpeed() {
        return speed;
    }

    public String getName() {
        return name;
    }
}

class SUV extends Vehicle {
    SUV() {
        super("SUV");
    }

    @Override
    public void forward(int mph) {
        System.out.print("[all-wheel drive engaged] ");
        drive(mph, 90, false, "climbs forward");
    }

    @Override
    public void reverse(int mph) {
        System.out.print("[rear camera on] ");
        drive(mph, 8, true, "reverses carefully");
    }
}

class SportsCar extends Vehicle {
    SportsCar() {
        super("SportsCar");
    }

    @Override
    public void forward(int mph) {
        System.out.print("[sport mode] ");
        drive(mph, 160, false, "accelerates forward");
    }

    @Override
    public void reverse(int mph) {
        System.out.print("[launch control disabled] ");
        drive(mph, 5, true, "creeps backward");
    }
}

class Hybrid extends Vehicle {
    Hybrid() {
        super("Hybrid");
    }

    @Override
    public void forward(int mph) {
        System.out.print(mph <= 25 ? "[electric motor] " : "[gas and electric] ");
        drive(mph, 110, false, "glides forward");
    }

    @Override
    public void reverse(int mph) {
        System.out.print("[electric motor only] ");
        drive(mph, 6, true, "reverses silently");
    }
}

public class VehicleDemo {
    public static void main(String[] args) {
        Vehicle[] fleet = { new Vehicle("Vehicle"), new SUV(), new SportsCar(), new Hybrid() };

        // Polymorphism: the same two calls run each class's own version.
        for (Vehicle v : fleet) {
            v.forward(60);
            v.reverse(12);
            System.out.println();
        }

        // Each override adds its own behavior.
        new Hybrid().forward(20);

        // The parent's hard limit applies even to the fastest subclass.
        new SportsCar().forward(190);

        // The parent's validation still protects every subclass.
        try {
            new SportsCar().forward(-30);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
