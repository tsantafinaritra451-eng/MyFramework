package presentation;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
                            throw new Exception(
                                    "Mapping dupliqué détecté pour l'URL : " + url + " [" + httpMethod + "]");
                        }

                        Mapping mapping = new Mapping(classe, methode);
                        mappingUrls.put(urlMethod, mapping);
                    }
                }
            }
        }
        return mappingUrls;
    }

    public static List<Class<?>> getClassesWithAnnotation(String nomPackage,
            Class<? extends Annotation> annotationClass) {
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

    public static String SauvegardeByte(Object mapFileInstance, String subFolder) {
        if (mapFileInstance == null) {
            System.out.println("DEBUG: mapFileInstance est null");
            return null;
        }

        Properties prop = new Properties();

        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("upload.properties")) {
            if (input == null) {
                System.out.println("DEBUG: upload.properties introuvable dans le classpath !");
                return null;
            }
            prop.load(input);

            String variablePath = prop.getProperty("upload.path");
            System.out.println("DEBUG: upload.path récupéré = " + variablePath);
            if (variablePath == null || variablePath.trim().isEmpty()) {
                System.out.println("DEBUG: upload.path est vide ou null dans le fichier properties !");
                return null;
            }

            Method getNomMethod = mapFileInstance.getClass().getMethod("getNom");
            Method getBytesMethod = mapFileInstance.getClass().getMethod("getBytes");

            String originalName = (String) getNomMethod.invoke(mapFileInstance);
            byte[] fileBytes = (byte[]) getBytesMethod.invoke(mapFileInstance);

            if (fileBytes == null || fileBytes.length == 0) {
                System.out.println("DEBUG: fileBytes est vide ou null !");
                return null;
            }
            
            String chemin = variablePath
                    + (subFolder != null && !subFolder.isEmpty() ? File.separator + subFolder : "");
            File uploadDir = new File(chemin);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                System.out.println("DEBUG: Création du dossier " + chemin + " -> " + created);
            }

            String uniqueFileName = System.currentTimeMillis() + "_" + originalName;
            Path destinationPath = Paths.get(chemin, uniqueFileName);

            java.nio.file.Files.write(destinationPath, fileBytes);
            System.out.println("DEBUG: Fichier écrit avec succès sur le disque : " + destinationPath);

            return uniqueFileName;
        } catch (Exception e) {
            System.out.println("DEBUG: EXCEPTION CAPTURÉE DANS SauvegardeByte :");
            e.printStackTrace();
            return null;
        }
    }
}