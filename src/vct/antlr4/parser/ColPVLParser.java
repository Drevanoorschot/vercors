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
public class ColPVLParser implements vct.col.util.Parser {

  @Override
  public ProgramUnit parse(File file) {
    String file_name=file.toString();
      try {
        TimeKeeper tk=new TimeKeeper();
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(file));
        PVFullLexer lexer = new PVFullLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PVFullParser parser = new PVFullParser(tokens);
        ParseTree tree = parser.program();
        Progress("parsing pass took %dms",tk.show());
        Debug("parser got: %s",tree.toStringTree(parser));

        ProgramUnit pu=PVLtoCOL.convert(tree,file_name,tokens,parser);      
        Progress("AST conversion pass took %dms",tk.show());
        pu=new FlattenVariableDeclarations(pu).rewriteAll();
        Progress("Variable pass took %dms",tk.show());
        pu=new PVLPostProcessor(pu).rewriteAll();
        Progress("Post processing pass took %dms",tk.show());
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

