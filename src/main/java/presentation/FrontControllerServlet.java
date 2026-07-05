package presentation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import mg.itu.tsanta.annotation.Controller;
import mg.itu.tsanta.annotation.Url;

public class FrontControllerServlet extends HttpServlet {
        public FrontControllerServlet() {
                super();
        }

        private Map<UrlMethod, Mapping> listUrl = new HashMap<>();

        @Override
        public void init() throws ServletException {
                try {
                        List<String> toutesClasses = Utilitaire.ScanneClass("controllers");

                        for (String className : toutesClasses) {
                                Class<?> clazz = Class.forName(className);
                                Method[] toutesMethodes = clazz.getDeclaredMethods();
                                if (clazz.isAnnotationPresent(Controller.class)) {
                                        for (Method toutMethode : toutesMethodes) {
                                                if (toutMethode.isAnnotationPresent(Url.class)) {
                                                        Url annotation = toutMethode.getAnnotation(Url.class);
                                                        String route = annotation.value();

                                                        UrlMethod key = new UrlMethod(route, "GET");
                                                        Mapping value = new Mapping(clazz.getName(),
                                                                        toutMethode.getName());

                                                        this.listUrl.put(key, value);
                                                }
                                        }
                                }
                        }

                        System.out.println("Framework OK " + this.listUrl.size());

                } catch (Exception e) {
                        throw new ServletException("Erreur initialisation", e);
                }
        }

        protected void processRequest(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                response.setContentType("text/plain");
                PrintWriter out = response.getWriter();
                String url = request.getPathInfo();
                if (url == null)
                        url = "/";

                String methodHttp = request.getMethod();

                UrlMethod cleRecherche = new UrlMethod(url, methodHttp);
                Mapping mappingTrouve = listUrl.get(cleRecherche);

                if (mappingTrouve != null) {
                        try {
                                Class<?> clazz = Class.forName(mappingTrouve.getClassName());

                                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

                                Method methodeAExecuter = clazz.getDeclaredMethod(mappingTrouve.getMethod());

                                Object resultat = methodeAExecuter.invoke(controllerInstance);

                                
                                if (resultat != null) {
                                        out.println("Résultat de l'exécution : " + resultat.toString());
                                }

                                return;

                        } catch (Exception e) {
                                throw new ServletException("Erreur lors de l'exécution du contrôleur : "
                                                + mappingTrouve.getClassName(), e);
                        }
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