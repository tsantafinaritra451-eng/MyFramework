package presentation;


import java.util.Objects;

public class UrlMethod {
    private String url;
    private String methodHttp; 

    public UrlMethod(String url, String methodHttp) {
        this.url = url;
        this.methodHttp = methodHttp.toUpperCase(); 
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMethodHttp() { return methodHttp; }
    public void setMethodHttp(String methodHttp) { this.methodHttp = methodHttp.toUpperCase(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; 
        if (o == null || getClass() != o.getClass()) return false;
        
        UrlMethod that = (UrlMethod) o;
        return Objects.equals(url, that.url) && 
               Objects.equals(methodHttp, that.methodHttp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, methodHttp);
    }
}