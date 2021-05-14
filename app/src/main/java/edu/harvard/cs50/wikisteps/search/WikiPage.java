package edu.harvard.cs50.wikisteps.search;

public class WikiPage {

    // store checked pages as objects with adjacent titles
    private String title;
    private String previousPage;
    private String nextPage;

    public WikiPage(String title) {
        this.title = title;
    }

    public String getPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(String previousPage) {
        this.previousPage = previousPage;
    }

    public String getNextPage() {
        return nextPage;
    }

    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "WikiPage{" +
                "title='" + title + '\'' +
                ", previousPage='" + previousPage + '\'' +
                ", nextPage='" + nextPage + '\'' +
                '}';
    }
}
