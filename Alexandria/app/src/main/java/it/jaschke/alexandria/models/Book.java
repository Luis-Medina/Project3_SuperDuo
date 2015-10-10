package it.jaschke.alexandria.models;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Luis on 9/20/2015.
 */
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ean = "";
    private String title = "";
    private String subtitle = "";
    private String desc = "";
    private String imgUrl = "";
    private ArrayList<String> authors = new ArrayList<>();
    private ArrayList<String> categories = new ArrayList<>();

    public Book(String ean, String title, String subtitle, String desc, String imgUrl) {
        this.ean = ean;
        this.title = title;
        this.subtitle = subtitle;
        this.desc = desc;
        this.imgUrl = imgUrl;
    }

    public ArrayList<String> getAuthors() {
        return authors;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }
}
