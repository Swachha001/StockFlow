package com.stockflow.model;

public class Category {
    private int id;
    private String name, description;
    private boolean active = true;
    private int productCount;

    public Category() { }
    public Category(int id, String name) { this.id = id; this.name = name; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    @Override public String toString() { return name; }
}
