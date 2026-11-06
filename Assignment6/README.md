# Assignment 6: Generic Classes and Vehicle Inheritance in Java

MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.

## Files

- `VehicleDemo.java`: Question 2. A `Vehicle` parent class with `forward()` and `reverse()`, and three subclasses (`SUV`, `SportsCar`, `Hybrid`) that override both methods. Every speed change goes through the parent's `final drive()` method, so no subclass can skip or loosen the parent's input check or its hard speed limits.
- `GenericsDemo.java`: Question 1 example. A generic `Garage<T extends Vehicle>` shows the useful side of generics (one reusable class, checked by the compiler, no casts) and the dangerous side (type erasure, and a raw type that lets a `SportsCar` into a `Garage<SUV>`, causing a `ClassCastException` later).
- `assignment6_output.txt`: the real compiler warnings and program output.

## Run

```
javac -Xlint:unchecked -Xlint:rawtypes VehicleDemo.java GenericsDemo.java
java VehicleDemo
java GenericsDemo
```

The two compiler warnings for `GenericsDemo.java` are expected. They are the compiler's only signal that the raw type is unsafe.

GitHub Actions compiles and runs both programs on every push (`.github/workflows/build.yml`).
