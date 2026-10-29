package presentation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.util.function.Function;
import jakarta.servlet.http.HttpSession;

import mg.itu.tsanta.annotation.Api;
import mg.itu.tsanta.annotation.Auth;

@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, maxFileSize = 1024 * 1024 * 10, maxRequestSize = 1024 * 1024 * 50)
public class FrontControllerServlet extends HttpServlet {

        public FrontControllerServlet() {
                super();
        }

        private Map<UrlMethod, Mapping> listUrl;
        private String prefix;
        private String suffix;
        private String authSessionKey;
        private String roleKey; // <--- AJOUTÉ ICI POUR CORRIGER L'ERREUR DE COMPILATION
        private static final String SESSION_MAP_KEY = "frameworkInstanceSessions";

        @Override
        public void init() throws ServletException {
                this.listUrl = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("mappingUrls");
                if (this.listUrl == null) {
                        throw new ServletException("Le mapping des URL n'a pas été initialisé par le Listener.");
                }
                this.prefix = getServletContext().getInitParameter("viewPrefix");
                this.suffix = getServletContext().getInitParameter("viewSuffix");

                this.authSessionKey = getServletContext().getInitParameter("authSessionKey");
                if (this.authSessionKey == null || this.authSessionKey.isEmpty()) {
                        this.authSessionKey = "utilisateurConnecte";
                }

                this.roleKey = getServletContext().getInitParameter("roleKey");
                if (this.roleKey == null || this.roleKey.isEmpty()) {
                        this.roleKey = "roleUtilisateur";
                }

                ServletContext context = getServletContext();
                if (context.getAttribute(SESSION_MAP_KEY) == null) {
                        context.setAttribute(SESSION_MAP_KEY, new ConcurrentHashMap<HttpSession, MySession>());
                }

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

                                boolean requiresAuth = clazz.isAnnotationPresent(mg.itu.tsanta.annotation.Auth.class)
                                                || methodeAExecuter.isAnnotationPresent(
                                                                mg.itu.tsanta.annotation.Auth.class);

                                if (requiresAuth) {
                                        HttpSession nativeRequestSession = request.getSession(false);
                                        MySession currentSession = null;

                                        if (nativeRequestSession != null) {
                                                @SuppressWarnings("unchecked")
                                                Map<HttpSession, MySession> instanceSessions = (Map<HttpSession, MySession>) getServletContext()
                                                                .getAttribute(SESSION_MAP_KEY);

                                                if (instanceSessions != null) {
                                                        currentSession = instanceSessions.get(nativeRequestSession);
                                                }
                                        }

                                        Object authValue = (currentSession != null) ? currentSession.get(authSessionKey)
                                                        : null;

                                        if (authValue == null) {
                                                response.setContentType("text/html; charset=UTF-8");
                                                PrintWriter out = response.getWriter();
                                                out.println("<h3>Accès refusé : Vous devez être connecté pour accéder à cette ressource.</h3>");
                                                out.println("<a href='formconnex'>Se connecter</a>");
                                                return;
                                        }

                                        if (authValue != null) {
                                                String userRole = (currentSession != null)
                                                                ? (String) currentSession.get(roleKey)
                                                                : null;
                                                Auth authAnnotation = null;

                                                if (methodeAExecuter.isAnnotationPresent(Auth.class)) {
                                                        authAnnotation = methodeAExecuter.getAnnotation(Auth.class);
                                                } else if (clazz.isAnnotationPresent(Auth.class)) {
                                                        authAnnotation = clazz.getAnnotation(Auth.class); 
                                                }
                                                
                                                if (authAnnotation != null) {
                                                        String[] rolesRequis = authAnnotation.role();
                                                        if (rolesRequis.length > 0) { 
                                                                boolean validationRole = false;

                                                                for (String r : rolesRequis) { 
                                                                        if (r.equals(userRole)) {
                                                                                validationRole = true;
                                                                                break;
                                                                        }
                                                                }

                                                                if (!validationRole) {
                                                                        response.setContentType("text/html; charset=UTF-8");
                                                                        PrintWriter out = response.getWriter();
                                                                        out.println("<h3>Accès refusé : Vous n'avez pas le rôle requis pour accéder à cette ressource.</h3>");
                                                                        return;
                                                                }
                                                        }
                                                }
                                        }
                                } // <--- Fin du bloc if (requiresAuth)

                                ServletContext servletContext = getServletContext();
                                Object springContext = null;
                                try {
                                        springContext = servletContext.getAttribute(
                                                        "org.springframework.web.context.WebApplicationContext.ROOT");
                                } catch (Exception e) {

                                }

                                Parameter[] parametres = methodeAExecuter.getParameters();
                                Object[] argument = new Object[parametres.length];

                                for (int i = 0; i < parametres.length; i++) {
                                        argument[i] = resolveParameter(parametres[i], request, springContext);
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

        // parametre
        private Object resolveParameter(Parameter param, HttpServletRequest request, Object springContext) {
                Class<?> paramType = param.getType();
                String paramName = param.getName();

                if (paramType.equals(presentation.MySession.class)) {
                        HttpSession nativeSession = request.getSession(true);

                        ServletContext context = getServletContext();
                        @SuppressWarnings("unchecked")
                        Map<HttpSession, MySession> instanceSessions = (Map<HttpSession, MySession>) context
                                        .getAttribute(SESSION_MAP_KEY);

                        MySession sessionTrouvee = null;

                        for (Map.Entry<HttpSession, MySession> entry : instanceSessions.entrySet()) {
                                HttpSession storedHttpSession = entry.getKey();

                                if (storedHttpSession.equals(nativeSession)) {
                                        sessionTrouvee = entry.getValue();
                                        break;
                                }
                        }

                        if (sessionTrouvee == null) {
                                sessionTrouvee = new MySession(nativeSession);
                                instanceSessions.put(nativeSession, sessionTrouvee);
                        }

                        return sessionTrouvee;
                }

                // contexte spring
                if (paramType.getName().equals("org.springframework.context.ApplicationContext")) {
                        return springContext;
                }
                // simple
                if (isSimpleType(paramType)) {
                        String reqValue = request.getParameter(paramName);
                        return convertSimpleType(paramType, reqValue);
                }
                // tableau et liste
                if (paramType.isArray() || java.util.List.class.isAssignableFrom(paramType)) {
                        return bindCollectionOrArray(paramType, paramName, request);
                }
                // si c'est un Objet complexe (ou imbriqué)
                return bindComplexObject(paramType, paramName, request);
        }

        private Object bindComplexObject(Class<?> paramType, String prefix, HttpServletRequest request) {
                try {
                        Object objectInstance = paramType.getDeclaredConstructor().newInstance();
                        Field[] fields = paramType.getDeclaredFields();

                        for (Field field : fields) {
                                field.setAccessible(true);
                                String fieldName = field.getName();
                                Class<?> fieldType = field.getType();

                                String fullPrefix = (prefix != null && !prefix.isEmpty()) ? prefix + "." + fieldName
                                                : fieldName;

                                if (isSimpleType(fieldType)) {
                                        String reqValue = request.getParameter(fullPrefix);
                                        if (reqValue == null || reqValue.trim().isEmpty()) {
                                                reqValue = request.getParameter(fieldName);
                                        }

                                        if (reqValue != null && !reqValue.trim().isEmpty()) {
                                                Object convertObject = convertSimpleType(fieldType, reqValue);
                                                field.set(objectInstance, convertObject);
                                        }
                                } else if (fieldType.isArray() || java.util.List.class.isAssignableFrom(fieldType)) {
                                        Object collectionVal = bindCollectionOrArray(fieldType, fullPrefix, request);
                                        if (collectionVal == null) {
                                                collectionVal = bindCollectionOrArray(fieldType, fieldName, request);
                                        }
                                        if (collectionVal != null) {
                                                field.set(objectInstance, collectionVal);
                                        }
                                } else if (fieldType.getSimpleName().equals("MapFile")) {
                                        try {
                                                Part filePart = request.getPart(fullPrefix);
                                                if (filePart == null) {
                                                        filePart = request.getPart(fieldName);
                                                }

                                                if (filePart != null && filePart.getSize() > 0) {
                                                        Object mapFileInstance = fieldType.getDeclaredConstructor()
                                                                        .newInstance();

                                                        String submittedFileName = filePart.getSubmittedFileName();
                                                        Method setNomMethod = fieldType.getMethod("setNom",
                                                                        String.class);
                                                        setNomMethod.invoke(mapFileInstance, submittedFileName);

                                                        String extension = "";
                                                        if (submittedFileName != null
                                                                        && submittedFileName.contains(".")) {
                                                                extension = submittedFileName.substring(
                                                                                submittedFileName.lastIndexOf("."));
                                                        }
                                                        Method setExtensionMethod = fieldType.getMethod("setExtension",
                                                                        String.class);
                                                        setExtensionMethod.invoke(mapFileInstance, extension);

                                                        try (InputStream inputStream = filePart.getInputStream();
                                                                        ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                                                                int nRead;
                                                                byte[] data = new byte[16384];
                                                                while ((nRead = inputStream.read(data, 0,
                                                                                data.length)) != -1) {
                                                                        buffer.write(data, 0, nRead);
                                                                }

                                                                Method setBytesMethod = fieldType.getMethod("setBytes",
                                                                                byte[].class);
                                                                setBytesMethod.invoke(mapFileInstance,
                                                                                buffer.toByteArray());
                                                        }

                                                        field.set(objectInstance, mapFileInstance);
                                                }
                                        } catch (Exception e) {
                                                e.printStackTrace();
                                        }
                                } else {
                                        Object nestedInstance = bindComplexObject(fieldType, fullPrefix, request);
                                        if (nestedInstance != null) {
                                                field.set(objectInstance, nestedInstance);
                                        }
                                }
                        }
                        return objectInstance;
                } catch (Exception e) {
                        return null;
                }
        }

        private Object bindCollectionOrArray(Class<?> paramType, String paramName, HttpServletRequest request) {
                String[] reqValues = request.getParameterValues(paramName);
                if (reqValues == null) {
                        return null;
                }

                if (paramType.isArray()) {
                        Class<?> componentType = paramType.getComponentType();
                        Object arrayInstance = java.lang.reflect.Array.newInstance(componentType, reqValues.length);
                        for (int j = 0; j < reqValues.length; j++) {
                                java.lang.reflect.Array.set(arrayInstance, j,
                                                convertSimpleType(componentType, reqValues[j]));
                        }
                        return arrayInstance;
                } else {
                        java.util.List<Object> listInstance = new java.util.ArrayList<>();
                        for (String val : reqValues) {
                                listInstance.add(val);
                        }
                        return listInstance;
                }
        }

        private boolean isSimpleType(Class<?> type) {
                return type.isPrimitive()
                                || type == String.class
                                || type == Integer.class
                                || type == Double.class
                                || type == Boolean.class;
        }

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
                return value;
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