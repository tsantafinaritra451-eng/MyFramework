package presentation;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import src.main.java.mg.itu.tsanta.annotation.Controller;

public class FontControllerServlet extends HttpServlet {
        public FontControllerServlet() {
                super();
        }

        private List<String> listControllers = new ArrayList<>();

    @Override
    public void init() throws ServletException{
        try{
                 List<String> toutesClasses = Utilitaire.ScanneClass("controllers");
                 for (String className : toutesClasses){
                        Class <?> clazz = Class.forName(className);
                        if(clazz.isAnnotationPresent(Controller.class)){
                                this.listControllers.add(className);
                        }
                }
                        
              System.out.println("Framwork OK"+ this.listControllers.size());                  
                        
        }catch (Exception e){
                throw new ServletException("Erreur initialisation", e);
        }
        
}

        protected void processRequest(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                response.setContentType("text/plain");
                String url = request.getRequestURL().toString();

                response.getWriter().println("URL demandée : " + url);
                response.getWriter().println("--- Liste des contrôleurs détectés par le framework ---");

                for (String ctrl : this.listControllers) {
                        response.getWriter().println("- " + ctrl);
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
