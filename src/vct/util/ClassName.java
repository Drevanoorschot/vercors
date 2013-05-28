package vct.util;

import java.util.Arrays;
import static hre.System.Abort;

public class ClassName {

  public final String name[];
  
  public ClassName(String ... name){
    if (name.length==0) Abort("empty name");
    this.name=Arrays.copyOf(name,name.length);
  }
  
  public ClassName(String[] fullName, String name2) {
    name=new String[fullName.length+1];
    for(int i=0;i<fullName.length;i++) name[i]=fullName[i];
    name[fullName.length]=name2;
  }

  public static boolean equal(String name1[],String name2[]){
    if (name1.length!=name2.length) return false;
    for(int i=0;i<name1.length;i++){
      if (!name1[i].equals(name2[i])) return false;
    }
    return true;
  }

  public String toString(String separator) {
    StringBuilder builder=new StringBuilder();
    builder.append(name[0]);
    for(int i=1;i<name.length;i++){
      builder.append(separator);
      builder.append(name[i]);
    }
    return builder.toString();
  }
  
  public static String[] copy(String name[]){
    return Arrays.copyOf(name,name.length);
  }
  
  public static boolean prefixOf(String name1[],String name2[]){
    int N=name1.length;
    if (name2.length!=N) return false;
    N--;
    for (int i=0;i<N;i++){
      if (!name1[i].equals(name2[i])) return false;
    }
    return name2[N].startsWith(name1[N]);
  }

  public int hashCode(){
    return toString(".").hashCode();
  }
  public boolean equals(Object o){
    if (o instanceof ClassName){
      return equal(name,((ClassName)o).name);
    } else {
      return false;
    }
  }
}
