package hre.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class ApiGen {

  public static void main(String args[]) throws IOException{
    Path currentRelativePath = Paths.get("");
    String s = currentRelativePath.toAbsolutePath().toString();
    System.out.println("Current relative path is: " + s);
    //api_gen(I1.class);
    //api_gen(I2.class);
    
    for (String name : args){
      Class c;
      try {
        c = Class.forName(args[0]);
        PrintStream out;
        if (args.length>1){
          out=new PrintStream(args[1]);
        } else {
          out=System.out;
        }
        api_gen(out,c);
        if (args.length>1){
          out.close();
        }
      } catch (ClassNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  private static String show(Type t){
    String res=t.toString();
    if (res.startsWith("class ")){
      res=res.substring(6);
    }
    return res;
  }

  private static void api_gen(PrintStream out,Class cl) throws IOException {
    //PrintStream out=new PrintStream(String.format("src/test/Wrapped%s.java",cl.getSimpleName()));
    out.printf("%s;%n", cl.getPackage());
    out.println("import java.lang.reflect.*;");
    out.println();
    out.printf( "/** Wrapper for %s implementations using reflection.  %n",cl.getSimpleName());
    out.println(" *  Thus it can wrap both older and newer versions without linker errors.");
    out.println(" *  This class is generated code! Do not modify!");
    out.println(" */");
    TypeVariable pars[]=cl.getTypeParameters();
    Method methods[]=cl.getMethods();
    java.util.Arrays.sort(methods,new Comparator<Method>(){
      @Override
      public int compare(Method o1, Method o2) {
        int tmp=o1.getName().compareTo(o2.getName());
        if (tmp!=0) return tmp;
        Type t1[]=o1.getParameterTypes();
        Type t2[]=o1.getParameterTypes();
        if (t1.length!=t2.length) return t1.length-t2.length;
        for(int i=0;i<t1.length;i++){
          String s1=t1[i].toString();
          String s2=t2[i].toString();
          tmp=s1.compareTo(s2);
          if (tmp!=0) return tmp;
        }
        return 0;
      }
    });
    if (pars.length>0){
      out.printf("class Wrapped%s", cl.getSimpleName());
      for(int i=0;i<pars.length;i++){
        out.printf("%s%s",i==0?"<":",",pars[i].getName());
        Type bounds[]=pars[i].getBounds();
        for (int j=0;j<bounds.length;j++){
          String res[]=bounds[j].toString().split(" ");
          out.printf("%s%s",j==0?" extends ":" & ",res[res.length-1]);
        }
      }      
      out.printf("> implements %s", cl.getSimpleName());
      for(int i=0;i<pars.length;i++){
        out.printf("%s%s",i==0?"<":",",pars[i].getName());
      }
      out.println("> {");
    } else {
      out.printf("class Wrapped%s implements %s {%n",
          cl.getSimpleName(),cl.getSimpleName());
    }
    for (int i=0;i<methods.length;i++){
      out.printf("private Method m%d;%n",i);
    }
    //out.printf("private final Class cl=%s.class;%n",cl.getName());
    out.println("private final Object obj;");
    out.printf("public Wrapped%s(Object obj){%n",cl.getSimpleName());
    out.println("  this.obj=obj;");
    out.println("  Class cl=obj.getClass();");
    for (int i=0;i<methods.length;i++){
      Type tt[]=methods[i].getParameterTypes();
      out.printf("  try {%n");
      out.printf("    m%d=cl.getMethod(\"%s\"",i,methods[i].getName());
      for(int j=0;j<tt.length;j++){
        String tmp[]=tt[j].toString().split(" ");
        String tcl=tmp[tmp.length-1];
        out.printf(",%s.class",tcl);
      }
      out.println(");");
      out.printf("  } catch (NoSuchMethodException e) {%n");
      out.printf("    throw new Error(\"NoSuchMethodException: \"+e.getMessage());%n");
      out.printf("  } catch (SecurityException e) {%n");
      out.printf("    throw new Error(\"SecurityException: \"+e.getMessage());%n");
      out.printf("  }%n");
    }
    out.println("}");
    for(int count=0;count<methods.length;count++){
      Method m=methods[count];
      boolean is_void=m.getGenericReturnType().toString().equals("void");
      Type t[]=m.getGenericParameterTypes();
      out.printf("public %s %s(",show(m.getGenericReturnType()),m.getName());
      for(int i=0;i<t.length;i++){
        out.printf("%s%s arg%d",i==0?"":",",show(t[i]),i);        
      }
      out.println("){");
      out.println("  try {");
      if (!is_void){
        out.printf("    return (%s)m%d.invoke(obj",show(m.getGenericReturnType()),count);
      } else {
        out.printf("    m%d.invoke(obj",count);
      }
      for(int i=0;i<t.length;i++){
        out.printf(",arg%d", i);
      }
      out.println(");");
      out.println("  } catch (IllegalAccessException | IllegalArgumentException e) {");
      out.println("    throw new Error(e.getClass()+\" \"+e.getMessage());");
      out.println("  } catch (InvocationTargetException e) {");
      out.println("    e.getCause().printStackTrace();");
      out.println("    throw new Error(\"in reflected call: \"+e.getCause().getClass()+\": \"+e.getCause().getMessage());");
      out.println("  }");

      out.println("}");
    }
    out.println("}");
    //out.close();
  }

  
}
