package presentation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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

        private List<Method> listUrl = new ArrayList<>();

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
                                             
                                                        this.listUrl.add(toutMethode);

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

                for (Method listeUrl : listUrl) {
                        Url annotation = listeUrl.getAnnotation(Url.class);
                        String route = annotation.value();
                        if (route.equals(url)) {
                                String nomClass = listeUrl.getDeclaringClass().getName();
                                String nomMethode = listeUrl.getName();

                                out.println("Url trouver");
                                out.println("methode:" + nomMethode);
                                out.println("class:" + nomClass);
                                return;

                        }

                }
                out.println("cette url" + url + "ne contient pas annotation");
                out.println("les url disponible avec leur methode et classe sonr:");

                for (Method urlDiso : listUrl) {
                        Url annotation = urlDiso.getAnnotation(Url.class);
                        String routeDispo = annotation.value();
                        String nomClassDispo = urlDiso.getDeclaringClass().getName();
                        String nomMethodeDispo = urlDiso.getName();

                        out.println("   URL      : " + routeDispo);
                        out.println("   Methode   : " + nomMethodeDispo);
                        out.println("   Class  : " + nomClassDispo);

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