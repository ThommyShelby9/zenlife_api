package com.api.expo.dto;

import java.util.List;

public class CategoryData {
    private String category;
    private List<SkillData> skills;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<SkillData> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillData> skills) {
        this.skills = skills;
    }
}
