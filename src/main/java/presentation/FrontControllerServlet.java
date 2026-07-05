package presentation;

import java.io.IOException;
<<<<<<< Updated upstream
import java.util.ArrayList;
import java.util.List;
=======
import java.io.PrintWriter;
import java.util.HashMap; // AJOUTÉ
import java.util.List;
import java.util.Map;     // AJOUTÉ
import java.lang.reflect.Method;
>>>>>>> Stashed changes

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import mg.itu.tsanta.annotation.Controller;

public class FrontControllerServlet extends HttpServlet {
        public FrontControllerServlet() {
                super();
        }

<<<<<<< Updated upstream
        private List<String> listControllers = new ArrayList<>();
=======
        private Map<UrlMethod, Mapping> listUrl = new HashMap<>();
>>>>>>> Stashed changes

        @Override
        public void init() throws ServletException {
                try {
                        List<String> toutesClasses = Utilitaire.ScanneClass("controllers");
                        for (String className : toutesClasses) {
                                Class<?> clazz = Class.forName(className);
                                if (clazz.isAnnotationPresent(Controller.class)) {
<<<<<<< Updated upstream
                                        this.listControllers.add(className);
=======
                                        for (Method toutMethode : toutesMethodes) {
                                                if (toutMethode.isAnnotationPresent(Url.class)) {
                                                        Url annotation = toutMethode.getAnnotation(Url.class);
                                                        String route = annotation.value();
                                                        
                                                        UrlMethod key = new UrlMethod(route, "GET"); 
                                                        Mapping value = new Mapping(clazz.getName(), toutMethode.getName());
                                                        
                                                        this.listUrl.put(key, value);
                                                }
                                        }
>>>>>>> Stashed changes
                                }
                        }
                        
                        System.out.println("Framework OK " + this.listControllers.size());                  
                        
                } catch (Exception e) {
                        throw new ServletException("Erreur initialisation", e);
                }
        }

        protected void processRequest(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                response.setContentType("text/plain");
<<<<<<< Updated upstream
                String url = request.getRequestURL().toString();

                response.getWriter().println("URL demandée : " + url);
                response.getWriter().println("--- Liste des contrôleurs détectés par le framework ---");

                for (String ctrl : this.listControllers) {
                        response.getWriter().println("- " + ctrl);
                }
=======
                PrintWriter out = response.getWriter();
                String url = request.getPathInfo();
                if (url == null) url = "/";
                
                String methodHttp = request.getMethod(); 

                UrlMethod cleRecherche = new UrlMethod(url, methodHttp);
                Mapping mappingTrouve = listUrl.get(cleRecherche);

                if (mappingTrouve != null) {
                        out.println("Url trouver");
                        out.println("methode:" + mappingTrouve.getMethod());
                        out.println("class:" + mappingTrouve.getClassName());
                        return;
                }
                
                out.println("cette url " + url + " avec la methode " + methodHttp + " ne contient pas annotation");
                out.println("les url disponible avec leur methode et classe sonr:");

                for (Map.Entry<UrlMethod, Mapping> entry : listUrl.entrySet()) {
                        UrlMethod urlDiso = entry.getKey();
                        Mapping mappingDispo = entry.getValue();

                        out.println("   URL      : " + urlDiso.getUrl() + " [" + urlDiso.getMethodHttp() + "]");
                        out.println("   Methode   : " + mappingDispo.getMethod());
                        out.println("   Class  : " + mappingDispo.getClassName());
                }
>>>>>>> Stashed changes
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                processRequest(request, response);
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                processRequest(request, response);
        }
}