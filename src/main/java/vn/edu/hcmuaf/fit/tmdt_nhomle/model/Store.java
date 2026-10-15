package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

public class Store {
    private final int id;
    private final String name;
    private final String description;
    private final String specialty;
    private final String location;
    private final double rating;
    private final int completedOrders;

    public Store(int id, String name, String description, String specialty,
                 String location, double rating, int completedOrders) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.specialty = specialty;
        this.location = location;
        this.rating = rating;
        this.completedOrders = completedOrders;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSpecialty() { return specialty; }
    public String getLocation() { return location; }
    public double getRating() { return rating; }
    public int getCompletedOrders() { return completedOrders; }
}
