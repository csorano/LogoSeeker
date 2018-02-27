package imt.logoseeker;

import java.io.Serializable;

class Brand implements Serializable {
    private String name;
    private String url;
    private String classifier;
    private String[] images;

    Brand(String name, String url, String classifier, String[] images)
    {
        this.name = name;
        this.url = url;
        this.classifier = classifier;
        this.images = images;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getClassifierName() {
        return classifier;
    }

    String getUrl() {
        return url;
    }

    String[] getImages() {
        return images;
    }
}
