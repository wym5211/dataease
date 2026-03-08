package io.dataease.model;

import lombok.Data;

import java.util.List;

@Data
public class TreeModel<T extends TreeBaseModel<?>> {

    private T data;

    private List<TreeModel<T>> children;

    public TreeModel(T data) {
        this.data = data;
    }

    public Long getId() {
        return data.getId();
    }

    public Long getPid() {
        return data.getPid();
    }

    public String getName() {
        return data.getName();
    }
}
