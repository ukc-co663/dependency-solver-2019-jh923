package depsolver;

import java.util.ArrayList;
import java.util.List;

class Package {
    private String name;
    private String version;
    private Integer size;
    private List<List<String>> dependsArray = new ArrayList<>();
    private List<String> conflictsArray = new ArrayList<>();

    public String getName() { return name; }

    public String getVersion() { return version; }

    public Integer getSize() { return size; }

    public List<List<String>> getDepends() { return dependsArray; }

    public List<String> getConflicts() { return conflictsArray; }

    public void setName(String name) { this.name = name; }

    public void setVersion(String version) { this.version = version; }

    public void setSize(Integer size) { this.size = size; }

    public void setDepends(List<List<String>> dependsArray) { this.dependsArray = dependsArray; }

    public void setConflicts(List<String> conflictsArray) { this.conflictsArray = conflictsArray; }

    public String getPackage() {
        String returnPackage = "Name " + getName() + "=" + getVersion() + "Size " + getSize() + "Dep : " + getDepends().toString() + "conflictsArray: " + getConflicts();
        return returnPackage;
    }
}