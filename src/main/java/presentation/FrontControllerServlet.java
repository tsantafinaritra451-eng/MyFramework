package presentation;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import com.google.gson.Gson;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import javax.management.ObjectInstance;
import mg.itu.tsanta.annotation.Api;

public class FrontControllerServlet extends HttpServlet {

        public FrontControllerServlet() {
                super();
        }

        private Map<UrlMethod, Mapping> listUrl;
        private String prefix;
        private String suffix;

        @Override
        public void init() throws ServletException {
                this.listUrl = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("mappingUrls");
                if (this.listUrl == null) {
                        throw new ServletException("Le mapping des URL n'a pas été initialisé par le Listener.");
                }
                this.prefix = getServletContext().getInitParameter("viewPrefix");
                this.suffix = getServletContext().getInitParameter("viewSuffix");
        }

        protected void processRequest(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                String url = request.getPathInfo();
                if (url == null || url.equals("/")) {
                        url = request.getServletPath();
                }

                String methodHttp = request.getMethod().toUpperCase();

                UrlMethod cleRecherche = new UrlMethod(url, methodHttp);
                Mapping mappingTrouve = listUrl.get(cleRecherche);

                if (mappingTrouve != null) {
                        try {
                                Class<?> clazz = mappingTrouve.getControllerInstance();
                                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                                Method methodeAExecuter = mappingTrouve.getMethode();

                                ServletContext servletContext = getServletContext();
                                Object springContext = null;
                                try {
                                        springContext = servletContext.getAttribute(
                                                        "org.springframework.web.context.WebApplicationContext.ROOT");
                                } catch (Exception e) {

                                }

                                Parameter[] parametres = methodeAExecuter.getParameters();
                                Object[] argument = new Object[parametres.length];

                                for (int i = 0; i < argument.length; i++) {
                                        Parameter param = parametres[i];
                                        Class<?> paramType = param.getType();
                                        if (paramType.getName()
                                                        .equals("org.springframework.context.ApplicationContext")) {
                                                argument[i] = springContext;
                                        } else if (isSimpleType(paramType)) {
                                                String paramName = param.getName();
                                                String reqValue = request.getParameter(paramName);
                                                argument[i] = convertSimpleType(paramType, reqValue);
                                        } else {
                                                try {
                                                        Object ObjectInstance = paramType.getDeclaredConstructor()
                                                                        .newInstance();
                                                        Field[] Fields = paramType.getDeclaredFields();
                                                        for (Field field : Fields) {
                                                                field.setAccessible(true);
                                                                String fieldName = field.getName();
                                                                String reqValue = request.getParameter(fieldName);
                                                                if (reqValue != null && !reqValue.trim().isEmpty()) {
                                                                        Object convertObject = convertSimpleType(field.getType(), reqValue);
                                                                        field.set(ObjectInstance, convertObject);

                                                                }

                                                        }
                                                        argument[i] = ObjectInstance;

                                                } catch (Exception e) {
                                                        argument[i] = null;
                                                }
                                        }

                                }

                                Object resultat = methodeAExecuter.invoke(controllerInstance, argument);

                                boolean isApi = clazz.isAnnotationPresent(Api.class)
                                                || methodeAExecuter.isAnnotationPresent(Api.class);

                                if (isApi) {
                                        response.setContentType("application/json; charset=UTF-8");
                                        PrintWriter out = response.getWriter();
                                        Gson gson = new Gson();

                                        if (resultat instanceof ModAndView) {
                                                ModAndView mv = (ModAndView) resultat;
                                                String jsonResponse = gson.toJson(mv.getAttribut());
                                                out.println(jsonResponse);
                                        } else {
                                                String jsonResponse = gson.toJson(resultat);
                                                out.println(jsonResponse);
                                        }
                                        out.flush();
                                        return;
                                }

                                response.setContentType("text/html; charset=UTF-8");
                                PrintWriter out = response.getWriter();

                                if (resultat instanceof ModAndView) {
                                        ModAndView mv = (ModAndView) resultat;

                                        for (Map.Entry<String, Object> attribut : mv.getAttribut().entrySet()) {
                                                request.setAttribute(attribut.getKey(), attribut.getValue());
                                        }

                                        String prochaineVue = mv.getViewName();
                                        String cheminComplet = this.prefix + prochaineVue + this.suffix;
                                        RequestDispatcher dispatcher = request.getRequestDispatcher(cheminComplet);
                                        dispatcher.forward(request, response);
                                } else {
                                        out.println("<h3>Route trouvée mais aucun ModAndView renvoyé.</h3>");
                                        if (resultat != null) {
                                                out.println("Résultat brut : " + resultat.toString());
                                        }
                                }
                                return;

                        } catch (Exception e) {
                                response.setContentType("text/html; charset=UTF-8");
                                PrintWriter out = response.getWriter();
                                e.printStackTrace(out);
                                out.println("<h3>Erreur lors de l'exécution du contrôleur : " + e.getMessage()
                                                + "</h3>");
                                return;
                        }
                }

                response.setContentType("text/html; charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.println("<h3> Aucune méthode ne correspond à l'URL : " + url + " [" + methodHttp + "]</h3>");
                out.println("<h3>Liste des routes disponibles :</h3>");
                for (Map.Entry<UrlMethod, Mapping> entry : listUrl.entrySet()) {
                        UrlMethod urlDiso = entry.getKey();
                        Mapping mappingDispo = entry.getValue();

                        out.println("   URL : " + urlDiso.getUrl() + " [" + urlDiso.getMethodHttp() + "]<br>");
                        out.println("   Méthode : " + mappingDispo.getMethode().getName() + "()<br>");
                        out.println("   Classe : " + mappingDispo.getControllerInstance().getName() + "<br><br>");
                }
        }

        // Vérifie si la classe est un type simple
        private boolean isSimpleType(Class<?> type) {
                return type.isPrimitive()
                                || type == String.class
                                || type == Integer.class
                                || type == Double.class
                                || type == Boolean.class;
        }

        // Convertit une valeur String vers le type cible
        private Object convertSimpleType(Class<?> type, String value) {
                if (value == null || value.trim().isEmpty()) {
                        if (type == int.class)
                                return 0;
                        if (type == double.class)
                                return 0.0;
                        if (type == boolean.class)
                                return false;
                        return null;
                }

                if (type == int.class || type == Integer.class) {
                        return Integer.parseInt(value);
                } else if (type == double.class || type == Double.class) {
                        return Double.parseDouble(value);
                } else if (type == boolean.class || type == Boolean.class) {
                        return Boolean.parseBoolean(value);
                }
                return value; // String par défaut
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