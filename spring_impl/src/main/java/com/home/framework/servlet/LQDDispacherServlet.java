package com.home.framework.servlet;

import com.home.framework.annotation.LQDAutowired;
import com.home.framework.annotation.LQDController;
import com.home.framework.annotation.LQDRequestMapping;
import com.home.framework.annotation.LQDService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author liqingdong
 * @desc 模拟spring dispatcherServlet核心功能
 */
@SuppressWarnings("all")
public class LQDDispacherServlet extends HttpServlet {
    /** application.properties 值*/
    private Properties contextConfig = new Properties();
    /** 扫秒到的需要加载的类 */
    private List<String> classNames = new ArrayList<>();
    /** ioc容器 */
    private Map<String, Object> ioc = new HashMap<>();
    /** handlerMapping： url -> method */
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    public void init() throws ServletException {
        // 1.加载配置文件
        doLoadConfig();

        // 2.扫秒先关的类
        doScanner(contextConfig.getProperty("basePackage"));
        System.out.println("lqd spring class names init successful !!! classNames :" + classNames);
        // 3.初始化扫秒到的类，并将初始化后的类添加到ioc容器中
        doInstance();

        // 4.DI 依赖注入
        doAutowired();

        // 5.初始化handlerMapping
        initHandlerMapping();

        // 初始化完成

        System.out.println("lqd spring init successful !!");
    }

    /**
     * 1.加载初始化配置文件属性
     */
    private void doLoadConfig() {
        String applicationContext = super.getServletConfig().getInitParameter("contextConfig");
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(applicationContext);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("lqd spring context config init successful !!! contextConfig :" + contextConfig);
    }

    /**
     * 2.通过配置文件扫秒相关类
     * @param basePackage
     */
    public void doScanner(String basePackage) {
        // getResource:表示classpath
        URL resource = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
        File classpath = new File(resource.getFile());
        for (File file : classpath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(basePackage + "." + file.getName());
            } else {
                if(!file.getName().endsWith(".class")) { continue; }
                String className = (basePackage + "." + file.getName()).replace(".class", "");
                classNames.add(className);
            }
        }
    }

    /**
     * 3.构建ioc
     */
    private void doInstance() {
        if (classNames.isEmpty()){return;}
        try {
            for (String className : classNames) {
                Class clazz = Class.forName(className);
                // 什么样的类需要初始化呢？
                // 添加了注解的类
                if (clazz.isAnnotationPresent(LQDService.class)
                || clazz.isAnnotationPresent(LQDController.class)) {
                    Object o = clazz.newInstance();
                    String beanName = clazz.getSimpleName();
                    ioc.put(toUpperCase(beanName), o);
                    // 处理自定义beanName
                    if (clazz.isAnnotationPresent(LQDService.class)) {
                        LQDService annotation = (LQDService) clazz.getAnnotation(LQDService.class);
                        String value = annotation.value();
                        if (!"".equals(value) && !ioc.containsKey(value)) {
                            ioc.put(value, clazz.newInstance());
                        }
                        for (Class<?> clz : clazz.getInterfaces()) {
                            if (ioc.containsKey(clz.getName())) {
                                throw new RuntimeException(clz.getName() + " is exists!");
                            }
                            ioc.put(clz.getName(), o);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("lqd spring ioc init successful !!! ioc :" + ioc);
    }

    /**
     * 自动注入
     */
    private void doAutowired() {
        Collection<Object> values = ioc.values();
        try {
            for (Object obj : values) {
                Class clazz = obj.getClass();
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field field : declaredFields) {
                    if (field.isAnnotationPresent(LQDAutowired.class)) {
                        LQDAutowired lqdAutowired = field.getAnnotation(LQDAutowired.class);
                        String value = lqdAutowired.value();
                        field.setAccessible(true);
                        // 通过注解中的value进行赋值
                        if (!"".equals(value)) {
                            field.set(obj, ioc.get(value));
                        } else {
                            // 根据类型注入
                            String name = field.getType().getName();
                            field.set(obj, ioc.get(name));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("lqd spring DI init successful !!! ");
    }

    private void initHandlerMapping() {
        for (Object obj : ioc.values()) {
            Class<?> clazz = obj.getClass();
            if (!clazz.isAnnotationPresent(LQDController.class)) {continue;}
            if (clazz.isAnnotationPresent(LQDRequestMapping.class)) {
                LQDRequestMapping requestMapping = clazz.getAnnotation(LQDRequestMapping.class);
                // 类上的requestMapping值
                String classMapping = requestMapping.value();
                Method[] declaredMethods = clazz.getDeclaredMethods();
                for (Method declaredMethod : declaredMethods) {
                    if (!declaredMethod.isAnnotationPresent(LQDRequestMapping.class)) {continue;}
                    LQDRequestMapping methodRequestMapping = declaredMethod.getAnnotation(LQDRequestMapping.class);
                    String methodMapping = methodRequestMapping.value();
                    String url = (classMapping + "/" + methodMapping).replaceAll("/+", "/");
                    handlerMapping.put(url, declaredMethod);
                }
            }
        }
        System.out.println("lqd spring handlerMapping init successful !!! handlerMapping :" + handlerMapping);
    }

    /**
     * 首字母大小写转换，大小写字母ASCII码相差32位，并且大写比小写ASCII码小
     * @param name
     * @return
     */
    private String toUpperCase(String name) {
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return new String(chars);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispach(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 server error! " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispach(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        String requestURI = req.getRequestURI();
        if (!handlerMapping.containsKey(requestURI)) {
            resp.getWriter().write("404 not found !!!");
            return;
        }
        Method method = handlerMapping.get(requestURI);
        // invoke方法调用需要对象实例，现在拿不到对象实例，对象实例存在ioc容器中
        // ioc容器获取对象需要类名，现在拿不到类名
        // 简单实现：根据method通过反射方式拿到method所在类，在拿到类名，通过ioc容器获取对象实例
        // 投机取巧，不应该这么搞
        String beanName = toUpperCase(method.getDeclaringClass().getSimpleName());
        // 拿不到调用参数，暂时写死
        Object id = method.invoke(ioc.get(beanName), req.getParameter("id"));
        resp.getWriter().write("invoke result: " + (String)id);
    }
}
