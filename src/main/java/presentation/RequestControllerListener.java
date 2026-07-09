package presentation;

import java.util.Map;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class RequestControllerListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
         ServletContext servletContext = sce.getServletContext();

         String packageCible = servletContext.getInitParameter("packageToScan");
         if (packageCible == null || packageCible.isEmpty()) {
            throw new RuntimeException("Le paramètre 'packageToScan' n'est pas défini.");
         }
         
         try {
            Map<UrlMethod, Mapping> mappingUrls = Utilitaire.getMappings(packageCible);
            servletContext.setAttribute("mappingUrls", mappingUrls);
            System.out.println("Mapping initialisé avec succès pour le package : " + packageCible);
         } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'initialisation des mappings", e);
         }
    }
}