package vct.antlr4.parser;

import static hre.System.*;
import hre.ast.FileOrigin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
import vct.col.rewrite.EncodeAsClass;
import vct.col.rewrite.FlattenVariableDeclarations;

/**
 * Parse specified code and convert the contents to COL. 
 */
public class ColIParser implements vct.col.util.Parser {

  protected ProgramUnit parse(String file_name,InputStream stream) throws IOException{
    ANTLRInputStream input = new ANTLRInputStream(stream);
    
    CLexer lexer = new CLexer(input);
    
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    
    CParser parser = new CParser(tokens);
    
    ParseTree tree = parser.compilationUnit();
    
    Debug("parser got: %s",tree.toStringTree(parser));

    ProgramUnit pu=CtoCOL.convert(tree,file_name,tokens,parser);
    
    System.err.println("after conversion");
    Configuration.getDiagSyntax().print(System.err, pu);
    
    pu=new CommentRewriter(pu,new CMLCommentParser()).rewriteAll();

    System.err.println("after comment processing");
    Configuration.getDiagSyntax().print(System.err, pu);

    pu=new FlattenVariableDeclarations(pu).rewriteAll();

    System.err.println("after flatteing variable decls");
    Configuration.getDiagSyntax().print(System.err, pu);

    pu=new SpecificationCollector(pu).rewriteAll();

    System.err.println("after collecting specifications");
    Configuration.getDiagSyntax().print(System.err, pu);

    // TODO: do not encode here, but at top level!
    pu=new EncodeAsClass(pu).rewriteAll();

    System.err.println("after rewriting to Ref class");
    Configuration.getDiagSyntax().print(System.err, pu);

    return pu;
  }
  
  @Override
  public ProgramUnit parse(File file) {
    String file_name=file.toString();
    try {
      InputStream stream =new FileInputStream(file);
      return parse(file_name,stream);
    } catch (FileNotFoundException e) {
      Fail("File %s has not been found",file_name);
    } catch (Exception e) {
      e.printStackTrace();
      Abort("Exception %s while parsing %s",e.getClass(),file_name);
    }
    return null;
  }

}

