package presentation;

import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class MySession {
    private Map<String, Object> map = new HashMap<>();
    private HttpSession httpSession;

    public MySession(HttpSession httpSession) {
        this.httpSession = httpSession;
        
        if (httpSession != null) {
            Enumeration<String> noms = httpSession.getAttributeNames();
            while (noms.hasMoreElements()) {
                String key = noms.nextElement();
                map.put(key, httpSession.getAttribute(key));
            }
        }
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public void setHttpSession(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    public void set(String key, Object value) {
        map.put(key, value);
        if (httpSession != null) {
            httpSession.setAttribute(key, value);
        }
    }

    public Object get(String key) {
        if (httpSession != null && httpSession.getAttribute(key) != null) {
            return httpSession.getAttribute(key);
        }
        return map.get(key);
    }

    public void delete(String key) {
        map.remove(key);
        if (httpSession != null) {
            httpSession.removeAttribute(key);
        }
    }
}