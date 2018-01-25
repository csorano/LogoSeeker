package imt.logoseeker;

/**
 * Created by Clément on 21/01/2018.
 */

public class Brand {
    private String name;
    private String url;
    private String classifier;
    private String[] images;

    public Brand() {

    }

    public Brand(String name, String url, String classifier, String[] images)
    {
        this.name = name;
        this.url = url;
        this.classifier = classifier;
        this.images = images;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClassifierName() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String[] getImages() {
        return images;
    }

    public void setImages(String[] images) {
        this.images = images;
    }

}
