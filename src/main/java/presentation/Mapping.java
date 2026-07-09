package presentation;

import java.lang.reflect.Method;

public class Mapping {
    private Class<?> ControllerInstance;
    private Method methode;

    public Mapping (Class<?> instance, Method methode){
        this.ControllerInstance = instance;
        this.methode = methode;
    }

    public Class<?> getControllerInstance() { return ControllerInstance; }
    public void setControllerInstance(Class<?> controllerInstance) { ControllerInstance = controllerInstance; }
    public Method getMethode() { return methode; }
    public void setMethode(Method methode) { this.methode = methode; }
}