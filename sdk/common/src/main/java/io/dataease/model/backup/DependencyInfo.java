package io.dataease.model.backup;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class DependencyInfo {
    private boolean hasDependencies;
    private Dependencies dependencies;

    public DependencyInfo() {
        this.dependencies = new Dependencies();
    }

    @Data
    public static class Dependencies {
        private List<ResourceItem> datasources = new ArrayList<>();
        private List<ResourceItem> datasets = new ArrayList<>();
    }

    @Data
    public static class ResourceItem {
        private String id;
        private String name;

        public ResourceItem() {}

        public ResourceItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResourceItem that = (ResourceItem) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
