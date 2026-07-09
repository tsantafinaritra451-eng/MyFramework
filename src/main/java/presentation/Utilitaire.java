package presentation;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mg.itu.tsanta.annotation.Controller;
import mg.itu.tsanta.annotation.Url;

public class Utilitaire {

 
    public static Map<UrlMethod, Mapping> getMappings(String packageCible) throws Exception {
        Map<UrlMethod, Mapping> mappingUrls = new HashMap<>();
        List<Class<?>> classesControllers = getClassesWithAnnotation(packageCible, Controller.class);
        
        if (classesControllers != null) {
            for (Class<?> classe : classesControllers) {
                Method[] methods = classe.getDeclaredMethods();
                for (Method methode : methods) {
                    if (methode.isAnnotationPresent(Url.class)) {
                        Url urlMapping = methode.getAnnotation(Url.class);
                        String url = urlMapping.value();
                        
                        String httpMethod = urlMapping.method().toUpperCase();
                        
                        UrlMethod urlMethod = new UrlMethod(url, httpMethod);

                        if (mappingUrls.containsKey(urlMethod)) {
                            throw new Exception("Mapping dupliqué détecté pour l'URL : " + url + " [" + httpMethod + "]");
                        }

                        Mapping mapping = new Mapping(classe, methode);
                        mappingUrls.put(urlMethod, mapping);
                    }
                }
            }
        }
        return mappingUrls;
    }

   
    public static List<Class<?>> getClassesWithAnnotation(String nomPackage, Class<? extends Annotation> annotationClass) {
        List<Class<?>> listeClasse = getAllClasse(nomPackage);
        List<Class<?>> classWithAnnotation = new ArrayList<>();
        for (Class<?> classe : listeClasse) {
            if (classe.isAnnotationPresent(annotationClass)) {
                classWithAnnotation.add(classe);
            }
        }
        return classWithAnnotation;
    }

  
    public static List<Class<?>> getAllClasse(String nomPackage) {
        List<Class<?>> listClass = new ArrayList<>();
        String path = nomPackage.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            return listClass;
        }
        try {
            File directory = new File(resource.toURI());
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String classNameWithoutEnd = file.getName().replace(".class", "");
                        String fullClassName = nomPackage + "." + classNameWithoutEnd;
                        Class<?> classe = Class.forName(fullClassName);
                        listClass.add(classe);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listClass;
    }
}