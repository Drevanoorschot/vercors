package vct.antlr4.parser;

import static hre.System.*;
import hre.ast.FileOrigin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import pv.parser.PVFullLexer;
import pv.parser.PVFullParser;
import vct.java.printer.JavaDialect;
import vct.java.printer.JavaSyntax;
import vct.parsers.*;
import vct.util.Configuration;
import vct.col.ast.ASTClass;
import vct.col.ast.ASTClass.ClassKind;
import vct.col.ast.ASTNode;
import vct.col.ast.ProgramUnit;
import vct.col.rewrite.AbstractRewriter;
import vct.col.rewrite.AnnotationInterpreter;
import vct.col.rewrite.FlattenVariableDeclarations;

/**
 * Parse specified code and convert the contents to COL. 
 */
public class ColJavaParser implements vct.col.util.Parser {

  @Override
  public ProgramUnit parse(File file) {
    String file_name=file.toString();
      try {
        TimeKeeper tk=new TimeKeeper();
        
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(file));
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        parser.setErrorHandler(new org.antlr.v4.runtime.BailErrorStrategy());
        ParseTree tree = parser.compilationUnit();        
        Progress("first parsing pass took %dms",tk.show());
        
        ProgramUnit pu=JavaToCol.convert(tree,file_name,tokens,parser);
        Progress("AST conversion took %dms",tk.show());
        
        Debug("program after Java parsing:%n%s",pu);
        //System.out.printf("parsing got %d entries%n",pu.size());
        //vct.util.Configuration.getDiagSyntax().print(System.out,pu);
        
        pu=new CommentRewriter(pu,new JMLCommentParser()).rewriteAll();
        Progress("Specification parsing took %dms",tk.show());
        
        pu=new FlattenVariableDeclarations(pu).rewriteAll();
        Progress("Flattening variables took %dms",tk.show());
        
        pu=new SpecificationCollector(pu).rewriteAll();
        Progress("Shuffling specifications took %dms",tk.show());        
        //vct.util.Configuration.getDiagSyntax().print(System.out,pu);
        
        pu=new JavaPostProcessor(pu).rewriteAll();
        Progress("post processing took %dms",tk.show());        
        //vct.util.Configuration.getDiagSyntax().print(System.out,pu);
        
        pu=new AnnotationInterpreter(pu).rewriteAll();
        Progress("interpreting annotations took %dms",tk.show());        
        //vct.util.Configuration.getDiagSyntax().print(System.out,pu);
        
        //cannnot resolve here: other .java files may be needed!
        //pu=new JavaResolver(pu).rewriteAll();
        //Progress("resolving library calls took %dms",tk.show());        
        //vct.util.Configuration.getDiagSyntax().print(System.out,pu);

        return pu;
      } catch (FileNotFoundException e) {
        Fail("File %s has not been found",file_name);
      } catch (Exception e) {
        e.printStackTrace();
        Abort("Exception %s while parsing %s",e.getClass(),file_name);
      }      
    return null;
  }

}

