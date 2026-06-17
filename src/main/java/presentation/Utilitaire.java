package src.main.java.presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

public class Utilitaire {
    public static List<String> ScanneClass(String packageName) throws Exception{
        List<String> classes = new ArrayList<>();

        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);

        if(resource == null){
            return classes;
        }

        File directory = new File(resource.getFile());
        if (directory.exists() && directory.isDirectory()){
            File[] files = directory.listFiles();
            if(files != null){
                for(File file: files){
                    if(file.isFile() && file.getName().endsWith(".class")){
                        String className = packageName +"."+file.getName().substring(0,file.getName().length() -6);
                        classes.add(className);
                    }
                }
            }
        }
        return classes;
    }
    
}
