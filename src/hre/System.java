package hre;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a way of sending text messages.
 * 
 * The current implementation assume that all messages are sent to
 * a PrintStream. Future implementations will be able to use
 * logging frameworks as well.
 * 
 */
class MessageStream {
  private PrintStream out;
  private String tag;
  public MessageStream(PrintStream out,String tag){
    this.out=out;
    this.tag=tag;
  }
  public void say(String format,Object...args){
    String message=String.format(format,args);
    out.printf("%s: %s%n",tag,message);
  }
}

/**
 * This class provides a way of providing feedback.
 */
public class System {
  
  private static Map<String,MessageStream> debug_map=new HashMap<String,MessageStream>();
  
  /**
   * Emit an error message, print stack trace and abort.
   * 
   * This method is meant for internal errors which are fatal
   * and may be reported as bugs.
   * 
   * @param format The formatting of the message.
   * @param args The arguments to be formatted.
   */
  public static void Abort(String format,Object...args){
    String message=String.format(format,args);
    java.lang.System.err.printf("%s%n",message);
    Thread.dumpStack();
    java.lang.System.exit(1);
  }
  
  /**
   * Emit an error message and abort.
   * 
   * This function is meant to be used for external error conditions,
   * such as bad input.
   */
  public static void Fail(String format,Object...args){
    String prefix="";
    if (where){
      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      int N=stackTraceElements.length;
      int idx=2;
      while(stackTraceElements[idx].getMethodName().equals("Fail")){
        idx++;
      }
      String name=stackTraceElements[idx].getClassName();
      int line=stackTraceElements[idx].getLineNumber();
      java.lang.System.err.printf("At line %d of %s:%n",line,name);
      prefix="  ";
    }
    String message=String.format(format,args);
    java.lang.System.err.printf("%s%s%n",prefix,message);
    java.lang.System.exit(1);
  }
  
  public static void EnableDebug(String className,PrintStream out,String tag){
    debug_map.put(className,new MessageStream(out,tag));
  }
  
  public static void DisableDebug(String className){
    debug_map.remove(className);
  }
  
  /**
   * Emit a debug message if the class calling this method is tagged for debugging.
   * 
   */
  public static void Debug(String format,Object...args){
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    int N=stackTraceElements.length;
    int idx=2;
    while(stackTraceElements[idx].getMethodName().equals("Debug")){
      idx++;
    }
    String name=stackTraceElements[idx].getClassName();
    for(;;){
      MessageStream out=debug_map.get(name);
      if (out!=null){
        out.say(format,args);
        return;
      }
      int lastdot=name.lastIndexOf(".");
      if (lastdot<0) return;
      name=name.substring(0,lastdot);
    }
  }
  
  private static boolean progress=true;
  
  public static void setProgressReporting(boolean progress){
    System.progress=progress;
  }
  
  /**
   * Emit a progress message, if those messages are enabled.
   */
  public static void Progress(String format,Object...args){
    if (progress){
      String message=String.format(format,args);
      java.lang.System.err.printf("%s%n",message);
    }
  }

  /**
   * Emit an output message.
   */
  public static void Output(String format,Object...args){
    String message=String.format(format,args);
    java.lang.System.out.printf("%s%n",message);    
  }
  
  /**
   * Emit a warning message.
   */
  public static void Warning(String format,Object...args){
    String message=String.format(format,args);
    java.lang.System.err.printf("WARNING: %s%n",message);    
  }

  private static boolean where=false;
  
  public static void EnableWhere(boolean b) {
    where=b;
  }
}
