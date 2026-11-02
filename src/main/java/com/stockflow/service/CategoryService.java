package com.stockflow.service;

import com.stockflow.dao.CategoryDAO;
import com.stockflow.exception.ValidationException;
import com.stockflow.model.Category;
import com.stockflow.model.Permission;
import com.stockflow.util.Check;
import com.stockflow.util.Session;

import java.util.List;

public class CategoryService {
    private final CategoryDAO dao = new CategoryDAO();

    /** Used by product forms and filters: any role that can see products may read categories. */
    public List<Category> search(String q, boolean includeInactive) {
        if (!Session.can(Permission.CATEGORIES)) Session.require(Permission.PRODUCTS_VIEW);
        return dao.search(q, includeInactive);
    }

    public void save(Category c) {
        Session.require(Permission.CATEGORIES);
        c.setName(Check.maxLen(Check.required(c.getName(), "Category name"), 80, "Category name"));
        c.setDescription(Check.maxLen(Check.blankToNull(c.getDescription()), 255, "Description"));
        if (dao.nameExists(c.getName(), c.getId())) throw new ValidationException("A category with this name already exists.");
        if (c.getId() == 0) {
            c.setId(dao.insert(c));
            ActivityService.log("Category added", c.getName());
        } else {
            dao.update(c);
            ActivityService.log("Category updated", c.getName());
        }
    }

    public void setActive(Category c, boolean active) {
        Session.require(Permission.CATEGORIES);
        if (!active && c.getProductCount() > 0) {
            throw new ValidationException("This category still has " + c.getProductCount() + " active product(s). Move or deactivate them first.");
        }
        dao.setActive(c.getId(), active);
        ActivityService.log(active ? "Category activated" : "Category deactivated", c.getName());
    }
}
